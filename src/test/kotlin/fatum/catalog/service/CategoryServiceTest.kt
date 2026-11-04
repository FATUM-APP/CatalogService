package fatum.catalog.service


import fatum.catalog.dto.request.category.CreateCategoryRequest
import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Category
import fatum.catalog.model.constant.JobType
import fatum.catalog.model.constant.PriceType
import fatum.catalog.model.constant.SchedulingModel
import fatum.catalog.model.constant.ServiceStatus
import fatum.catalog.repository.CategoryRepository
import io.github.fatumapp.storage.FileStorageClient
import io.github.fatumapp.storage.StoredFile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.multipart.MultipartFile
import kotlin.test.assertContains
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class CategoryServiceTest {

    @Mock lateinit var repository: CategoryRepository
    @Mock lateinit var fileStorageClient: FileStorageClient

    private lateinit var service: CategoryService
    private lateinit var belleza: Category

    @BeforeEach
    fun setUp() {
        service = CategoryService(repository, fileStorageClient, "test-logo-route")
        belleza = Category("Belleza", SchedulingModel.SINGLE_SESSION)
    }

    private fun validImage(): MultipartFile {
        val file = mock<MultipartFile>()
        whenever(file.isEmpty).thenReturn(false)
        whenever(file.size).thenReturn(1024)
        whenever(file.contentType).thenReturn("image/jpeg")
        return file
    }

    private fun uploadResult(key: String = "logo-key-1") = StoredFile(
        id = null, key = key, bucket = null,
        originalFilename = "logo.jpg", contentType = "image/jpeg", size = 1024
    )

    @Nested
    inner class CreateCategory {
        @Test
        fun `createRoot should throw CATEGORY_EXISTS when name already exists`() {
            whenever(repository.existsByNameIgnoreCase("Belleza")).thenReturn(true)

            val request = CreateCategoryRequest(
                name = "belleza",
                schedulingModel = SchedulingModel.SINGLE_SESSION
            )

            val exception = assertThrows<FatumServiceException> {
                service.createRoot(request, null)
            }

            assertEquals(CatalogErrorCode.CATEGORY_EXISTS, exception.errorCode)
            verify(repository, never()).save(any())
        }

        @Test
        fun `createRoot should upload logo when provided`() {
            whenever(repository.existsByNameIgnoreCase("Belleza")).thenReturn(false)
            val image = validImage()
            whenever(fileStorageClient.upload(image, "test-logo-route")).thenReturn(uploadResult())

            val captor = argumentCaptor<Category>()
            whenever(repository.save(captor.capture())).thenAnswer { it.arguments[0] }

            val request = CreateCategoryRequest(
                name = "Belleza",
                schedulingModel = SchedulingModel.SINGLE_SESSION
            )
            service.createRoot(request, image)

            val saved = captor.firstValue
            assertEquals("Belleza", saved.name)
            assertEquals("logo-key-1", saved.logoKey)
            verify(fileStorageClient).upload(image, "test-logo-route")
        }

        @Test
        fun `createRoot should work without logo`() {
            whenever(repository.existsByNameIgnoreCase("Belleza")).thenReturn(false)

            val captor = argumentCaptor<Category>()
            whenever(repository.save(captor.capture())).thenAnswer { it.arguments[0] }

            val request = CreateCategoryRequest(
                name = "Belleza",
                schedulingModel = SchedulingModel.SINGLE_SESSION
            )
            service.createRoot(request, null)

            val saved = captor.firstValue
            assertEquals("Belleza", saved.name)
            assertNull(saved.logoKey)
            verify(fileStorageClient, never()).upload(any(), any())
        }

        @Test
        fun `createSubCategory should throw CATEGORY_EXISTS when name already exists`() {
            whenever(repository.findByNameIgnoreCase("belleza")).thenReturn(belleza)
            whenever(repository.existsByNameIgnoreCase("Uñas")).thenReturn(true)

            val request = CreateCategoryRequest(
                name = "uñas",
                _parent = "belleza",
                schedulingModel = SchedulingModel.SINGLE_SESSION
            )

            val exception = assertThrows<FatumServiceException> {
                service.createSubCategory(request, null)
            }

            assertEquals(CatalogErrorCode.CATEGORY_EXISTS, exception.errorCode)
            verify(repository, never()).save(any())
        }

        @Test
        fun `createSubCategory should throw when parent does not exist`() {
            whenever(repository.findByNameIgnoreCase("Belleza")).thenReturn(null)

            val request = CreateCategoryRequest(
                name = "Uñas",
                _parent = "Belleza",
                schedulingModel = SchedulingModel.SINGLE_SESSION
            )

            val exception = assertThrows<FatumServiceException> {
                service.createSubCategory(request, null)
            }

            assertEquals(CatalogErrorCode.CATEGORY_NOT_FOUND, exception.errorCode)
            verify(repository).findByNameIgnoreCase("Belleza")
            verify(repository, never()).save(any())
        }

        @Test
        fun `should create root category`() {
            whenever(repository.existsByNameIgnoreCase("Belleza")).thenReturn(false)

            val captor = argumentCaptor<Category>()
            whenever(repository.save(captor.capture())).thenAnswer { it.arguments[0] }

            val request = CreateCategoryRequest(
                name = "Belleza",
                schedulingModel = SchedulingModel.SINGLE_SESSION
            )
            service.createRoot(request, null)

            val saved = captor.firstValue
            assertEquals("Belleza", saved.name)
            assertEquals(SchedulingModel.SINGLE_SESSION, saved.schedulingModel)
            verify(repository).save(any())
            verify(repository).existsByNameIgnoreCase("Belleza")
        }

        @Test
        fun `should create subcategory`() {
            whenever(repository.existsByNameIgnoreCase("Uñas")).thenReturn(false)
            whenever(repository.findByNameIgnoreCase("Belleza")).thenReturn(belleza)

            val captor = argumentCaptor<Category>()
            whenever(repository.save(captor.capture())).thenAnswer { it.arguments[0] }

            val request = CreateCategoryRequest(
                name = "Uñas",
                _parent = "Belleza",
                schedulingModel = SchedulingModel.SINGLE_SESSION
            )
            service.createSubCategory(request, null)

            val saved = captor.firstValue
            verify(repository).findByNameIgnoreCase("Belleza")
            assertEquals(belleza, saved.parent)
            assertContains(belleza.subcategories, saved)
            verify(repository).save(any())
            verify(repository).existsByNameIgnoreCase("Uñas")
        }

        @Test
        fun `createRoot should throw INVALID_IMAGE when logo is empty`() {
            val image = mock<MultipartFile>()
            whenever(image.isEmpty).thenReturn(true)

            val request = CreateCategoryRequest(
                name = "Belleza",
                schedulingModel = SchedulingModel.SINGLE_SESSION
            )

            val exception = assertThrows<FatumServiceException> {
                service.createRoot(request, image)
            }

            assertEquals(CatalogErrorCode.INVALID_IMAGE, exception.errorCode)
            verify(repository, never()).save(any())
        }

        @Test
        fun `createRoot should throw IMAGE_TOO_LARGE when logo exceeds limit`() {
            val image = mock<MultipartFile>()
            whenever(image.isEmpty).thenReturn(false)
            whenever(image.size).thenReturn(4L * 1024 * 1024)

            val request = CreateCategoryRequest(
                name = "Belleza",
                schedulingModel = SchedulingModel.SINGLE_SESSION
            )

            val exception = assertThrows<FatumServiceException> {
                service.createRoot(request, image)
            }

            assertEquals(CatalogErrorCode.IMAGE_TOO_LARGE, exception.errorCode)
            verify(repository, never()).save(any())
        }

        @Test
        fun `createRoot should throw INVALID_IMAGE_FORMAT for unsupported type`() {
            val image = mock<MultipartFile>()
            whenever(image.isEmpty).thenReturn(false)
            whenever(image.size).thenReturn(1024)
            whenever(image.contentType).thenReturn("image/gif")

            val request = CreateCategoryRequest(
                name = "Belleza",
                schedulingModel = SchedulingModel.SINGLE_SESSION
            )

            val exception = assertThrows<FatumServiceException> {
                service.createRoot(request, image)
            }

            assertEquals(CatalogErrorCode.INVALID_IMAGE_FORMAT, exception.errorCode)
            verify(repository, never()).save(any())
        }
    }

    @Nested
    inner class FindCategory {

        @Test
        fun `should find category by name when isAble is not provided`() {
            whenever(repository.findByNameIgnoreCase("Belleza")).thenReturn(belleza)
            val found = service.findCategory("Belleza")
            assertEquals(belleza, found)
            verify(repository).findByNameIgnoreCase("Belleza")
            verify(repository, never()).findByNameIgnoreCaseAndIsAble(any(), any())
        }

        @Test
        fun `should find category by name and isAble when isAble is provided`() {
            whenever(repository.findByNameIgnoreCaseAndIsAble("Belleza", true))
                .thenReturn(belleza)

            val found = service.findCategory("Belleza", true)

            assertEquals(belleza, found)
            verify(repository).findByNameIgnoreCaseAndIsAble("Belleza", true)
            verify(repository, never()).findByNameIgnoreCase(any())
        }

        @Test
        fun `should throw CATEGORY_NOT_FOUND when category does not exist`() {
            whenever(repository.findByNameIgnoreCase("Inexistente")).thenReturn(null)

            val exception = assertThrows<FatumServiceException> {
                service.findCategory("Inexistente")
            }

            assertEquals(CatalogErrorCode.CATEGORY_NOT_FOUND, exception.errorCode)
        }
    }

    @Nested
    inner class SearchCategoryServices {

        @Test
        fun `should return services from able categories`() {
            val pelo = Category("Pelo", belleza)
            val unas = Category("Uñas", belleza)
            unas.disable()

            val corte = CatalogItem(
                providerId = "provider-1", title = "Corte", priceType = PriceType.FIXED,
                providerName = "Provider Test", providerUsername = "providertest",
                jobType = JobType.PRESENCIAL, category = pelo
            )

            val tinte = CatalogItem(
                providerId = "provider-2", title = "Tinte", priceType = PriceType.FIXED,
                providerName = "Provider Test", providerUsername = "providertest",
                jobType = JobType.PRESENCIAL, category = pelo
            ).apply { status = ServiceStatus.SUSPENDED }

            CatalogItem(
                providerId = "provider-3", title = "Manicure", priceType = PriceType.FIXED,
                providerName = "Provider Test", providerUsername = "providertest",
                jobType = JobType.PRESENCIAL, category = unas
            )

            whenever(repository.findByNameIgnoreCaseAndIsAble("Belleza", true)).thenReturn(belleza)

            val result = service.searchCategoryServices("Belleza", null)

            assertEquals(setOf(corte,tinte), result)
            verify(repository).findByNameIgnoreCaseAndIsAble("Belleza", true)
        }

        @Test
        fun `should return services from  depending on status filter`() {
            val pelo = Category("Pelo", belleza)
            val corte = CatalogItem(
                providerId = "provider-1", title = "Corte", priceType = PriceType.FIXED,
                providerName = "Provider Test", providerUsername = "providertest",
                jobType = JobType.PRESENCIAL, category = pelo
            ).apply { status = ServiceStatus.SUSPENDED }

            val tinte = CatalogItem(
                providerId = "provider-1", title = "Tinte", priceType = PriceType.FIXED,
                providerName = "Provider Test", providerUsername = "providertest",
                jobType = JobType.PRESENCIAL, category = pelo
            )

            whenever(repository.findByNameIgnoreCaseAndIsAble("Belleza", true)).thenReturn(belleza)

            val result = service.searchCategoryServices("Belleza")
            val result2 = service.searchCategoryServices("Belleza", status = ServiceStatus.SUSPENDED)
            val result3 = service.searchCategoryServices("Belleza", status = ServiceStatus.INACTIVE)

            assertAll(
                { assertEquals(setOf(corte), result2) },
                { assertEquals(setOf(tinte), result) },
                { assertEquals(setOf<CatalogItem>(), result3) }
            )

        }

        @Test
        fun `should return all statuses when status filter is null`() {
            val pelo = Category("Pelo", belleza)
            CatalogItem(
                providerId = "provider-1", title = "Corte", priceType = PriceType.FIXED,
                providerName = "Provider Test", providerUsername = "providertest",
                jobType = JobType.PRESENCIAL, category = pelo
            ).apply { status = ServiceStatus.SUSPENDED }

            CatalogItem(
                providerId = "provider-1", title = "Tinte", priceType = PriceType.FIXED,
                providerName = "Provider Test", providerUsername = "providertest",
                jobType = JobType.PRESENCIAL, category = pelo
            )

            whenever(repository.findByNameIgnoreCaseAndIsAble("Belleza", true)).thenReturn(belleza)

            val result = service.searchCategoryServices("Belleza", status = null)

            assertEquals(2, result.size)
            verify(repository).findByNameIgnoreCaseAndIsAble("Belleza", true)
        }
    }

    @Nested
    inner class SearchRootCategories {

        @Test
        fun `should return root categories filtered by isAble when provided`() {
            val mascotas = Category("Mascotas", SchedulingModel.SINGLE_SESSION)
            whenever(repository.findByParentIsNullAndIsAble(true))
                .thenReturn(listOf(belleza, mascotas))

            val result = service.searchRootCategories(true)

            assertEquals(listOf(belleza, mascotas), result)
            verify(repository).findByParentIsNullAndIsAble(true)
            verify(repository, never()).findByParentIsNull()
        }

        @Test
        fun `should return all root categories when isAble is not provided`() {
            val mascotas = Category("Mascotas", SchedulingModel.SINGLE_SESSION)
            whenever(repository.findByParentIsNull())
                .thenReturn(listOf(belleza, mascotas))

            val result = service.searchRootCategories()

            assertEquals(listOf(belleza,mascotas), result)
            verify(repository).findByParentIsNull()
            verify(repository, never()).findByParentIsNullAndIsAble(any())
        }
    }

    @Nested
    inner class SearchSubCategories {

        @Test
        fun `should return all subcategories when areAble is not provided`() {
            val pelo = Category("Pelo", belleza)
            val unas = Category("Uñas", belleza)
            unas.disable()

            whenever(repository.findByNameIgnoreCaseAndIsAble("Belleza", true)).thenReturn(belleza)

            val result = service.searchSubCategories("Belleza")

            assertEquals(setOf(pelo, unas), result)
            verify(repository).findByNameIgnoreCaseAndIsAble("Belleza", true)
        }

        @Test
        fun `should return only able subcategories when areAble is true`() {
            val pelo = Category("Pelo", belleza)
            val unas = Category("Uñas", belleza)
            unas.disable()

            whenever(repository.findByNameIgnoreCaseAndIsAble("Belleza", true)).thenReturn(belleza)

            val result = service.searchSubCategories("Belleza", areAble = true)

            assertEquals(setOf(pelo), result)
            verify(repository).findByNameIgnoreCaseAndIsAble("Belleza", true)
        }

        @Test
        fun `should return only disabled subcategories when areAble is false`() {
            val pelo = Category("Pelo", belleza)
            val unas = Category("Uñas", belleza)
            unas.disable()

            whenever(repository.findByNameIgnoreCaseAndIsAble("Belleza", true)).thenReturn(belleza)

            val result = service.searchSubCategories("Belleza", areAble = false)

            assertEquals(setOf(unas), result)
            verify(repository).findByNameIgnoreCaseAndIsAble("Belleza", true)
        }
    }

    @Nested
    inner class DisableCategory {

        @Test
        fun `should disable an able category and save it`() {
            whenever(repository.findByNameIgnoreCase("Belleza")).thenReturn(belleza)

            val result = service.disableCategory("Belleza")

            assertFalse(result)
            assertFalse(belleza.isAble)
            verify(repository).save(belleza)
        }

        @Test
        fun `should not save when category is already disabled`() {
            belleza.disable()
            whenever(repository.findByNameIgnoreCase("Belleza")).thenReturn(belleza)

            val result = service.disableCategory("Belleza")

            assertFalse(result)
            verify(repository, never()).save(any())
        }
    }

    @Nested
    inner class EnableCategory {

        @Test
        fun `should enable a disabled category and save it`() {
            belleza.disable()
            whenever(repository.findByNameIgnoreCase("Belleza")).thenReturn(belleza)

            val result = service.enableCategory("Belleza")

            assertTrue(result)
            assertTrue(belleza.isAble)
            verify(repository).save(belleza)
        }

        @Test
        fun `should not save when category is already able`() {
            whenever(repository.findByNameIgnoreCase("Belleza")).thenReturn(belleza)

            val result = service.enableCategory("Belleza")

            assertTrue(result)
            verify(repository, never()).save(any())
        }

        @Test
        fun `should enable a disabled subcategory when parent is able`() {
            val child = Category("Uñas", belleza)
            child.disable()
            whenever(repository.findByNameIgnoreCase("Uñas")).thenReturn(child)

            val result = service.enableCategory("Uñas")

            assertTrue(result)
            assertTrue(child.isAble)
            verify(repository).save(child)
        }

        @Test
        fun `should throw when parent is disabled`() {
            val child = Category("Uñas", belleza)
            belleza.disable()
            whenever(repository.findByNameIgnoreCase("Uñas")).thenReturn(child)

            val exception = assertThrows<FatumServiceException> {
                service.enableCategory("Uñas")
            }

            assertEquals(CatalogErrorCode.ENABLING_SUB_WITH_ROOT_DISABLED, exception.errorCode)
            verify(repository, never()).save(any())
        }
    }

    @Nested
    inner class DeleteCategory {

        @Test
        fun `should delete category without services`() {
            whenever(repository.findByNameIgnoreCase("Belleza")).thenReturn(belleza)

            service.deleteCategory("Belleza")

            verify(repository).delete(belleza)
            verify(repository, never()).save(any())
        }

        @Test
        fun `should disable category with services instead of deleting`() {
            CatalogItem(
                providerId = "provider-123", title = "Corte", priceType = PriceType.FIXED,
                providerName = "Provider Test", providerUsername = "providertest",
                jobType = JobType.PRESENCIAL, category = belleza
            )
            whenever(repository.findByNameIgnoreCase("Belleza")).thenReturn(belleza)
            whenever(repository.save(belleza)).thenReturn(belleza)
            service.deleteCategory("Belleza")

            assertFalse(belleza.isAble)
            assertTrue(belleza.services.isNotEmpty())
            verify(repository).save(belleza)
            verify(repository, never()).delete(any())
        }
    }

    @Nested
    inner class UpdateLogo {

        @Test
        fun `should upload logo for category without existing logo`() {
            whenever(repository.findByNameIgnoreCase("Belleza")).thenReturn(belleza)
            val image = validImage()
            whenever(fileStorageClient.upload(image, "test-logo-route")).thenReturn(uploadResult())

            service.updateLogo("Belleza", image)

            assertEquals("logo-key-1", belleza.logoKey)
            verify(fileStorageClient).upload(image, "test-logo-route")
            verify(fileStorageClient, never()).delete(any(), any())
            verify(repository).save(belleza)
        }

        @Test
        fun `should delete old logo before uploading new one`() {
            belleza.logoKey = "old-logo-key"
            whenever(repository.findByNameIgnoreCase("Belleza")).thenReturn(belleza)
            val image = validImage()
            whenever(fileStorageClient.upload(image, "test-logo-route")).thenReturn(uploadResult("new-logo-key"))

            service.updateLogo("Belleza", image)

            assertEquals("new-logo-key", belleza.logoKey)
            verify(fileStorageClient).delete("test-logo-route", "old-logo-key")
            verify(fileStorageClient).upload(image, "test-logo-route")
            verify(repository).save(belleza)
        }

        @Test
        fun `should throw INVALID_IMAGE when image is empty`() {
            whenever(repository.findByNameIgnoreCase("Belleza")).thenReturn(belleza)
            val image = mock<MultipartFile>()
            whenever(image.isEmpty).thenReturn(true)

            val exception = assertThrows<FatumServiceException> {
                service.updateLogo("Belleza", image)
            }

            assertEquals(CatalogErrorCode.INVALID_IMAGE, exception.errorCode)
            verify(fileStorageClient, never()).upload(any(), any())
        }

        @Test
        fun `should throw IMAGE_TOO_LARGE when image exceeds max size`() {
            whenever(repository.findByNameIgnoreCase("Belleza")).thenReturn(belleza)
            val image = mock<MultipartFile>()
            whenever(image.isEmpty).thenReturn(false)
            whenever(image.size).thenReturn(4L * 1024 * 1024)

            val exception = assertThrows<FatumServiceException> {
                service.updateLogo("Belleza", image)
            }

            assertEquals(CatalogErrorCode.IMAGE_TOO_LARGE, exception.errorCode)
            verify(fileStorageClient, never()).upload(any(), any())
        }

        @Test
        fun `should throw INVALID_IMAGE_FORMAT when content type is not allowed`() {
            whenever(repository.findByNameIgnoreCase("Belleza")).thenReturn(belleza)
            val image = mock<MultipartFile>()
            whenever(image.isEmpty).thenReturn(false)
            whenever(image.size).thenReturn(1024)
            whenever(image.contentType).thenReturn("image/gif")

            val exception = assertThrows<FatumServiceException> {
                service.updateLogo("Belleza", image)
            }

            assertEquals(CatalogErrorCode.INVALID_IMAGE_FORMAT, exception.errorCode)
            verify(fileStorageClient, never()).upload(any(), any())
        }

        @Test
        fun `should throw CATEGORY_NOT_FOUND when category does not exist`() {
            whenever(repository.findByNameIgnoreCase("Inexistente")).thenReturn(null)
            val image = mock<MultipartFile>()

            val exception = assertThrows<FatumServiceException> {
                service.updateLogo("Inexistente", image)
            }

            assertEquals(CatalogErrorCode.CATEGORY_NOT_FOUND, exception.errorCode)
        }
    }

    @Nested
    inner class GetCategoryLogoUrl {

        @Test
        fun `should return presigned url when logo exists`() {
            belleza.logoKey = "logo-key"
            whenever(fileStorageClient.presignedUrl("test-logo-route", "logo-key"))
                .thenReturn("https://storage.example.com/logo-key")

            val url = service.getCategoryLogoUrl(belleza)

            assertEquals("https://storage.example.com/logo-key", url)
        }

        @Test
        fun `should return null when no logo`() {
            val url = service.getCategoryLogoUrl(belleza)

            assertNull(url)
        }
    }
}
