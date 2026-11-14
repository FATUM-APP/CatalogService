package fatum.catalog.realtime

import fatum.catalog.realtime.sse.SseHub
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.LocalDate

class CalendarRealtimePublisherTest {

    private val hub: SseHub = mock()
    private val publisher = CalendarRealtimePublisher(hub)

    @Test
    fun `a new unavailability says which days and to whose calendar`() {
        publisher.changed("provider-1", CalendarChange.UNAVAILABLE_ADDED, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 22))

        argumentCaptor<Collection<String>>().apply {
            verify(hub).publish(capture(), any(), any())
            assertEquals(listOf(SseHub.calendarTopic("provider-1")), firstValue.toList())
        }
        argumentCaptor<Map<String, Any?>>().apply {
            verify(hub).publish(any(), any(), capture())
            assertEquals("CALENDAR_CHANGED", firstValue["type"])
            assertEquals("UNAVAILABLE_ADDED", firstValue["change"])
            assertEquals("2026-11-20", firstValue["start"])
            assertEquals("2026-11-22", firstValue["end"])
        }
    }

    @Test
    fun `a weekly schedule change carries no dates`() {
        publisher.changed("provider-1", CalendarChange.WEEKLY_SCHEDULE_CHANGED)

        argumentCaptor<Map<String, Any?>>().apply {
            verify(hub).publish(any(), any(), capture())
            assertEquals("WEEKLY_SCHEDULE_CHANGED", firstValue["change"])
            assertEquals(null, firstValue["start"])
        }
    }
}
