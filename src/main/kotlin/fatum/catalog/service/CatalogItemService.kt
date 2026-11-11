package fatum.catalog.service

import fatum.catalog.client.UserServiceClient
import fatum.catalog.dto.request.item.CatalogImage
import fatum.catalog.dto.request.item.CreateServiceRequest
import fatum.catalog.dto.request.item.UpdateServiceRequest

import fatum.catalog.dto.mapper.toEntity
import fatum.catalog.dto.request.item.ActivateCatalogItem
import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.CatalogItem
import fatum.catalog.model.ServiceImage
import fatum.catalog.model.constant.PriceType
import fatum.catalog.model.constant.ServiceStatus
import fatum.catalog.repository.CatalogItemRepository
import io.github.fatumapp.storage.FileStorageClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal

@Service
class CatalogItemService(
    private val catalogItemRepository: CatalogItemRepository,
    private val categoryService: CategoryService,
    private val fileStorageClient: FileStorageClient,
    @Value("\${catalog.images.storage-route}") private val storageRoute: String,
    private val userServiceClient: UserServiceClient,
) {
    //==========================================CREATE==============================================

    /**
     * Creates a new catalog item
     * @param request
     * @param images
     */
    @Transactional
    fun createCatalogItem(request: CreateServiceRequest, images: List<MultipartFile>) : String{
        validateProfessional(request.providerId)
        validateImages(images)
        validateUniqueness(request)

        val serviceImages = transformToServiceImages(images)
        val category = categoryService.findCategory(request.category,true)
         return catalogItemRepository.save(request.toEntity(
            serviceImages,
            category,
            userServiceClient.getUserById(request.providerId)
            )).id!!
    }



    //==========================================READ==============================================
    @Transactional(readOnly = true)
    fun findById(id : String, status : ServiceStatus? = null) : CatalogItem =
        getById(id, status)

    @Transactional(readOnly = true)
    fun findByProviderAndCategory(providerId: String, categoryName: String? = null) : List<CatalogItem> =
        getByProviderAndCategory(providerId, categoryName)

    /**
     * Searches catalog items by title
     * @param title
     * @param serviceStatus, its default value is ACTIVE
     */
    @Transactional(readOnly = true)
    fun findByTitle(title: String, serviceStatus: ServiceStatus? = ServiceStatus.ACTIVE): List<CatalogItem> =
        getByTitle(title, serviceStatus)

    /**
     * Searches if a catalog has in its title a substring
     * @param keyword
     * @param status, its default value is ACTIVE
     */
    @Transactional(readOnly = true)
    fun findByTitleContaining(keyword:  String, status : ServiceStatus = ServiceStatus.ACTIVE): List<CatalogItem> =
        catalogItemRepository.searchByTitle(keyword, status, true)

    /**
     * Searches if a catalog has in its title a prefix
     * @param prefix
     * @param status, its default value is ACTIVE
     */
    @Transactional(readOnly = true)
    fun findByTitleStartingWith(prefix: String, status: ServiceStatus = ServiceStatus.ACTIVE): List<CatalogItem> =
        catalogItemRepository.autocompleteByTitle(prefix, status, true)


    @Transactional(readOnly = true)
    fun searchInCatalog(keyword: String, status : ServiceStatus? = null): List<CatalogItem> =
        catalogItemRepository.searchInCatalog(keyword,status,true)

    //DUMMY
    @Transactional(readOnly = true)
    fun filterCatalog(
        priceType: PriceType? = null,
        minPrice: BigDecimal? = BigDecimal.ZERO,
        maxPrice: BigDecimal? = null,
        isNegotiable: Boolean? = null,
        categoryName: String? = null,
        status: ServiceStatus? = ServiceStatus.ACTIVE
    ): List<CatalogItem> =
        catalogItemsFilter(
            priceType = priceType,
            minPrice = minPrice,
            maxPrice = maxPrice,
            isNegotiable = isNegotiable,
            categoryName = categoryName,
            status = status
        )

    /**
     * Return all items (for admin if status = null, or filtered by status)
     */
    @Transactional(readOnly = true)
    fun findAll(status: ServiceStatus? = null): List<CatalogItem> =
        catalogItemsFilter(status = status)

    @Transactional(readOnly = true)
    fun seeCatalogImages(catalogItemId: String, status : ServiceStatus? = null):
            List<CatalogImage> = getImages(catalogItemId,status)


    //==========================================UPDATE==============================================

    @Transactional
    fun updateCatalogItem(request: UpdateServiceRequest, images: List<MultipartFile>): CatalogItem {
        val catalogItem = getById(request.id)
        validateImages(images)

        val oldImages = catalogItem.serviceCatalog.toList()
        catalogItem.serviceCatalog.clear()
        catalogItem.serviceCatalog.addAll(transformToServiceImages(images))
        deleteBucketImages(oldImages)


        request.title?.let {
            validateTitleUniqueness(it, catalogItem.providerId)
            catalogItem.title = it
        }
        request.description?.let {
            catalogItem.description = it
        }
        request.basePrice?.let {
            catalogItem.basePrice = it
        }
        request.priceType?.let {
            catalogItem.priceType = it
        }
        when (request.isNegotiable) {
            true -> catalogItem.makeNegotiable()
            false -> catalogItem.makeNonNegotiable()
            null -> Unit
        }
        catalogItem.defaultEstimatedTime = request.defaultEstimatedTime
        request.jobType?.let {
            catalogItem.jobType = it
        }
        catalogItem.fixedQuestions = request.fixedQuestions

        return catalogItemRepository.save(catalogItem)
    }

    @Transactional
    fun deactivate(request: ActivateCatalogItem){
        val catalogItem = getById(request.id)
        if(catalogItem.status != ServiceStatus.ACTIVE)
            throw FatumServiceException(CatalogErrorCode.SERVICE_STATUS_CANT_BE_CHANGED)
        catalogItem.status = ServiceStatus.INACTIVE
        catalogItemRepository.save(catalogItem)
    }

    @Transactional
    fun activate(request: ActivateCatalogItem){
        val catalogItem = getById(request.id)
        if(catalogItem.status != ServiceStatus.INACTIVE)
            throw FatumServiceException(CatalogErrorCode.SERVICE_STATUS_CANT_BE_CHANGED)
        catalogItem.status = ServiceStatus.ACTIVE
        catalogItemRepository.save(catalogItem)
    }

    //==========================================HELPERS==============================================
    private fun getById(id : String, status : ServiceStatus? = null): CatalogItem =
        (if(status == null) catalogItemRepository.findById(id).orElse(null)
        else catalogItemRepository.findByIdFiltered(id,status,true))
            ?: throw FatumServiceException(CatalogErrorCode.SERVICE_NOT_FOUND)

    private fun getByTitle(title: String, serviceStatus: ServiceStatus? = ServiceStatus.ACTIVE): List<CatalogItem> =
    if(serviceStatus == null)catalogItemRepository.findByTitle(title,null,true)
    else catalogItemRepository.findByTitle(title, serviceStatus,true)

    private fun validateUniqueness(request: CreateServiceRequest) {
        if (catalogItemRepository.existsByCategoryNameAndProviderIdIgnoreCase(request.category, request.providerId))
            throw FatumServiceException(CatalogErrorCode.UK_SERVICES_CATEGORY_PROVIDER)
        validateTitleUniqueness(request.title, request.providerId)
    }
    private fun validateTitleUniqueness(title: String, providerId: String){
        if (catalogItemRepository.existsByTitleAndProviderIdIgnoreCase(title, providerId))
            throw FatumServiceException(CatalogErrorCode.UK_SERVICES_TITLE_PROVIDER)
    }
    /**
     * Filter catalog items by combining optional criteria
     */
    private fun catalogItemsFilter(
        priceType: PriceType? = null,
        minPrice: BigDecimal? = BigDecimal.ZERO,
        maxPrice: BigDecimal? = null,
        isNegotiable: Boolean? = null,
        categoryName: String? = null,
        status: ServiceStatus? = ServiceStatus.ACTIVE
    ): List<CatalogItem> =
        catalogItemRepository.filterCatalog(
            priceType = priceType,
            minPrice = minPrice,
            maxPrice = maxPrice,
            isNegotiable = isNegotiable,
            status = status,
            categoryName = categoryName,
            true
        )
    private fun getByProviderAndCategory(providerId: String, categoryName: String? = null) : List<CatalogItem> {
        categoryName?.let { categoryService.findCategory(name = categoryName, isAble = true) }
        return catalogItemRepository.findByProviderAndOptionalCategory(providerId, categoryName)
    }


    private fun validateProfessional(providerId: String){
        val provider = userServiceClient.getUserById(providerId)
        if (!provider.isValid) throw FatumServiceException(CatalogErrorCode.INVALID_USER)
        if (!provider.isProfessional) throw FatumServiceException(CatalogErrorCode.NOT_A_PROFESSIONAL)
    }

    //==========================================IMAGES==============================================
    companion object {
        private const val MAX_IMAGES = 10
        private const val MAX_IMAGE_SIZE = 5L * 1024 * 1024  // 5MB per image
        private val ALLOWED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }
    private fun transformToServiceImages(images : List<MultipartFile>) : List<ServiceImage> =
        images.map { image ->
        val uploaded = fileStorageClient.upload(image, storageRoute)
        ServiceImage(
            imageKey = uploaded.key,
            originalFilename = uploaded.originalFilename,
            contentType = uploaded.contentType,
            fileSize = uploaded.size
        )
    }

    private fun validateImages(images: List<MultipartFile>) {
        if (images.isEmpty()) throw FatumServiceException(CatalogErrorCode.NO_SERVICE_IMAGES)
        if (images.size > MAX_IMAGES) throw FatumServiceException(CatalogErrorCode.TOO_MANY_IMAGES)
        for (image in images) {
            if (image.isEmpty) throw FatumServiceException(CatalogErrorCode.INVALID_IMAGE)
            if (image.size > MAX_IMAGE_SIZE) throw FatumServiceException(CatalogErrorCode.IMAGE_TOO_LARGE)
            val contentType = image.contentType?.lowercase()
            if (contentType == null || contentType !in ALLOWED_IMAGE_TYPES)
                throw FatumServiceException(CatalogErrorCode.INVALID_IMAGE_FORMAT)
        }
    }

    private fun deleteBucketImages(images: List<ServiceImage>)  {
        images.forEach { image ->
            fileStorageClient.delete(storageRoute, image.imageKey)
        }
    }

    private fun getImages(catalogItemId: String, status: ServiceStatus? = null): List<CatalogImage> {
        val serviceCatalog = getById(catalogItemId, status).serviceCatalog
        return serviceCatalog.map { image ->
            val url = fileStorageClient.presignedUrl(
                storageRoute,
                image.imageKey
            ) ?: throw FatumServiceException(CatalogErrorCode.NO_IMAGE_URL)
            CatalogImage(
                image.id ?: throw FatumServiceException(CatalogErrorCode.NO_IMAGE_ID),
                image.originalFilename,
                image.contentType,
                image.fileSize,
                url
            )
        }
    }

}


