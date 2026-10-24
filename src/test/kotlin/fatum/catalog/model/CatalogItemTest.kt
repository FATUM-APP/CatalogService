package fatum.catalog.model

import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.constant.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class CatalogItemTest {

    private lateinit var category: Category

    @BeforeEach
    fun setUp() {
        category = Category("Plomeria", SchedulingModel.SINGLE_SESSION)
    }

    private fun createItem(
        providerId: String = "provider-123",
        title: String = "Mi Servicio",
        priceType: PriceType = PriceType.FIXED,
        jobType: JobType = JobType.PRESENCIAL,
        category: Category = this.category,
        estimatedTime: Int? = null
    ) = CatalogItem(
        providerId = providerId,
        title = title,
        priceType = priceType,
        jobType = jobType,
        category = category,
        defaultEstimatedTime = estimatedTime
    )

    @Nested
    inner class Creation {
        @Test
        fun `should create item with default values`() {
            val item = createItem(title = "Mi Servicio")
            assertEquals("Mi Servicio", item.title)
            assertEquals(BigDecimal.ZERO, item.basePrice)
            assertEquals(ServiceStatus.ACTIVE, item.status)
            assertFalse(item.isNegotiable)
            assertNull(item.defaultEstimatedTime)
            assertNull(item.description)
            assertTrue(item.serviceCatalog.isEmpty())
            assertTrue(item.fixedQuestions.isEmpty())
        }

        @Test
        fun `should add automatically service in category`() {
            assertTrue(category.services.isEmpty())
            val item = createItem()
            assertEquals(category, item.category)
            assertTrue(category.services.contains(item))
        }

        @Test
        fun `should format title to title case`() {
            val item = createItem(title = "    limpieza de TUBERIAS    ")
            assertEquals("Limpieza De Tuberias", item.title)
        }

        @Test
        fun `should throw on blank providerId`() {
            val ex = assertThrows<FatumServiceException> {
                createItem(providerId = "   ")
            }
            assertEquals(CatalogErrorCode.BLANK, ex.errorCode)
        }

        @Test
        fun `should throw on blank title`() {
            val ex = assertThrows<FatumServiceException> {
                createItem(title = "   ")
            }
            assertEquals(CatalogErrorCode.BLANK, ex.errorCode)
        }

        @Test
        fun `should throw on negative base price`() {
            val ex = assertThrows<FatumServiceException> {
                val item = CatalogItem(
                    providerId = "123",
                    title = "Mi servicio",
                    priceType = PriceType.HOURLY,
                    jobType = JobType.PRESENCIAL,
                    category = category,
                    basePrice = BigDecimal(-1)
                )
            }
            assertEquals(CatalogErrorCode.NEGATIVE_PRICE, ex.errorCode)
        }
        @Test
        fun `should not create an item with an estimated time more than 1440`() {
            val ex = assertThrows<FatumServiceException> {
                createItem(estimatedTime = 1441)
            }
            assertEquals(CatalogErrorCode.MAX_ESTIMATED_TIME, ex.errorCode)
        }
        @Test
        fun `should be equals to itself`() {
            val item1 = createItem()
            val item2 = item1
            assertEquals(item1, item2)
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `should update catalog item`() {
            val item = createItem()
            item.title = "nuevo titulo"
            item.basePrice = BigDecimal(100)
            item.defaultEstimatedTime = 100
            assertEquals("Nuevo Titulo", item.title)
            assertEquals(BigDecimal(100), item.basePrice)
            assertEquals(100, item.defaultEstimatedTime)
        }

        @Test
        fun `should throw on blank title update`() {
            val item = createItem()
            val ex = assertThrows<FatumServiceException> {
                item.title = "   "
            }
            assertEquals(CatalogErrorCode.BLANK, ex.errorCode)
        }

        @Test
        fun `should toggle negotiable`() {
            val item = createItem()

            item.makeNegotiable()
            assertTrue(item.isNegotiable)

            item.makeNonNegotiable()
            assertFalse(item.isNegotiable)
        }

        @Test
        fun `should allow estimated time of exactly 1440`() {
            val item = createItem()
            item.defaultEstimatedTime = 1440
            assertEquals(1440, item.defaultEstimatedTime)
        }

        @Test
        fun `should throw on negative base price`() {
            val item = createItem()
            val ex = assertThrows<FatumServiceException> {
                item.basePrice = BigDecimal(-3000)
            }
            assertEquals(CatalogErrorCode.NEGATIVE_PRICE, ex.errorCode)
        }
        @Test
        fun `should throw on negative estimated time`() {
            val item = createItem()
            val ex = assertThrows<FatumServiceException> {
                item.defaultEstimatedTime = -5
            }
            assertEquals(CatalogErrorCode.NEGATIVE_ESTIMATED_TIME, ex.errorCode)
        }

        @Test
        fun `should allow null estimated time`() {
            val item = createItem()
            item.defaultEstimatedTime = null
            assertNull(item.defaultEstimatedTime)
        }

        @Test
        fun `should throw on estimated time more than 1440`() {
            val item = createItem()
            val ex = assertThrows<FatumServiceException> {
                item.defaultEstimatedTime = 1441
            }
            assertEquals(CatalogErrorCode.MAX_ESTIMATED_TIME, ex.errorCode)
        }
    }
}