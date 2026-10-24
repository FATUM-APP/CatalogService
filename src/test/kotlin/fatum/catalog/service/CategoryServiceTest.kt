package fatum.catalog.service


import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Category
import fatum.catalog.model.constant.JobType
import fatum.catalog.model.constant.PriceType
import fatum.catalog.model.constant.SchedulingModel
import fatum.catalog.model.constant.ServiceStatus
import fatum.catalog.repository.CategoryRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertContains
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class CategoryServiceTest {


    @Mock
    lateinit var repository: CategoryRepository
    @InjectMocks
    lateinit var service: CategoryService

    private lateinit var belleza: Category


    @BeforeEach
    fun setUp() {
        belleza = Category("Belleza", SchedulingModel.SINGLE_SESSION)
    }

    @Nested
    inner class CreateCategory {
        @Test
        fun `createRoot should throw CATEGORY_EXISTS when name already exists`() {
            whenever(repository.existsByNameIgnoreCase("belleza")).thenReturn(true)

            val exception = assertThrows<FatumServiceException> {
                service.createRoot("belleza", SchedulingModel.SINGLE_SESSION)
            }

            assertEquals(CatalogErrorCode.CATEGORY_EXISTS, exception.errorCode)
            verify(repository, never()).save(any())
        }

        @Test
        fun `createSubCategory should throw CATEGORY_EXISTS when name already exists`() {
            whenever(repository.existsByNameIgnoreCase("uñas")).thenReturn(true)

            val exception = assertThrows<FatumServiceException> {
                service.createSubCategory("uñas", "belleza")
            }

            assertEquals(CatalogErrorCode.CATEGORY_EXISTS, exception.errorCode)
            verify(repository, never()).save(any())
        }

        @Test
        fun `createSubCategory should throw when parent does not exist`() {
            whenever(repository.existsByNameIgnoreCase("Uñas")).thenReturn(false)
            whenever(repository.findByNameIgnoreCase("Belleza")).thenReturn(null)

            val exception = assertThrows<FatumServiceException> {
                service.createSubCategory("Uñas", "Belleza")
            }

            assertEquals(CatalogErrorCode.CATEGORY_NOT_FOUND, exception.errorCode)
            verify(repository).existsByNameIgnoreCase("Uñas")
            verify(repository).findByNameIgnoreCase("Belleza")
            verify(repository, never()).save(any())
        }

        @Test
        fun `should create root category`() {
            whenever(repository.existsByNameIgnoreCase("Belleza")).thenReturn(false)

            val captor = argumentCaptor<Category>()
            whenever(repository.save(captor.capture())).thenAnswer { it.arguments[0] }

            val savedCategory = service.createRoot(
                "Belleza",SchedulingModel.SINGLE_SESSION
            )

            assertEquals("Belleza", savedCategory.name)
            assertEquals(SchedulingModel.SINGLE_SESSION, savedCategory.schedulingModel)
            verify(repository).save(any())
            verify(repository).existsByNameIgnoreCase("Belleza")
        }

        @Test
        fun `should create subcategory`() {
            whenever(repository.existsByNameIgnoreCase("Uñas")).thenReturn(false)
            whenever(repository.findByNameIgnoreCase("Belleza")).thenReturn(belleza)

            val captor = argumentCaptor<Category>()
            whenever(repository.save(captor.capture())).thenAnswer { it.arguments[0] }

            val savedCategory = service.createSubCategory("Uñas", "Belleza")

            verify(repository).findByNameIgnoreCase("Belleza")
            assertEquals(belleza, savedCategory.parent)
            assertContains(belleza.subcategories, savedCategory)
            verify(repository).save(any())
            verify(repository).existsByNameIgnoreCase("Uñas")
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
                jobType = JobType.PRESENCIAL, category = pelo
            )

            val tinte = CatalogItem(
                providerId = "provider-2", title = "Tinte", priceType = PriceType.FIXED,
                jobType = JobType.PRESENCIAL, category = pelo
            ).apply { status = ServiceStatus.SUSPENDED }

            CatalogItem(
                providerId = "provider-3", title = "Manicure", priceType = PriceType.FIXED,
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
                jobType = JobType.PRESENCIAL, category = pelo
            ).apply { status = ServiceStatus.SUSPENDED }

            val tinte = CatalogItem(
                providerId = "provider-1", title = "Tinte", priceType = PriceType.FIXED,
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
                jobType = JobType.PRESENCIAL, category = pelo
            ).apply { status = ServiceStatus.SUSPENDED }

            CatalogItem(
                providerId = "provider-1", title = "Tinte", priceType = PriceType.FIXED,
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



}