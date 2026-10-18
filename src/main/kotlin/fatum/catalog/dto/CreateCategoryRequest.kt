package fatum.catalog.dto

import fatum.catalog.model.constant.SchedulingModel
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateCategoryRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 30, message = "Name must not exceed 30 characters")
    val name: String,
    val schedulingModel: SchedulingModel? = null,
    val parentName: String? = null,
)
