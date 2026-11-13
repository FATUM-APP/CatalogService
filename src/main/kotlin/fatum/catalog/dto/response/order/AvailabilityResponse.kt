package fatum.catalog.dto.response.order

import fatum.catalog.model.constant.AvailabilityReason
import java.time.LocalDateTime


data class AvailabilityResponse(
    val providerId: String,
    val booking: LocalDateTime,
    val durationMinutes: Int,
    val endsAt: LocalDateTime,
    val available: Boolean,
    val reason: AvailabilityReason? = null
)
