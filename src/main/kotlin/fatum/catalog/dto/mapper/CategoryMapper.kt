package fatum.catalog.dto.mapper

import fatum.catalog.dto.request.category.CreateCategoryRequest
import fatum.catalog.dto.response.category.CategoryResponse
import fatum.catalog.model.Category

fun CreateCategoryRequest.toRoot(logoKey: String? = null) =
    Category(
        name = name.trim(),
        schedulingModel = schedulingModel,
        logoKey = logoKey
    )

fun CreateCategoryRequest.toSubcategory(parent: Category, logoKey: String? = null) =
    Category(
        name = name.trim(),
        parent = parent,
        logoKey = logoKey
    )

fun Category.toResponse(logoUrl: String? = null) : CategoryResponse =
    CategoryResponse(
        name = name,
        slug = slug,
        parentSlug = parent?.slug,
        schedulingModel = schedulingModel.name,
        isAble = isAble,
        logoUrl = logoUrl
    )