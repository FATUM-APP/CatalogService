package fatum.catalog.service

import fatum.catalog.dto.CreateRootCategory
import fatum.catalog.dto.CreateSubCategory
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
    fun createRoot(request: CreateRootCategory): Category {
        if (categoryRepository.existsByNameIgnoreCase(request.name))
            throw FatumServiceException(CatalogErrorCode.CATEGORY_EXISTS)
        val category = CategoryMapper.toRoot(request)
        return categoryRepository.save(category)
    }

    @Transactional
    fun createSubCategory(request: CreateSubCategory): Category {
        if (categoryRepository.existsByNameIgnoreCase(request.name))
            throw FatumServiceException(CatalogErrorCode.CATEGORY_EXISTS)
        val parent = getCategory(request.parent)
        val category = CategoryMapper.toSub(request, parent)
        return categoryRepository.save(category)
    }


    @Transactional(readOnly = true)
    fun findCategory(name: String, isAble: Boolean? = null): Category = getCategory(name, isAble)

    @Transactional(readOnly = true)
    fun searchCategoryServices(category: String): Set<CatalogItem> =
        getCategory(category, true).services

    @Transactional(readOnly = true)
    fun searchSubCategories(category: String): Set<Category> =
        getCategory(category, true).subcategories

    @Transactional(readOnly = true)
    fun searchRootCategories(isAble: Boolean? = null): List<Category> =
        if(isAble != null) categoryRepository.findByParentIsNullAndIsAble(isAble)
        else categoryRepository.findByParentIsNull()




    @Transactional
    fun disableCategory(name: String): Boolean {
        val category = getCategory(name)
        if(category.isAble){
            category.disable()
            categoryRepository.save(category)
        }
        return category.isAble
    }

    @Transactional
    fun enableCategory(name: String): Boolean {
        val category = getCategory(name)
        if(category.parent?.isAble == false) throw FatumServiceException(CatalogErrorCode.ENABLING_SUB_WITH_ROOT_DISABLED)
        if(!category.isAble){
            category.enable()
            categoryRepository.save(category)
        }
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

    //=================================== HELPERS=====================================================
    private fun getCategory(name: String, isAble: Boolean? = null): Category =
        (if (isAble == null) categoryRepository.findByNameIgnoreCase(name)
        else categoryRepository.findByNameIgnoreCaseAndIsAble(name, isAble))
            ?: throw FatumServiceException(CatalogErrorCode.CATEGORY_NOT_FOUND)
}
