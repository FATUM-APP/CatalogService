package fatum.catalog.service

import fatum.catalog.dto.CreateServiceRequest
import fatum.catalog.dto.UpdateServiceRequest
import fatum.catalog.dto.mapper.toEntity
import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Category
import fatum.catalog.model.ServiceImage
import fatum.catalog.model.constant.JobType
import fatum.catalog.model.constant.PriceType
import fatum.catalog.model.constant.SchedulingModel
import fatum.catalog.model.constant.ServiceStatus
import fatum.catalog.repository.CatalogItemRepository
import io.github.fatumapp.storage.FileStorageClient
import io.github.fatumapp.storage.StoredFile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class CatalogItemServiceTest {

    @Mock lateinit var catalogItemRepository: CatalogItemRepository
    @Mock lateinit var categoryService: CategoryService
    @Mock lateinit var fileStorageClient: FileStorageClient

    private lateinit var service: CatalogItemService
    private lateinit var category: Category

    @BeforeEach
    fun setUp() {
        service = CatalogItemService(catalogItemRepository, categoryService, fileStorageClient, "test-route")
        category = Category("Plomeria", SchedulingModel.SINGLE_SESSION)
    }

    private fun createItem(
        providerId: String = "provider-123",
        title: String = "Mi Servicio",
        priceType: PriceType = PriceType.FIXED,
        jobType: JobType = JobType.PRESENCIAL,
        cat: Category = this.category
    ) = CatalogItem(
        providerId = providerId, title = title,
        priceType = priceType, jobType = jobType, category = cat
    )

    private fun validImage(): MultipartFile {
        val file = mock<MultipartFile>()
        whenever(file.contentType).thenReturn("image/jpeg")
        whenever(file.size).thenReturn(1024L)
        whenever(file.isEmpty).thenReturn(false)
        return file
    }

    private fun uploadResult(key: String = "key-1") = StoredFile(
        id = null, key = key, bucket = null,
        originalFilename = "photo.jpg", contentType = "image/jpeg", size = 1024
    )

    private fun serviceImageWithId(id: String, imageKey: String = "key-1"): ServiceImage {
        val image = ServiceImage(
            imageKey = imageKey, originalFilename = "photo.jpg",
            contentType = "image/jpeg", fileSize = 1024
        )
        ServiceImage::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(image, id)
        }
        return image
    }

    // =========================================================================
    //  CLEAN DATA
    // =========================================================================

    @Nested
    inner class TrimSanitization {

        @Test
        fun `UpdateServiceRequest should trim id, title and description`() {
            val request = UpdateServiceRequest(
                _id = "   abc-123   ",
                _title = "  Mi Titulo  ",
                _description = "  Una descripcion  "
            )

            assertAll(
                { assertEquals("abc-123", request.id) },
                { assertEquals("Mi Titulo", request.title) },
                { assertEquals("Una descripcion", request.description) }
            )
        }

        @Test
        fun `UpdateServiceRequest should handle null title and description`() {
            val request = UpdateServiceRequest(_id = "  abc  ")

            assertAll(
                { assertEquals("abc", request.id) },
                { assertNull(request.title) },
                { assertNull(request.description) }
            )
        }

        @Test
        fun `CatalogItemMapper should trim providerId, title and description`() {
            val request = CreateServiceRequest(
                providerId = "   provider-1   ",
                title = "  Mi Servicio  ",
                description = "  Una descripcion  ",
                priceType = PriceType.FIXED,
                category = "Plomeria"
            )
            val category = Category("Plomeria", SchedulingModel.SINGLE_SESSION)

            val entity = request.toEntity(emptyList(), category)

            assertAll(
                { assertEquals("provider-1", entity.providerId) },
                { assertEquals("Mi Servicio", entity.title) },
                { assertEquals("Una descripcion", entity.description) }
            )
        }
    }

    // =========================================================================
    //  CREATE
    // =========================================================================

    @Nested
    inner class CreateCatalogItem {

        @Test
        fun `should create catalog item successfully`() {
            val image = validImage()
            val request = CreateServiceRequest(
                providerId = "provider-123", title = "Mi Servicio",
                priceType = PriceType.FIXED, category = "Plomeria"
            )

            whenever(catalogItemRepository.existsByCategoryNameAndProviderIdIgnoreCase("Plomeria", "provider-123"))
                .thenReturn(false)
            whenever(catalogItemRepository.existsByTitleAndProviderIdIgnoreCase("Mi Servicio", "provider-123"))
                .thenReturn(false)
            whenever(fileStorageClient.upload(any<MultipartFile>(), eq("test-route")))
                .thenReturn(uploadResult())
            whenever(categoryService.findCategory("Plomeria", true)).thenReturn(category)
            whenever(catalogItemRepository.save(any<CatalogItem>())).thenAnswer { it.arguments[0] }

            val result = service.createCatalogItem(request, listOf(image))

            assertAll(
                { assertEquals("Mi Servicio", result.title) },
                { assertEquals("provider-123", result.providerId) },
                { assertEquals(PriceType.FIXED, result.priceType) },
                { assertEquals(category, result.category) },
                { assertEquals(1, result.serviceCatalog.size) }
            )
            verify(catalogItemRepository).save(any())
            verify(fileStorageClient).upload(any<MultipartFile>(), eq("test-route"))
        }

        @Test
        fun `should throw NO_SERVICE_IMAGES when images list is empty`() {
            val request = CreateServiceRequest(
                providerId = "p", title = "t", priceType = PriceType.FIXED, category = "c"
            )

            val ex = assertThrows<FatumServiceException> {
                service.createCatalogItem(request, emptyList())
            }

            assertEquals(CatalogErrorCode.NO_SERVICE_IMAGES, ex.errorCode)
            verify(catalogItemRepository, never()).save(any())
        }

        @Test
        fun `should throw TOO_MANY_IMAGES when more than 10 images`() {
            val images = (1..11).map { mock<MultipartFile>() }
            val request = CreateServiceRequest(
                providerId = "p", title = "t", priceType = PriceType.FIXED, category = "c"
            )

            val ex = assertThrows<FatumServiceException> {
                service.createCatalogItem(request, images)
            }

            assertEquals(CatalogErrorCode.TOO_MANY_IMAGES, ex.errorCode)
        }

        @Test
        fun `should throw INVALID_IMAGE when image file is empty`() {
            val image = mock<MultipartFile>()
            whenever(image.isEmpty).thenReturn(true)

            val request = CreateServiceRequest(
                providerId = "p", title = "t", priceType = PriceType.FIXED, category = "c"
            )

            val ex = assertThrows<FatumServiceException> {
                service.createCatalogItem(request, listOf(image))
            }

            assertEquals(CatalogErrorCode.INVALID_IMAGE, ex.errorCode)
        }

        @Test
        fun `should throw IMAGE_TOO_LARGE when image exceeds 3MB`() {
            val image = mock<MultipartFile>()
            whenever(image.isEmpty).thenReturn(false)
            whenever(image.size).thenReturn(4L * 1024 * 1024)

            val request = CreateServiceRequest(
                providerId = "p", title = "t", priceType = PriceType.FIXED, category = "c"
            )

            val ex = assertThrows<FatumServiceException> {
                service.createCatalogItem(request, listOf(image))
            }

            assertEquals(CatalogErrorCode.IMAGE_TOO_LARGE, ex.errorCode)
        }

        @Test
        fun `should throw INVALID_IMAGE_FORMAT when content type is not allowed`() {
            val image = mock<MultipartFile>()
            whenever(image.isEmpty).thenReturn(false)
            whenever(image.size).thenReturn(1024L)
            whenever(image.contentType).thenReturn("application/pdf")

            val request = CreateServiceRequest(
                providerId = "p", title = "t", priceType = PriceType.FIXED, category = "c"
            )

            val ex = assertThrows<FatumServiceException> {
                service.createCatalogItem(request, listOf(image))
            }

            assertEquals(CatalogErrorCode.INVALID_IMAGE_FORMAT, ex.errorCode)
        }

        @Test
        fun `should throw INVALID_IMAGE_FORMAT when content type is null`() {
            val image = mock<MultipartFile>()
            whenever(image.isEmpty).thenReturn(false)
            whenever(image.size).thenReturn(1024L)
            whenever(image.contentType).thenReturn(null)

            val request = CreateServiceRequest(
                providerId = "p", title = "t", priceType = PriceType.FIXED, category = "c"
            )

            val ex = assertThrows<FatumServiceException> {
                service.createCatalogItem(request, listOf(image))
            }

            assertEquals(CatalogErrorCode.INVALID_IMAGE_FORMAT, ex.errorCode)
        }

        @Test
        fun `should throw UK_SERVICES_CATEGORY_PROVIDER when category provider already exists`() {
            val image = validImage()
            val request = CreateServiceRequest(
                providerId = "provider-123", title = "Mi Servicio",
                priceType = PriceType.FIXED, category = "Plomeria"
            )

            whenever(catalogItemRepository.existsByCategoryNameAndProviderIdIgnoreCase("Plomeria", "provider-123"))
                .thenReturn(true)

            val ex = assertThrows<FatumServiceException> {
                service.createCatalogItem(request, listOf(image))
            }

            assertEquals(CatalogErrorCode.UK_SERVICES_CATEGORY_PROVIDER, ex.errorCode)
            verify(catalogItemRepository, never()).save(any())
        }

        @Test
        fun `should throw UK_SERVICES_TITLE_PROVIDER when title provider already exists`() {
            val image = validImage()
            val request = CreateServiceRequest(
                providerId = "provider-123", title = "Mi Servicio",
                priceType = PriceType.FIXED, category = "Plomeria"
            )

            whenever(catalogItemRepository.existsByCategoryNameAndProviderIdIgnoreCase("Plomeria", "provider-123"))
                .thenReturn(false)
            whenever(catalogItemRepository.existsByTitleAndProviderIdIgnoreCase("Mi Servicio", "provider-123"))
                .thenReturn(true)

            val ex = assertThrows<FatumServiceException> {
                service.createCatalogItem(request, listOf(image))
            }

            assertEquals(CatalogErrorCode.UK_SERVICES_TITLE_PROVIDER, ex.errorCode)
            verify(catalogItemRepository, never()).save(any())
        }
    }

    // =========================================================================
    //  READ - findById
    // =========================================================================

    @Nested
    inner class FindById {

        @Test
        fun `should return item when found without status`() {
            val item = createItem()
            whenever(catalogItemRepository.findById("item-1")).thenReturn(Optional.of(item))

            val result = service.findById("item-1")

            assertSame(item, result)
        }

        @Test
        fun `should return item when found with status filter`() {
            val item = createItem()
            whenever(catalogItemRepository.findByIdFiltered("item-1", ServiceStatus.ACTIVE, true))
                .thenReturn(item)

            val result = service.findById("item-1", ServiceStatus.ACTIVE)

            assertSame(item, result)
            verify(catalogItemRepository).findByIdFiltered("item-1", ServiceStatus.ACTIVE, true)
        }

        @Test
        fun `should throw SERVICE_NOT_FOUND when not found`() {
            whenever(catalogItemRepository.findById("item-999")).thenReturn(Optional.empty())

            val ex = assertThrows<FatumServiceException> {
                service.findById("item-999")
            }

            assertEquals(CatalogErrorCode.SERVICE_NOT_FOUND, ex.errorCode)
        }

        @Test
        fun `should throw SERVICE_NOT_FOUND when found but status does not match`() {
            whenever(catalogItemRepository.findByIdFiltered("item-1", ServiceStatus.ACTIVE, true))
                .thenReturn(null)

            val ex = assertThrows<FatumServiceException> {
                service.findById("item-1", ServiceStatus.ACTIVE)
            }

            assertEquals(CatalogErrorCode.SERVICE_NOT_FOUND, ex.errorCode)
        }
    }

    // =========================================================================
    //  READ - findByTitle
    // =========================================================================

    @Nested
    inner class FindByTitle {

        @Test
        fun `should return items depending on status filter`() {
            val item = createItem()
            whenever(catalogItemRepository.findByTitle("Mi Servicio", ServiceStatus.ACTIVE, true))
                .thenReturn(listOf(item))
            whenever(catalogItemRepository.findByTitle("Mi Servicio", null, true))
                .thenReturn(listOf(item))
            whenever(catalogItemRepository.findByTitle("Mi Servicio", ServiceStatus.INACTIVE, true))
                .thenReturn(emptyList())

            val withActive = service.findByTitle("Mi Servicio")
            val withNull = service.findByTitle("Mi Servicio", null)
            val withInactive = service.findByTitle("Mi Servicio", ServiceStatus.INACTIVE)

            assertAll(
                { assertEquals(1, withActive.size) },
                { assertEquals(1, withNull.size) },
                { assertTrue(withInactive.isEmpty()) }
            )
        }

        @Test
        fun `should return empty list when no match`() {
            whenever(catalogItemRepository.findByTitle("Inexistente", ServiceStatus.ACTIVE, true))
                .thenReturn(emptyList())

            val result = service.findByTitle("Inexistente")

            assertTrue(result.isEmpty())
        }
    }

    // =========================================================================
    //  READ - search / autocomplete
    // =========================================================================

    @Nested
    inner class SearchAndAutocomplete {

        @Test
        fun `should delegate search by title containing keyword`() {
            val item = createItem()
            whenever(catalogItemRepository.searchByTitle("Serv", ServiceStatus.ACTIVE, true))
                .thenReturn(listOf(item))

            val result = service.findByTitleContaining("Serv")

            assertEquals(1, result.size)
            verify(catalogItemRepository).searchByTitle("Serv", ServiceStatus.ACTIVE, true)
        }

        @Test
        fun `should delegate autocomplete by title prefix`() {
            val item = createItem()
            whenever(catalogItemRepository.autocompleteByTitle("Mi", ServiceStatus.ACTIVE, true))
                .thenReturn(listOf(item))

            val result = service.findByTitleStartingWith("Mi")

            assertEquals(1, result.size)
            verify(catalogItemRepository).autocompleteByTitle("Mi", ServiceStatus.ACTIVE, true)
        }

        @Test
        fun `should delegate search in catalog`() {
            val item = createItem()
            whenever(catalogItemRepository.searchInCatalog("plom", null, true))
                .thenReturn(listOf(item))

            val result = service.searchInCatalog("plom")

            assertEquals(1, result.size)
            verify(catalogItemRepository).searchInCatalog("plom", null, true)
        }

        @Test
        fun `should return empty list when no search results`() {
            whenever(catalogItemRepository.searchByTitle("xyz", ServiceStatus.ACTIVE, true))
                .thenReturn(emptyList())
            whenever(catalogItemRepository.autocompleteByTitle("xyz", ServiceStatus.ACTIVE, true))
                .thenReturn(emptyList())
            whenever(catalogItemRepository.searchInCatalog("xyz", null, true))
                .thenReturn(emptyList())

            assertAll(
                { assertTrue(service.findByTitleContaining("xyz").isEmpty()) },
                { assertTrue(service.findByTitleStartingWith("xyz").isEmpty()) },
                { assertTrue(service.searchInCatalog("xyz").isEmpty()) }
            )
        }
    }

    // =========================================================================
    //  READ - filter / findAll
    // =========================================================================

    @Nested
    inner class FilterAndFindAll {

        @Test
        fun `should filter catalog with multiple criteria`() {
            val item = createItem()
            whenever(catalogItemRepository.filterCatalog(
                PriceType.FIXED, BigDecimal(10), BigDecimal(100), true, ServiceStatus.ACTIVE, true
            )).thenReturn(listOf(item))

            val result = service.filterCatalog(
                priceType = PriceType.FIXED,
                minPrice = BigDecimal(10),
                maxPrice = BigDecimal(100),
                isNegotiable = true,
                status = ServiceStatus.ACTIVE
            )

            assertEquals(1, result.size)
        }

        @Test
        fun `should find all items without filters`() {
            val item = createItem()
            whenever(catalogItemRepository.filterCatalog(
                null, BigDecimal.ZERO, null, null, null, true
            )).thenReturn(listOf(item))

            val result = service.findAll()

            assertEquals(1, result.size)
        }

        @Test
        fun `should return empty list when no items match`() {
            whenever(catalogItemRepository.filterCatalog(
                PriceType.HOURLY, BigDecimal(999), null, null, ServiceStatus.ACTIVE, true
            )).thenReturn(emptyList())

            val result = service.filterCatalog(
                priceType = PriceType.HOURLY,
                minPrice = BigDecimal(999)
            )

            assertTrue(result.isEmpty())
        }
    }

    // =========================================================================
    //  UPDATE
    // =========================================================================

    @Nested
    inner class UpdateCatalogItem {

        @Test
        fun `should update all fields and replace images`() {
            val oldImage = ServiceImage(
                imageKey = "old-key", originalFilename = "old.jpg",
                contentType = "image/jpeg", fileSize = 500
            )
            val item = createItem()
            item.serviceCatalog = mutableListOf(oldImage)

            val request = UpdateServiceRequest(
                _id = "item-1", _title = "Nuevo Titulo", _description = "Nueva desc",
                basePrice = BigDecimal(200), priceType = PriceType.HOURLY,
                isNegotiable = true, defaultEstimatedTime = 60, jobType = JobType.REMOTE
            )

            whenever(catalogItemRepository.findById("item-1")).thenReturn(Optional.of(item))
            whenever(fileStorageClient.upload(any<MultipartFile>(), eq("test-route")))
                .thenReturn(uploadResult("new-key"))
            whenever(catalogItemRepository.existsByTitleAndProviderIdIgnoreCase("Nuevo Titulo", "provider-123"))
                .thenReturn(false)
            whenever(catalogItemRepository.save(any<CatalogItem>())).thenAnswer { it.arguments[0] }

            val result = service.updateCatalogItem(request, listOf(validImage()))

            assertAll(
                { assertEquals("Nuevo Titulo", result.title) },
                { assertEquals("Nueva desc", result.description) },
                { assertEquals(BigDecimal(200), result.basePrice) },
                { assertEquals(PriceType.HOURLY, result.priceType) },
                { assertTrue(result.isNegotiable) },
                { assertEquals(60, result.defaultEstimatedTime) },
                { assertEquals(JobType.REMOTE, result.jobType) }
            )
            verify(fileStorageClient).delete("test-route", "old-key")
            verify(fileStorageClient).upload(any<MultipartFile>(), eq("test-route"))
        }

        @Test
        fun `should skip null fields on update`() {
            val item = createItem()
            val originalTitle = item.title
            val originalPrice = item.basePrice

            val request = UpdateServiceRequest(_id = "item-1")

            whenever(catalogItemRepository.findById("item-1")).thenReturn(Optional.of(item))
            whenever(fileStorageClient.upload(any<MultipartFile>(), eq("test-route")))
                .thenReturn(uploadResult())
            whenever(catalogItemRepository.save(any<CatalogItem>())).thenAnswer { it.arguments[0] }

            val result = service.updateCatalogItem(request, listOf(validImage()))

            assertAll(
                { assertEquals(originalTitle, result.title) },
                { assertEquals(originalPrice, result.basePrice) },
                { assertFalse(result.isNegotiable) }
            )
            verify(catalogItemRepository, never()).existsByTitleAndProviderIdIgnoreCase(any(), any())
        }

        @Test
        fun `should throw UK_SERVICES_TITLE_PROVIDER when new title already exists`() {
            val item = createItem()
            val request = UpdateServiceRequest(_id = "item-1", _title = "Titulo Existente")

            whenever(catalogItemRepository.findById("item-1")).thenReturn(Optional.of(item))
            whenever(fileStorageClient.upload(any<MultipartFile>(), eq("test-route")))
                .thenReturn(uploadResult())
            whenever(catalogItemRepository.existsByTitleAndProviderIdIgnoreCase("Titulo Existente", "provider-123"))
                .thenReturn(true)

            val ex = assertThrows<FatumServiceException> {
                service.updateCatalogItem(request, listOf(validImage()))
            }

            assertEquals(CatalogErrorCode.UK_SERVICES_TITLE_PROVIDER, ex.errorCode)
        }

        @Test
        fun `should throw SERVICE_NOT_FOUND when item does not exist`() {
            val request = UpdateServiceRequest(_id = "item-999")
            whenever(catalogItemRepository.findById("item-999")).thenReturn(Optional.empty())

            val ex = assertThrows<FatumServiceException> {
                service.updateCatalogItem(request, emptyList())
            }

            assertEquals(CatalogErrorCode.SERVICE_NOT_FOUND, ex.errorCode)
            verify(catalogItemRepository, never()).save(any())
        }
    }

    // =========================================================================
    //  ACTIVATE / DEACTIVATE
    // =========================================================================

    @Nested
    inner class ActivateDeactivate {

        @Test
        fun `should deactivate item`() {
            val item = createItem()
            whenever(catalogItemRepository.findById("item-1")).thenReturn(Optional.of(item))
            whenever(catalogItemRepository.save(any<CatalogItem>())).thenAnswer { it.arguments[0] }

            service.deactivate("item-1")

            assertEquals(ServiceStatus.INACTIVE, item.status)
            verify(catalogItemRepository).save(item)
        }

        @Test
        fun `should activate item`() {
            val item = createItem()
            item.status = ServiceStatus.INACTIVE

            whenever(catalogItemRepository.findById("item-1")).thenReturn(Optional.of(item))
            whenever(catalogItemRepository.save(any<CatalogItem>())).thenAnswer { it.arguments[0] }

            service.activate("item-1")

            assertEquals(ServiceStatus.ACTIVE, item.status)
            verify(catalogItemRepository).save(item)
        }

        @Test
        fun `should throw SERVICE_NOT_FOUND on deactivate when not found`() {
            whenever(catalogItemRepository.findById("item-999")).thenReturn(Optional.empty())

            val ex = assertThrows<FatumServiceException> {
                service.deactivate("item-999")
            }

            assertEquals(CatalogErrorCode.SERVICE_NOT_FOUND, ex.errorCode)
            verify(catalogItemRepository, never()).save(any())
        }
    }

    // =========================================================================
    //  SEE CATALOG IMAGES
    // =========================================================================

    @Nested
    inner class SeeCatalogImages {

        @Test
        fun `should return images with presigned URLs`() {
            val image = serviceImageWithId("img-1", "key-1")
            val item = createItem()
            item.serviceCatalog = mutableListOf(image)

            whenever(catalogItemRepository.findByIdFiltered("item-1", ServiceStatus.ACTIVE, true))
                .thenReturn(item)
            whenever(fileStorageClient.presignedUrl("test-route", "key-1"))
                .thenReturn("https://storage.example.com/photo.jpg")

            val result = service.seeCatalogImages("item-1", ServiceStatus.ACTIVE)

            assertAll(
                { assertEquals(1, result.size) },
                { assertEquals("img-1", result[0].id) },
                { assertEquals("photo.jpg", result[0].originalFilename) },
                { assertEquals("image/jpeg", result[0].contentType) },
                { assertEquals(1024L, result[0].size) },
                { assertEquals("https://storage.example.com/photo.jpg", result[0].url) }
            )
        }

        @Test
        fun `should return empty list when item has no images`() {
            val item = createItem()

            whenever(catalogItemRepository.findByIdFiltered("item-1", ServiceStatus.ACTIVE, true))
                .thenReturn(item)

            val result = service.seeCatalogImages("item-1", ServiceStatus.ACTIVE)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `should throw NO_IMAGE_URL when presigned URL is null`() {
            val image = serviceImageWithId("img-1", "key-1")
            val item = createItem()
            item.serviceCatalog = mutableListOf(image)

            whenever(catalogItemRepository.findByIdFiltered("item-1", ServiceStatus.ACTIVE, true))
                .thenReturn(item)
            whenever(fileStorageClient.presignedUrl("test-route", "key-1"))
                .thenReturn(null)

            val ex = assertThrows<FatumServiceException> {
                service.seeCatalogImages("item-1", ServiceStatus.ACTIVE)
            }

            assertEquals(CatalogErrorCode.NO_IMAGE_URL, ex.errorCode)
        }

        @Test
        fun `should throw NO_IMAGE_ID when image has no id`() {
            val image = ServiceImage(
                imageKey = "key-1", originalFilename = "photo.jpg",
                contentType = "image/jpeg", fileSize = 1024
            )
            val item = createItem()
            item.serviceCatalog = mutableListOf(image)

            whenever(catalogItemRepository.findByIdFiltered("item-1", ServiceStatus.ACTIVE, true))
                .thenReturn(item)
            whenever(fileStorageClient.presignedUrl("test-route", "key-1"))
                .thenReturn("https://storage.example.com/photo.jpg")

            val ex = assertThrows<FatumServiceException> {
                service.seeCatalogImages("item-1", ServiceStatus.ACTIVE)
            }

            assertEquals(CatalogErrorCode.NO_IMAGE_ID, ex.errorCode)
        }
    }
}
