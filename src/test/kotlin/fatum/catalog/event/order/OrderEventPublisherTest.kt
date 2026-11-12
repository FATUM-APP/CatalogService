package fatum.catalog.event.order

import fatum.catalog.model.constant.OrderStatus
import fatum.catalog.realtime.sse.SseHub
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.eventbridgev2.EventBridgeV2Client
import software.amazon.awssdk.services.eventbridgev2.model.PutEventsRequest
import software.amazon.awssdk.services.eventbridgev2.model.PutEventsResponse
import tools.jackson.databind.ObjectMapper

class OrderEventPublisherTest {

    private val eventBridge: EventBridgeV2Client = mock()
    private val hub: SseHub = mock()
    private val publisher = OrderEventPublisher(eventBridge, "", ObjectMapper(), hub)

    /** Todo lo que se mandó al canal, separado por nombre de evento SSE. */
    private fun sentTo(name: String): List<Pair<List<String>, Map<String, Any?>>> {
        val topics = argumentCaptor<Collection<String>>()
        val names = argumentCaptor<String>()
        val events = argumentCaptor<Map<String, Any?>>()
        verify(hub, atLeastOnce()).publish(topics.capture(), names.capture(), events.capture())
        return topics.allValues.indices
            .filter { names.allValues[it] == name }
            .map { topics.allValues[it].toList() to events.allValues[it] }
    }

    @Test
    fun `a status change reaches both parties`() {
        publisher.statusChanged("order-1", "client-1", "provider-1", OrderStatus.ACCEPTED)

        assertEquals(
            listOf(SseHub.userTopic("client-1"), SseHub.userTopic("provider-1")),
            sentTo("order").first().first
        )
    }

    @Test
    fun `a new order only reaches the provider`() {
        publisher.orderCreated("order-1", "provider-1", "Plomería")

        assertEquals(listOf(SseHub.userTopic("provider-1")), sentTo("order").first().first)
    }

    @Test
    fun `the message says what kind of event it is`() {
        publisher.confirmationChanged("order-1", "client-1", "provider-1", "START", "CUSTOMER", true, false)

        val event = sentTo("order").first().second
        assertEquals("ORDER_CONFIRMATION_CHANGED", event["type"])
        assertEquals("order-1", event["orderId"])
        assertEquals("CUSTOMER", event["party"])
        assertEquals(true, event["confirmed"])
    }

    @Test
    fun `an order also moves the calendar of the provider`() {
        publisher.orderCreated("order-1", "provider-1", "Plomería")

        val calendar = sentTo("calendar").first()
        assertEquals(listOf(SseHub.calendarTopic("provider-1")), calendar.first)
        assertEquals("CALENDAR_CHANGED", calendar.second["type"])
        assertEquals("ORDER_UPDATED", calendar.second["change"])
    }

    @Test
    fun `without a bus nothing goes to EventBridge`() {
        publisher.statusChanged("order-1", "client-1", "provider-1", OrderStatus.CANCELLED)

        verifyNoInteractions(eventBridge)
    }

    @Test
    fun `with a bus the event is published with its source and type`() {
        whenever(eventBridge.putEvents(any<PutEventsRequest>()))
            .thenReturn(PutEventsResponse.builder().failedEntryCount(0).build())
        val withBus = OrderEventPublisher(
            eventBridge, "arn:aws:events:us-east-1:1:event-bus/fatum", ObjectMapper(), hub
        )

        withBus.orderCreated("order-1", "provider-1", "Plomería")

        argumentCaptor<PutEventsRequest>().apply {
            verify(eventBridge).putEvents(capture())
            val entry = firstValue.entries().first()
            assertEquals(OrderEventPublisher.SOURCE, entry.source())
            assertEquals("ORDER_CREATED", entry.detailType())
            assertTrue(entry.detail().contains("\"orderId\":\"order-1\""))
        }
    }
}
