package fatum.catalog.dto.mapper

import fatum.catalog.dto.CreateCategoryRequest
import fatum.catalog.model.Category


object CategoryMapper {
    fun toEntity(categoryRequest: CreateCategoryRequest, parent: Category?): Category {
        return Category(
            categoryRequest.name,
            categoryRequest.schedulingModel
        )
    }
}