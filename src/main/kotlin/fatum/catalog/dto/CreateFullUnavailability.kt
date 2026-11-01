package fatum.catalog.dto

import jakarta.validation.constraints.NotBlank
import org.springframework.cglib.core.Local
import java.time.LocalDate
import java.time.LocalDateTime

data class CreateFullUnavailability(
    @field:NotBlank(message = "Provider id is required")
    val providerId: String,
    val reason: String? = null,
    val start: LocalDate,
    val end : LocalDate
)
