package fatum.catalog.dto.mapper


import fatum.catalog.dto.CreateRootCategory
import fatum.catalog.dto.CreateSubCategory
import fatum.catalog.model.Category

object CategoryMapper {

    fun toSub(request: CreateSubCategory, parent: Category): Category {
        return Category(request.name, parent = parent)
    }

    fun toRoot(request: CreateRootCategory): Category {
        return Category(request.name, request.schedulingModel)
    }
}