package fatum.catalog.realtime

import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Category
import fatum.catalog.model.Order
import fatum.catalog.model.ProviderAvailability
import fatum.catalog.model.ProviderUnavailableRange
import fatum.catalog.model.constant.ConfirmingParty
import fatum.catalog.model.constant.JobType
import fatum.catalog.model.constant.OrderStatus
import fatum.catalog.model.constant.PriceType
import fatum.catalog.model.constant.SchedulingModel
import fatum.catalog.model.vo.BookedSlot
import fatum.catalog.model.vo.CalendarDay
import fatum.catalog.realtime.dto.CalendarChangeReason
import fatum.catalog.realtime.dto.CalendarUpdateMode
import fatum.catalog.realtime.dto.ConfirmationState
import fatum.catalog.realtime.dto.OrderChangeType
import fatum.catalog.realtime.event.CalendarChange
import fatum.catalog.realtime.listener.OrderRealtimeListener
import fatum.catalog.realtime.listener.ProviderAvailabilityRealtimeListener
import fatum.catalog.realtime.listener.ProviderUnavailableRangeRealtimeListener
import fatum.catalog.realtime.mapper.toProviderDay
import fatum.catalog.realtime.mapper.toPublicDay
import fatum.catalog.realtime.mapper.toRealtimeUpdate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class RealtimeMappersAndListenersTest {

    private val dispatcher: RealtimeDispatcher = mock()

    @AfterEach
    fun tearDown() {
        RealtimeHooks.uninstall(dispatcher)
    }

    private fun catalogItem() = CatalogItem(
        providerId = "provider-1",
        title = "Reparacion De Tuberias",
        providerName = "Provider Test",
        providerUsername = "providertest",
        priceType = PriceType.FIXED,
        jobType = JobType.PRESENCIAL,
        category = Category("Plomeria", SchedulingModel.SINGLE_SESSION),
        basePrice = BigDecimal(100),
        defaultEstimatedTime = 60
    ).also { item ->
        CatalogItem::class.java.getDeclaredField("id").apply { isAccessible = true }.set(item, "service-1")
    }

    private fun order(status: OrderStatus = OrderStatus.PENDING): Order {
        val order = Order(
            clientId = "client-1",
            clientName = "Juan Perez",
            clientUsername = "juanperez",
            booking = LocalDateTime.of(2026, 11, 20, 9, 0),
            catalogItem = catalogItem(),
            negotiatedPrice = BigDecimal(100),
            deliveryCity = "Bogota",
            deliveryState = "Cundinamarca",
            deliveryCountry = "Colombia",
            deliveryResidence = "Calle 123",
            answers = emptySet()
        )
        Order::class.java.getDeclaredField("id").apply { isAccessible = true }.set(order, "order-1")
        Order::class.java.getDeclaredField("status").apply { isAccessible = true }.set(order, status)
        return order
    }

    @Test
    fun `should map an order to a realtime snapshot`() {
        val order = order()
        order.startConfirmation.confirm(ConfirmingParty.CUSTOMER)

        val update = order.toRealtimeUpdate(OrderChangeType.ORDER_CREATED)

        assertEquals("order-1", update.orderId)
        assertEquals("client-1", update.clientId)
        assertEquals("provider-1", update.providerId)
        assertEquals("Provider Test", update.providerName)
        assertEquals("service-1", update.serviceId)
        assertEquals(OrderStatus.PENDING, update.status)
        assertEquals(ConfirmationState.PARTIAL, update.startConfirmation.state)
        assertEquals(ConfirmationState.PENDING, update.paymentConfirmation.state)
    }

    @Test
    fun `public calendar view should hide order ids`() {
        val day = CalendarDay(
            date = LocalDate.of(2026, 11, 20),
            isAvailable = true,
            bookedSlots = listOf(
                BookedSlot("order-1", LocalDateTime.of(2026, 11, 20, 9, 0), 60, OrderStatus.ACCEPTED)
            )
        )

        val public = day.toPublicDay()
        val provider = day.toProviderDay()

        assertEquals(1, public.slots.size)
        assertEquals(LocalDateTime.of(2026, 11, 20, 10, 0), public.slots.first().end)
        assertEquals("order-1", provider.slots.first().orderId)
    }

    @Test
    fun `order listener should publish a created event`() {
        RealtimeHooks.install(dispatcher)

        OrderRealtimeListener().onCreated(order())

        val captor = argumentCaptor<fatum.catalog.realtime.dto.OrderRealtimeUpdate>()
        verify(dispatcher).orderChanged(captor.capture())
        assertEquals(OrderChangeType.ORDER_CREATED, captor.firstValue.changeType)
        assertEquals("order-1", captor.firstValue.orderId)
    }

    @Test
    fun `order listener should flag cancellations`() {
        RealtimeHooks.install(dispatcher)

        OrderRealtimeListener().onUpdated(order(OrderStatus.CANCELLED))

        val captor = argumentCaptor<fatum.catalog.realtime.dto.OrderRealtimeUpdate>()
        verify(dispatcher).orderChanged(captor.capture())
        assertEquals(OrderChangeType.ORDER_CANCELLED, captor.firstValue.changeType)
    }

    @Test
    fun `availability listener should invalidate the weekly day`() {
        RealtimeHooks.install(dispatcher)

        ProviderAvailabilityRealtimeListener().onChanged(ProviderAvailability("provider-1", DayOfWeek.FRIDAY))

        val captor = argumentCaptor<CalendarChange>()
        verify(dispatcher).calendarChanged(captor.capture())
        assertEquals(CalendarUpdateMode.INVALIDATE, captor.firstValue.mode)
        assertEquals(DayOfWeek.FRIDAY, captor.firstValue.day)
        assertEquals(setOf(CalendarChangeReason.AVAILABILITY_CHANGED), captor.firstValue.reasons)
    }

    @Test
    fun `unavailability listener should patch the date range`() {
        RealtimeHooks.install(dispatcher)
        val range = ProviderUnavailableRange(
            "provider-1",
            "Vacaciones",
            LocalDate.of(2026, 11, 20),
            LocalDate.of(2026, 11, 25)
        )

        ProviderUnavailableRangeRealtimeListener().onCreated(range)

        val captor = argumentCaptor<CalendarChange>()
        verify(dispatcher).calendarChanged(captor.capture())
        assertEquals(CalendarUpdateMode.DAYS, captor.firstValue.mode)
        assertEquals(LocalDate.of(2026, 11, 20), captor.firstValue.from)
        assertEquals(LocalDate.of(2026, 11, 25), captor.firstValue.to)
    }

    @Test
    fun `listeners should be no-ops without a spring context`() {
        RealtimeHooks.uninstall(dispatcher)

        OrderRealtimeListener().onCreated(order())

        verifyNoInteractions(dispatcher)
        assertNull(RealtimeHooks.current())
    }
}
