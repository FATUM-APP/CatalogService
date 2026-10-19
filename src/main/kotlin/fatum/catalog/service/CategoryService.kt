package fatum.catalog.service

import fatum.catalog.dto.CreateCategoryRequest
import fatum.catalog.dto.mapper.CategoryMapper
import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.CatalogItem
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
            throw FatumServiceException(CatalogErrorCode.CATEGORY_EXISTS)

        val parent = request.parentName?.let { getCategory(it) }
        val category = CategoryMapper.toEntity(request, parent)
        return categoryRepository.save(category)
    }

    @Transactional(readOnly = true)
    fun findCategory(name: String): Category = getCategory(name)

    @Transactional(readOnly = true)
    fun getCategoryServices(category: String): Set<CatalogItem> =
        getCategory(category).getServices()

    @Transactional(readOnly = true)
    fun getSubCategories(category: String): Set<Category> =
        getCategory(category).getSubCategories()

    @Transactional(readOnly = true)
    fun getRootCategories(): List<Category> =
        categoryRepository.findByParentIsNull()

    @Transactional
    fun disableCategory(name: String): Boolean {
        val category = getCategory(name)
        category.disable()
        categoryRepository.save(category)
        return category.isAble
    }

    @Transactional
    fun enableCategory(name: String): Boolean {
        val category = getCategory(name)
        category.enable()
        categoryRepository.save(category)
        return category.isAble
    }

    @Transactional
    fun deleteCategory(name: String) {
        val category = getCategory(name)
        if (category.hasServices()) {
            category.disable()
            categoryRepository.save(category)
        } else {
            categoryRepository.delete(category)
        }
    }

    private fun getCategory(name: String): Category =
        categoryRepository.findByNameIgnoreCase(name)
            .orElseThrow { FatumServiceException(CatalogErrorCode.CATEGORY_NOT_FOUND) }
}
