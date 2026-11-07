package fatum.catalog.service


import fatum.catalog.dto.mapper.toRoot
import fatum.catalog.dto.mapper.toSubcategory
import fatum.catalog.dto.request.category.CreateCategoryRequest
import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Category
import fatum.catalog.model.constant.SchedulingModel
import fatum.catalog.model.constant.ServiceStatus
import fatum.catalog.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import io.github.fatumapp.storage.FileStorageClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.multipart.MultipartFile

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val fileStorageClient: FileStorageClient,
    @Value("\${catalog.categories.logo-route}") private val logoStorageRoute: String,
) {

    companion object {
        private const val MAX_LOGO_SIZE = 3L * 1024 * 1024
        private val ALLOWED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }


    @Transactional
    fun createRoot(request: CreateCategoryRequest, logo: MultipartFile?) {
        val logoKey = logo?.let {
            validateLogoImage(it)
            fileStorageClient.upload(it, logoStorageRoute).key
        }
        val category = request.toRoot(logoKey)
        if (categoryRepository.existsByNameIgnoreCase(category.name))
            throw FatumServiceException(CatalogErrorCode.CATEGORY_EXISTS)
        categoryRepository.save(category)
    }

    @Transactional
    fun createSubCategory(request: CreateCategoryRequest, logo: MultipartFile?) {
        val parent = getCategory(request.parent ?: "")
        if (categoryRepository.existsByNameIgnoreCase(request.name.trim()))
            throw FatumServiceException(CatalogErrorCode.CATEGORY_EXISTS)
        val logoKey = logo?.let {
            validateLogoImage(it)
            fileStorageClient.upload(it, logoStorageRoute).key
        }
        val subcategory = request.toSubcategory(parent, logoKey)
        categoryRepository.save(subcategory)
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

    //=================================== LOGO =====================================================

    @Transactional
    fun updateLogo(name: String, image: MultipartFile) {
        val category = getCategory(name)
        validateLogoImage(image)
        category.logoKey?.let { fileStorageClient.delete(logoStorageRoute, it) }
        val uploaded = fileStorageClient.upload(image, logoStorageRoute)
        category.logoKey = uploaded.key
        categoryRepository.save(category)
    }

    fun getCategoryLogoUrl(category: Category): String? =
        category.logoKey?.let {
            try { fileStorageClient.presignedUrl(logoStorageRoute, it) }
            catch (_: Exception) { null }
        }

    private fun validateLogoImage(image: MultipartFile) {
        if (image.isEmpty) throw FatumServiceException(CatalogErrorCode.INVALID_IMAGE)
        if (image.size > MAX_LOGO_SIZE) throw FatumServiceException(CatalogErrorCode.IMAGE_TOO_LARGE)
        val contentType = image.contentType?.lowercase()
        if (contentType == null || contentType !in ALLOWED_IMAGE_TYPES)
            throw FatumServiceException(CatalogErrorCode.INVALID_IMAGE_FORMAT)
    }

}
