package fatum.catalog.dto.request.schedule

import java.time.DayOfWeek

data class FullAvailability(
    private val _providerId: String,
    val day: DayOfWeek
){
    val providerId = _providerId.trim()
}
