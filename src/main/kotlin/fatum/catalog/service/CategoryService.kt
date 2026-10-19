package fatum.catalog.service

import fatum.catalog.dto.CreateCategoryRequest
import fatum.catalog.dto.mapper.CategoryMapper
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.Category
import fatum.catalog.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository
) {

    @Transactional
    fun create(request: CreateCategoryRequest): Category {
        if (categoryRepository.existsByNameIgnoreCase(request.name))
            throw FatumServiceException(FatumServiceException.CATEGORY_EXISTS)

        val parent = request.parentName?.let { findCategory(it) }
        val category = CategoryMapper.toEntity(request, parent)
        return categoryRepository.save(category)
    }

    @Transactional(readOnly = true)
    fun findCategory(name: String): Category {
        return categoryRepository.findByNameIgnoreCase(name)
            .orElseThrow { FatumServiceException(FatumServiceException.CATEGORY_NOT_FOUND) }
    }

    @Transactional
    fun disableCategory(name: String): Boolean {
        val category = findCategory(name)
        category.disable()
        categoryRepository.save(category)
        return category.isAble
    }

    @Transactional
    fun enableCategory(name: String): Boolean {
        val category = findCategory(name)
        category.enable()
        categoryRepository.save(category)
        return category.isAble
    }

    @Transactional
    fun deleteCategory(name: String) {
        val category = findCategory(name)
        if (category.hasServices()) disableCategory(name)
        else categoryRepository.delete(category)
    }
}