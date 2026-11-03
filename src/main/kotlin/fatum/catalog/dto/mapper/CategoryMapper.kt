package fatum.catalog.dto.mapper

import fatum.catalog.dto.request.category.CreateCategoryRequest
import fatum.catalog.dto.response.category.CategoryResponse
import fatum.catalog.model.Category

fun CreateCategoryRequest.toRoot() =
    Category(
        name = name.trim(),
        schedulingModel = schedulingModel
    )

fun CreateCategoryRequest.toSubcategory(parent: Category) =
    Category(
        name = name.trim(),
        parent = parent
    )

fun Category.toResponse() : CategoryResponse =
    CategoryResponse(
        name = name,
        slug = slug,
        parent = parent?.toResponse(),
        subcategories = subcategories.mapTo(mutableSetOf()) { it.toResponse() },
        schedulingModel = schedulingModel.name,
        isAble = isAble
    )