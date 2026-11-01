package fatum.catalog.dto

import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class CreateUnavailability(
    @field:NotBlank(message = "Provider id is required")
    val providerId: String,
    val reason: String? = null,
    val start: LocalDateTime,
    val end : LocalDateTime
)
