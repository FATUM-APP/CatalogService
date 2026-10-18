package fatum.catalog.service

import fatum.catalog.dto.CreateCategoryRequest
import fatum.catalog.dto.mapper.CategoryMapper
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.Category
import fatum.catalog.repository.CategoryRepository
import org.springframework.transaction.annotation.Transactional

class CategoryService(
    val categoryRepository: CategoryRepository
) {
    @Transactional
    fun createCategory(request: CreateCategoryRequest): Category {
        if(categoryRepository.existsById(request.name))
            throw FatumServiceException(FatumServiceException.CATEGORY_EXISTS)
        val parent = categoryRepository.findById(request.parentName)
            ?: throw FatumServiceException(FatumServiceException.PARENT_CATEGORY_NOT_FOUND)
        val category = CategoryMapper.toEntity(request, parent)
    }


}