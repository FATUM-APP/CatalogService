package fatum.catalog.model

import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.constant.*
import fatum.catalog.model.vo.DualConfirmation
import fatum.catalog.model.vo.QuestionAnswer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDateTime

class OrderTest {

    private lateinit var category: Category
    private lateinit var catalogItem: CatalogItem

    @BeforeEach
    fun setUp() {
        category = Category("Plomeria", SchedulingModel.SINGLE_SESSION)
        catalogItem = CatalogItem(
            providerId = "provider-123",
            title = "Reparacion De Tuberias",
            priceType = PriceType.FIXED,
            jobType = JobType.PRESENCIAL,
            category = category,
            basePrice = BigDecimal(100),
            defaultEstimatedTime = 60
        )
    }

    private fun createOrder(
        clientId: String = "client-456",
        clientName: String = "juanperez",
        booking: LocalDateTime = LocalDateTime.of(2026, 11, 15, 10, 0),
        item: CatalogItem = catalogItem,
        negotiatedPrice: BigDecimal = item.basePrice,
        deliveryCity: String = "Bogota",
        deliveryState: String = "Cundinamarca",
        deliveryCountry: String = "Colombia",
        deliveryResidence: String = "Calle 123 #45-67",
        answers: Set<QuestionAnswer> = emptySet(),
        requirements: String? = null
    ) = Order(
        clientId = clientId,
        clientName = clientName,
        booking = booking,
        catalogItem = item,
        negotiatedPrice = negotiatedPrice,
        deliveryCity = deliveryCity,
        deliveryState = deliveryState,
        deliveryCountry = deliveryCountry,
        deliveryResidence = deliveryResidence,
        answers = answers,
        requirements = requirements
    )

    @Nested
    inner class Creation {

        @Test
        fun `should create order with default values from catalog item`() {
            val order = createOrder()

            assertAll(
                { assertEquals("client-456", order.clientId) },
                { assertEquals("juanperez", order.clientName) },
                { assertEquals("provider-123", order.providerId) },
                { assertEquals(BigDecimal(100), order.negotiatedPrice) },
                { assertEquals(60, order.estimatedTime) },
                { assertEquals(OrderStatus.PENDING, order.status) },
                { assertNotNull(order.createdAt) },
                { assertNull(order.deliveryStartedAt) },
                { assertNull(order.reason) },
                { assertTrue(order.answers.isEmpty()) },
                { assertNull(order.requirements) }
            )
        }

        @Test
        fun `should create order with custom negotiated price`() {
            val order = createOrder(negotiatedPrice = BigDecimal(250))

            assertEquals(BigDecimal(250), order.negotiatedPrice)
        }

        @Test
        fun `should derive estimated time from catalog item`() {
            val order = createOrder()

            assertEquals(catalogItem.defaultEstimatedTime, order.estimatedTime)
        }

        @Test
        fun `should allow modifying estimated time after creation`() {
            val order = createOrder()
            order.estimatedTime = 120

            assertEquals(120, order.estimatedTime)
        }

        @Test
        fun `should create order with answers and requirements`() {
            val answers = setOf(
                QuestionAnswer("Tipo de tuberia", setOf("PVC")),
                QuestionAnswer("Es urgente", setOf("true"))
            )
            val order = createOrder(answers = answers, requirements = "Llegar por la puerta trasera")

            assertAll(
                { assertEquals(2, order.answers.size) },
                { assertEquals("Llegar por la puerta trasera", order.requirements) }
            )
        }

        @Test
        fun `should inherit provider id from catalog item`() {
            val order = createOrder()
            assertEquals(catalogItem.providerId, order.providerId)
        }

        @Test
        fun `should throw NEGATIVE_PRICE on negative negotiated price`() {
            val ex = assertThrows<FatumServiceException> {
                createOrder(negotiatedPrice = BigDecimal(-1))
            }
            assertEquals(CatalogErrorCode.NEGATIVE_PRICE, ex.errorCode)
        }

        @Test
        fun `should throw MAX_ESTIMATED_TIME when estimated time exceeds 1440`() {
            val order = createOrder()

            val ex = assertThrows<FatumServiceException> {
                order.estimatedTime = 1441
            }
            assertEquals(CatalogErrorCode.MAX_ESTIMATED_TIME, ex.errorCode)
        }

        @Test
        fun `should throw MAX_ESTIMATED_TIME on negative estimated time`() {
            val order = createOrder()

            val ex = assertThrows<FatumServiceException> {
                order.estimatedTime = -1
            }
            assertEquals(CatalogErrorCode.MAX_ESTIMATED_TIME, ex.errorCode)
        }

        @Test
        fun `should allow null estimated time`() {
            val order = createOrder()
            order.estimatedTime = null
            assertNull(order.estimatedTime)
        }

        @Test
        fun `should allow zero negotiated price`() {
            val order = createOrder(negotiatedPrice = BigDecimal.ZERO)
            assertEquals(BigDecimal.ZERO, order.negotiatedPrice)
        }

        @Test
        fun `should allow estimated time of exactly 1440`() {
            val order = createOrder()
            order.estimatedTime = 1440
            assertEquals(1440, order.estimatedTime)
        }
    }

    @Nested
    inner class DualConfirmations {

        @Test
        fun `should initialize all confirmations as not confirmed`() {
            val order = createOrder()

            assertAll(
                { assertFalse(order.startConfirmation.isConfirmed) },
                { assertFalse(order.completionConfirmation.isConfirmed) },
                { assertFalse(order.paymentConfirmation.isConfirmed) }
            )
        }

        @Test
        fun `should confirm start by both parties`() {
            val order = createOrder()
            order.startConfirmation.confirm(ConfirmingParty.CUSTOMER)
            assertFalse(order.startConfirmation.isConfirmed)

            order.startConfirmation.confirm(ConfirmingParty.PROVIDER)
            assertTrue(order.startConfirmation.isConfirmed)
        }

        @Test
        fun `should confirm completion by both parties`() {
            val order = createOrder()
            order.completionConfirmation.confirm(ConfirmingParty.PROVIDER)
            assertFalse(order.completionConfirmation.isConfirmed)

            order.completionConfirmation.confirm(ConfirmingParty.CUSTOMER)
            assertTrue(order.completionConfirmation.isConfirmed)
        }

        @Test
        fun `should confirm payment by both parties`() {
            val order = createOrder()
            order.paymentConfirmation.confirm(ConfirmingParty.CUSTOMER)
            order.paymentConfirmation.confirm(ConfirmingParty.PROVIDER)
            assertTrue(order.paymentConfirmation.isConfirmed)
        }
    }

    @Nested
    inner class Equality {

        @Test
        fun `should be equal to itself`() {
            val order = createOrder()
            assertEquals(order, order)
        }

        @Test
        fun `two orders without id should not be equal`() {
            val order1 = createOrder()
            val order2 = createOrder()
            assertNotEquals(order1, order2)
        }

        @Test
        fun `should not be equal to non-Order`() {
            val order = createOrder()
            assertNotEquals(order, "not an order")
        }
    }
}
