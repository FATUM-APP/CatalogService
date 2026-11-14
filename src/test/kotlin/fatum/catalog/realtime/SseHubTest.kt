package fatum.catalog.realtime

import fatum.catalog.realtime.sse.SseHub
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SseHubTest {

    private val hub = SseHub(RealtimeProperties(maxConnectionsPerUser = 2))

    @Test
    fun `a user can keep several tabs open`() {
        hub.subscribe(SseHub.userTopic("user-1"))
        hub.subscribe(SseHub.userTopic("user-1"))

        assertEquals(2, hub.connectionCount(SseHub.userTopic("user-1")))
    }

    @Test
    fun `beyond the limit the oldest tab is closed`() {
        hub.subscribe(SseHub.userTopic("user-1"))
        hub.subscribe(SseHub.userTopic("user-1"))
        hub.subscribe(SseHub.userTopic("user-1"))

        assertEquals(2, hub.connectionCount(SseHub.userTopic("user-1")))
    }

    @Test
    fun `publishing to nobody does nothing`() {
        hub.publish(listOf(SseHub.userTopic("user-9")), "order", mapOf("type" to "ORDER_CREATED"))
        hub.publish(emptyList(), "order", mapOf("type" to "ORDER_CREATED"))

        assertEquals(0, hub.connectionCount(SseHub.userTopic("user-9")))
    }

    @Test
    fun `publishing to a connected user does not fail`() {
        hub.subscribe(SseHub.userTopic("user-1"))

        hub.publish(listOf(SseHub.userTopic("user-1")), "order", mapOf("type" to "ORDER_STATUS_CHANGED"))

        assertEquals(1, hub.connectionCount(SseHub.userTopic("user-1")))
    }

    @Test
    fun `a provider and their clients share the calendar topic`() {
        hub.subscribe(SseHub.calendarTopic("provider-1"))
        hub.subscribe(SseHub.calendarTopic("provider-1"))

        hub.publish(listOf(SseHub.calendarTopic("provider-1")), "calendar", mapOf("change" to "UNAVAILABLE_ADDED"))

        assertEquals(2, hub.connectionCount(SseHub.calendarTopic("provider-1")))
    }
}
