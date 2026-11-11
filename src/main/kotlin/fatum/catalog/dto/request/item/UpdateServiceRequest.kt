package fatum.catalog.dto.request.item

import fatum.catalog.model.constant.JobType
import fatum.catalog.model.constant.PriceType
import fatum.catalog.model.vo.FixedQuestion
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class UpdateServiceRequest(
    @field:NotBlank(message = "Provider id is required")
    @field:Size(max = 36, message = "Provider id must not exceed 36 characters")
    var id: String,
    @field:Pattern(regexp = "\\S.*", message = "Title cannot be empty")
    @field:Size(max = 40, message = "Title must not exceed 40 characters")
    var  title: String? = null,
    @field:Size(max = 1000, message = "Description must not exceed 1000 characters")
    var description: String? = null,
    @field:PositiveOrZero(message = "Base price cannot be negative")
    val basePrice: BigDecimal? = null,
    val priceType: PriceType? = null,
    val isNegotiable: Boolean? = null,
    @field:PositiveOrZero(message = "Default estimated time cannot be negative")
    val defaultEstimatedTime: Int? = null,
    val jobType: JobType? = null,
    val fixedQuestions: Set<@JvmSuppressWildcards @Valid FixedQuestion> = emptySet()
){
    init{
        this.id = id.trim()
        this.title = title?.trim()
        this.description = description?.trim()
    }



}