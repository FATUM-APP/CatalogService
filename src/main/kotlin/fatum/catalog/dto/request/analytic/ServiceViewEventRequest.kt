package fatum.catalog.dto.request.analytic

import java.time.LocalDateTime

data class ServiceViewEventRequest(
    val eventType: String,
    val userId: String,
    val serviceId: String,
    val dwellTimeSeconds: Int,
    val timestamp: LocalDateTime
)