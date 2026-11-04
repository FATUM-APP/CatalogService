package fatum.catalog.dto.response.schedule

import java.time.LocalTime

data class Availability(
    val providerId: String,
    val day: String,
    val initialTime: LocalTime,
    val finalTime: LocalTime
)
