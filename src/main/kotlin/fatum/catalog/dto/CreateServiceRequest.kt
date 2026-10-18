package fatum.catalog.dto


import fatum.catalog.model.constant.JobType
import fatum.catalog.model.constant.PriceType
import fatum.catalog.model.vo.FixedQuestion
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateServiceRequest(
    @field:NotBlank(message = "Provider id is required")
    @field:Size(max = 36, message = "Provider id must not exceed 36 characters")
    val providerId: String,

    @field:NotBlank(message = "Title is required")
    @field:Size(max = 40, message = "Title must not exceed 40 characters")
    val title: String,

    @field:Size(max = 1000, message = "Description must not exceed 1000 characters")
    val description: String? = null,

    @field:PositiveOrZero(message = "Base price cannot be negative")
    val basePrice: BigDecimal = BigDecimal.ZERO,

    @field:NotNull(message = "Price type is required")
    val priceType: PriceType,
    val isNegotiable: Boolean = false,
    val defaultEstimatedTime: Int? = null,
    val jobType: JobType = JobType.PRESENCIAL,
    @field:NotBlank(message = "Category is required")
    @field:Size(max = 30, message = "Category must not exceed 30 characters")
    val category: String,
    @field:Valid
    val fixedQuestions: Set<FixedQuestion> = emptySet()
    )