package fatum.catalog.dto.request.schedule

import java.time.DayOfWeek
import java.time.LocalTime

data class ChangeAvailability(
    private val _providerId: String,
    val day: DayOfWeek,
    val start: LocalTime,
    val end: LocalTime
){
    val providerId = _providerId.trim()
}
