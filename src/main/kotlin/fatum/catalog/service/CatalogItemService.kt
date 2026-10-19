package fatum.catalog.service

import fatum.catalog.dto.CreateServiceRequest
import fatum.catalog.dto.mapper.CatalogItemMapper
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.CatalogItem
import fatum.catalog.model.ServiceImage
import fatum.catalog.repository.CatalogItemRepository
import io.github.fatumapp.storage.FileStorageClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class CatalogItemService(
    private val catalogItemRepository: CatalogItemRepository,
    private val categoryService: CategoryService,
    private val fileStorageClient: FileStorageClient,
    @Value("\${catalog.images.storage-route}") private val storageRoute: String
) {

    @Transactional
    fun createCatalogItem(request: CreateServiceRequest, images: List<MultipartFile>): CatalogItem {
        validateImages(images)
        validateUniqueness(request)

        require(images.isNotEmpty()) { FatumServiceException.NO_SERVICE_IMAGES }
        if (images.size > MAX_IMAGES) throw FatumServiceException(FatumServiceException.TOO_MANY_IMAGES)

        val serviceImages = images.map { image ->
            val uploaded = fileStorageClient.upload(image, storageRoute)
            ServiceImage(
                imageKey = uploaded.key,
                originalFilename = uploaded.originalFilename,
                contentType = uploaded.contentType,
                fileSize = uploaded.size
            )
        }

        val category = categoryService.findCategory(request.category)
        return catalogItemRepository.save(CatalogItemMapper.toEntity(request, serviceImages, category))
    }

    fun findByTitle(title: String): List<CatalogItem> =
        catalogItemRepository.findByTitleIgnoreCase(title)

    fun findByTitleContaining(keyword: String): List<CatalogItem> =
        catalogItemRepository.findByTitleContainingIgnoreCase(keyword)

    fun findByTitleStartingWith(prefix: String): List<CatalogItem> =
        catalogItemRepository.findByTitleStartingWithIgnoreCase(prefix)

    fun searchInCatalog(keyword: String): List<CatalogItem> =
        catalogItemRepository.searchInCatalog(keyword)

    fun findAll(): List<CatalogItem> =
        catalogItemRepository.findAll()

    private fun validateUniqueness(request: CreateServiceRequest) {
        val category = categoryService.findCategory(request.category)
        if (catalogItemRepository.existsByCategoryAndProviderIdIgnoreCase(category, request.providerId))
            throw FatumServiceException(FatumServiceException.UK_SERVICES_CATEGORY_PROVIDER)
        if (catalogItemRepository.existsByTitleAndProviderIdIgnoreCase(request.title, request.providerId))
            throw FatumServiceException(FatumServiceException.UK_SERVICES_TITLE_PROVIDER)
    }

    private fun validateImages(images: List<MultipartFile>) {
        if (images.isEmpty()) return
        for (image in images) {
            if (image.isEmpty) throw FatumServiceException(FatumServiceException.INVALID_IMAGE)
            if (image.size > MAX_IMAGE_SIZE) throw FatumServiceException(FatumServiceException.IMAGE_TOO_LARGE)
            val contentType = image.contentType?.lowercase()
            if (contentType == null || contentType !in ALLOWED_IMAGE_TYPES)
                throw FatumServiceException(FatumServiceException.INVALID_IMAGE_FORMAT)
        }
    }

    companion object {
        private const val MAX_IMAGES = 10
        private const val MAX_IMAGE_SIZE = 3L * 1024 * 1024
        private val ALLOWED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }
}