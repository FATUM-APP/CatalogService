package fatum.catalog.dto.request

import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class CreateFullUnavailability(
    @field:NotBlank(message = "Provider id is required")
    val providerId: String,
    val reason: String? = null,
    val start: LocalDate,
    val end : LocalDate
)