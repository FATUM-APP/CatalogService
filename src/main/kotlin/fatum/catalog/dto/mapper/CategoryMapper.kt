package fatum.catalog.dto.mapper

import fatum.catalog.dto.CreateCategoryRequest
import fatum.catalog.model.Category

object CategoryMapper {

    fun toEntity(request: CreateCategoryRequest, parent: Category?): Category {
        if (parent != null) {
            return Category(request.name, parent = parent)
        }
        return Category(request.name, request.schedulingModel)
    }
}