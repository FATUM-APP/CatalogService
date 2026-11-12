package fatum.catalog.realtime

import fatum.catalog.model.constant.OrderStatus
import fatum.catalog.model.vo.CalendarDay
import fatum.catalog.realtime.config.RealtimeProperties
import fatum.catalog.realtime.dto.CalendarChangeReason
import fatum.catalog.realtime.dto.CalendarUpdateMode
import fatum.catalog.realtime.dto.ConfirmationState
import fatum.catalog.realtime.dto.ConfirmationView
import fatum.catalog.realtime.dto.OrderChangeType
import fatum.catalog.realtime.dto.OrderRealtimeUpdate
import fatum.catalog.realtime.dto.ProviderCalendarUpdate
import fatum.catalog.realtime.dto.PublicCalendarUpdate
import fatum.catalog.realtime.dto.RealtimeMessage
import fatum.catalog.realtime.event.CalendarChange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.messaging.Message
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.support.AbstractMessageChannel
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDate
import java.time.LocalDateTime

class RealtimeDispatcherTest {

    private class CapturingChannel : AbstractMessageChannel() {
        val messages = mutableListOf<Message<*>>()
        override fun sendInternal(message: Message<*>, timeout: Long): Boolean {
            messages.add(message)
            return true
        }
    }

    private lateinit var channel: CapturingChannel
    private lateinit var dispatcher: RealtimeDispatcher
    private lateinit var calendarPatchService: CalendarPatchService

    @BeforeEach
    fun setUp() {
        channel = CapturingChannel()
        calendarPatchService = mock()
        dispatcher = RealtimeDispatcher(
            SimpMessagingTemplate(channel),
            calendarPatchService,
            RealtimeProperties()
        )
    }

    private fun order(
        orderId: String = "order-1",
        status: OrderStatus = OrderStatus.ACCEPTED,
        changeType: OrderChangeType = OrderChangeType.ORDER_UPDATED
    ) = OrderRealtimeUpdate(
        changeType = changeType,
        orderId = orderId,
        status = status,
        booking = LocalDateTime.of(2026, 11, 20, 9, 0),
        estimatedTime = 60,
        reason = null,
        createdAt = LocalDateTime.of(2026, 11, 12, 8, 0),
        updatedAt = LocalDateTime.of(2026, 11, 12, 8, 30),
        deliveryStartedAt = null,
        clientId = "client-1",
        clientName = "Juan Perez",
        clientUsername = "juanperez",
        providerId = "provider-1",
        providerName = "Provider",
        providerUsername = "provider",
        serviceId = "service-1",
        startConfirmation = ConfirmationView(null, null, ConfirmationState.PENDING),
        completionConfirmation = ConfirmationView(null, null, ConfirmationState.PENDING),
        paymentConfirmation = ConfirmationView(null, null, ConfirmationState.PENDING)
    )

    private fun destinations() = channel.messages.map { it.headers[SimpMessageHeaderAccessor.DESTINATION_HEADER] as String }

    @Test
    fun `should notify both stakeholders when there is no transaction`() {
        whenever(calendarPatchService.resolve(any()))
            .thenReturn(CalendarPatch(CalendarUpdateMode.INVALIDATE, emptyList()))

        dispatcher.orderChanged(order())

        assertEquals(
            setOf("/user/client-1/queue/orders", "/user/provider-1/queue/orders"),
            destinations().filter { it.endsWith("/queue/orders") }.toSet()
        )
        val payload = channel.messages.first().payload as RealtimeMessage<*>
        assertEquals("ORDER_UPDATED", payload.type)
        assertEquals("order-1", (payload.payload as OrderRealtimeUpdate).orderId)
        // El cambio de una orden también mueve el calendario del proveedor.
        assertTrue(destinations().contains("/topic/calendar/provider-1"))
    }

    @Test
    fun `should not send anything before commit`() {
        TransactionSynchronizationManager.initSynchronization()
        try {
            dispatcher.orderChanged(order())
            assertTrue(channel.messages.isEmpty())
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun `should collapse several updates of the same order into one message per stakeholder`() {
        whenever(calendarPatchService.resolve(any()))
            .thenReturn(CalendarPatch(CalendarUpdateMode.INVALIDATE, emptyList()))

        TransactionSynchronizationManager.initSynchronization()
        try {
            dispatcher.orderChanged(order(status = OrderStatus.PENDING, changeType = OrderChangeType.ORDER_CREATED))
            dispatcher.orderChanged(order(status = OrderStatus.ACCEPTED))
            dispatcher.orderChanged(order(status = OrderStatus.DELIVERING))

            TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }

        val orderMessages = channel.messages.filter { (it.headers[SimpMessageHeaderAccessor.DESTINATION_HEADER] as String).endsWith("/queue/orders") }
        assertEquals(2, orderMessages.size)
        orderMessages.forEach {
            assertEquals(OrderStatus.DELIVERING, ((it.payload as RealtimeMessage<*>).payload as OrderRealtimeUpdate).status)
        }
    }

    @Test
    fun `should publish public and provider calendar messages`() {
        val day = CalendarDay(date = LocalDate.of(2026, 11, 20), isAvailable = true)
        whenever(calendarPatchService.resolve(any()))
            .thenReturn(CalendarPatch(CalendarUpdateMode.DAYS, listOf(day)))

        dispatcher.calendarChanged(
            CalendarChange.range(
                "provider-1",
                CalendarChangeReason.UNAVAILABILITY_CREATED,
                LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 21)
            )
        )

        assertEquals(
            setOf("/topic/calendar/provider-1", "/user/provider-1/queue/calendar"),
            destinations().toSet()
        )
        val publicMessage = channel.messages.first { (it.headers[SimpMessageHeaderAccessor.DESTINATION_HEADER] as String).startsWith("/topic") }
        val publicPayload = (publicMessage.payload as RealtimeMessage<*>).payload as PublicCalendarUpdate
        assertEquals(CalendarUpdateMode.DAYS, publicPayload.mode)
        assertEquals(1, publicPayload.days.size)

        val providerMessage = channel.messages.first { (it.headers[SimpMessageHeaderAccessor.DESTINATION_HEADER] as String).endsWith("/queue/calendar") }
        val providerPayload = (providerMessage.payload as RealtimeMessage<*>).payload as ProviderCalendarUpdate
        assertEquals(1, providerPayload.days.size)
    }

    @Test
    fun `should fall back to invalidate when the patch cannot be computed`() {
        whenever(calendarPatchService.resolve(any())).thenThrow(IllegalStateException("database down"))

        dispatcher.calendarChanged(
            CalendarChange.range(
                "provider-1",
                CalendarChangeReason.ORDER_UPDATED,
                LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 20)
            )
        )

        val publicMessage = channel.messages.first { (it.headers[SimpMessageHeaderAccessor.DESTINATION_HEADER] as String).startsWith("/topic") }
        val publicPayload = (publicMessage.payload as RealtimeMessage<*>).payload as PublicCalendarUpdate
        assertEquals(CalendarUpdateMode.INVALIDATE, publicPayload.mode)
        assertTrue(publicPayload.days.isEmpty())
        verify(calendarPatchService, times(1)).resolve(any())
    }
}
