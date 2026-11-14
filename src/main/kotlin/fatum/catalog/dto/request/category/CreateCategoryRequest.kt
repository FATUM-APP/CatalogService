package fatum.catalog.dto.request.category

import fatum.catalog.model.constant.SchedulingModel

data class CreateCategoryRequest(
    val name: String,
    private val _parent: String? = null,
    val schedulingModel: SchedulingModel
){
    val parent = _parent?.trim()
}
