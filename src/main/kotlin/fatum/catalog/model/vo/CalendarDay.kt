package fatum.catalog.model.vo

import java.time.LocalDate
import java.time.LocalTime

data class CalendarDay(
    val date: LocalDate,
    val isAvailable: Boolean,
    val initialTime: LocalTime? = null,
    val finalTime: LocalTime? = null,
    val unavailableReason: String? = null,
    val bookedSlots: List<BookedSlot> = emptyList()
)