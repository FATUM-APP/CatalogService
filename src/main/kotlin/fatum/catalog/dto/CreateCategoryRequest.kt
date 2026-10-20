package fatum.catalog.dto

import fatum.catalog.model.constant.SchedulingModel
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateRootCategory(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 30, message = "Name must not exceed 30 characters")
    private val _name: String,
    @field:NotNull(message = "Scheduling model is required")
    val schedulingModel: SchedulingModel,
){
    val name = _name.trim()
}

data class CreateSubCategory(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 30, message = "Name must not exceed 30 characters")
    private val _name: String,

    @field:NotBlank(message = "root is required")
    @field:Size(max = 30, message = "root must not exceed 30 characters")
    private val _parent: String,
){
    val name = _name.trim()
    val parent = _parent.trim()
}

