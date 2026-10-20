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
    private val _providerId: String,

    @field:NotBlank(message = "Title is required")
    @field:Size(max = 40, message = "Title must not exceed 40 characters")
    private val _title: String,

    @field:Size(max = 1000, message = "Description must not exceed 1000 characters")
    private val _description: String? = null,

    @field:PositiveOrZero(message = "Base price cannot be negative")
    val basePrice: BigDecimal = BigDecimal.ZERO,

    @field:NotNull(message = "Price type is required")
    val priceType: PriceType,
    val isNegotiable: Boolean = false,
    val defaultEstimatedTime: Int? = null,
    @field:NotNull(message = "Job type is required")
    val jobType: JobType = JobType.PRESENCIAL,

    @field:NotBlank(message = "Category is required")
    @field:Size(max = 30, message = "Category must not exceed 30 characters")
    private val _category: String,
    @field:Valid
    val fixedQuestions: Set<FixedQuestion> = emptySet()
    ){
    val providerId = _providerId.trim()
    val title = _title.trim()
    val category = _category.trim()
    val description = _description?.trim() ?: null
}