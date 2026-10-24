package fatum.catalog.service


import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Category
import fatum.catalog.model.constant.SchedulingModel
import fatum.catalog.model.constant.ServiceStatus
import fatum.catalog.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository
) {

    @Transactional
    fun createRoot(name: String, scheduling: SchedulingModel): Category {
        if (categoryRepository.existsByNameIgnoreCase(name))
            throw FatumServiceException(CatalogErrorCode.CATEGORY_EXISTS)
        return categoryRepository.save(Category(name,scheduling))
    }

    @Transactional
    fun createSubCategory(name: String, parentName: String): Category {
        if (categoryRepository.existsByNameIgnoreCase(name))
            throw FatumServiceException(CatalogErrorCode.CATEGORY_EXISTS)
        val parent = getCategory(parentName)
        return categoryRepository.save(Category(name, parent))
    }


    @Transactional(readOnly = true)
    fun findCategory(name: String, isAble: Boolean? = null): Category = getCategory(name, isAble)

    @Transactional(readOnly = true)
    fun searchCategoryServices(category: String, status: ServiceStatus? = ServiceStatus.ACTIVE): Set<CatalogItem> =
        getCategory(category, true).services
            .filter { it.category.isAble && (status == null || it.status == status) }
            .toSet()


    @Transactional(readOnly = true)
    fun searchSubCategories(category: String, areAble: Boolean? = null): Set<Category> =
        getCategory(category, true).subcategories
            .filter {areAble == null || it.isAble == areAble }
            .toSet()


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
        } else categoryRepository.delete(category)

    }

    //=================================== HELPERS=====================================================
    private fun getCategory(name: String, isAble: Boolean? = null): Category =
        (if (isAble == null) categoryRepository.findByNameIgnoreCase(name)
        else categoryRepository.findByNameIgnoreCaseAndIsAble(name, isAble))
            ?: throw FatumServiceException(CatalogErrorCode.CATEGORY_NOT_FOUND)


}
