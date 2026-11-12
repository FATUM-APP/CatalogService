package fatum.catalog.realtime

import fatum.catalog.model.vo.CalendarDay
import fatum.catalog.realtime.config.RealtimeProperties
import fatum.catalog.realtime.dto.CalendarChangeReason
import fatum.catalog.realtime.dto.CalendarUpdateMode
import fatum.catalog.realtime.event.CalendarChange
import fatum.catalog.service.CalendarService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class CalendarPatchServiceTest {

    private val calendarService: CalendarService = org.mockito.kotlin.mock()
    private val properties = RealtimeProperties()
    private val service = CalendarPatchService(calendarService, properties)

    private fun day(date: String) = CalendarDay(date = LocalDate.parse(date), isAvailable = true)

    @Test
    fun `should patch only the affected days`() {
        whenever(calendarService.getMonthCalendar("p1", 2026, 11)).thenReturn(
            listOf(day("2026-11-19"), day("2026-11-20"), day("2026-11-21"), day("2026-11-22"))
        )

        val patch = service.resolve(
            CalendarChange.range(
                "p1",
                CalendarChangeReason.UNAVAILABILITY_CREATED,
                LocalDate.parse("2026-11-20"),
                LocalDate.parse("2026-11-21")
            )
        )

        assertEquals(CalendarUpdateMode.DAYS, patch.mode)
        assertEquals(listOf(LocalDate.parse("2026-11-20"), LocalDate.parse("2026-11-21")), patch.days.map { it.date })
    }

    @Test
    fun `should query every month touched by the range`() {
        whenever(calendarService.getMonthCalendar("p1", 2026, 11)).thenReturn(listOf(day("2026-11-30")))
        whenever(calendarService.getMonthCalendar("p1", 2026, 12)).thenReturn(listOf(day("2026-12-01")))

        val patch = service.resolve(
            CalendarChange.range(
                "p1",
                CalendarChangeReason.UNAVAILABILITY_CREATED,
                LocalDate.parse("2026-11-30"),
                LocalDate.parse("2026-12-01")
            )
        )

        assertEquals(2, patch.days.size)
        verify(calendarService).getMonthCalendar("p1", 2026, 11)
        verify(calendarService).getMonthCalendar("p1", 2026, 12)
    }

    @Test
    fun `should invalidate when the range exceeds the patch window`() {
        val patch = service.resolve(
            CalendarChange.range(
                "p1",
                CalendarChangeReason.UNAVAILABILITY_CREATED,
                LocalDate.parse("2026-11-01"),
                LocalDate.parse("2027-06-01")
            )
        )

        assertEquals(CalendarUpdateMode.INVALIDATE, patch.mode)
        assertTrue(patch.days.isEmpty())
        verify(calendarService, never()).getMonthCalendar(any(), any(), any())
    }

    @Test
    fun `should invalidate availability changes without querying the calendar`() {
        val patch = service.resolve(
            CalendarChange.invalidate("p1", CalendarChangeReason.AVAILABILITY_CHANGED)
        )

        assertEquals(CalendarUpdateMode.INVALIDATE, patch.mode)
        verify(calendarService, never()).getMonthCalendar(any(), any(), any())
    }
}
