package fatum.catalog.dto.response.schedule

import java.time.LocalDateTime

data class Unavailability(
    val id: String?,
    val providerId: String,
    var reason: String? = null,
    val startDate: LocalDateTime,
    val finalDate: LocalDateTime
)