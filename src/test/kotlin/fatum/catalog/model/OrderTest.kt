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
            providerName = "Provider Test",
            providerUsername = "providertest",
            priceType = PriceType.FIXED,
            jobType = JobType.PRESENCIAL,
            category = category,
            basePrice = BigDecimal(100),
            defaultEstimatedTime = 60
        )
    }

    private fun createOrder(
        clientId: String = "client-456",
        clientName: String = "Juan Perez",
        clientUsername: String = "juanperez",
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
        clientUsername = clientUsername,
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

    private fun setStatus(order: Order, status: OrderStatus) {
        Order::class.java.getDeclaredField("status").apply {
            isAccessible = true
            set(order, status)
        }
    }

    @Nested
    inner class Creation {

        @Test
        fun `should create order with default values from catalog item`() {
            val order = createOrder()

            assertAll(
                { assertEquals("client-456", order.clientId) },
                { assertEquals("Juan Perez", order.clientName) },
                { assertEquals("juanperez", order.clientUsername) },
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
    inner class ChangeStatus {

        @Test
        fun `should transition from PENDING to ACCEPTED`() {
            val order = createOrder()
            order.changeStatus(OrderStatus.ACCEPTED)
            assertEquals(OrderStatus.ACCEPTED, order.status)
        }

        @Test
        fun `should transition from PENDING to REJECTED`() {
            val order = createOrder()
            order.changeStatus(OrderStatus.REJECTED)
            assertEquals(OrderStatus.REJECTED, order.status)
        }

        @Test
        fun `should transition from PENDING to CANCELLED with reason`() {
            val order = createOrder()
            order.changeStatus(OrderStatus.CANCELLED, "Cliente no disponible")

            assertAll(
                { assertEquals(OrderStatus.CANCELLED, order.status) },
                { assertEquals("Cliente no disponible", order.reason) }
            )
        }

        @Test
        fun `should transition from ACCEPTED to DELIVERING`() {
            val order = createOrder()
            setStatus(order, OrderStatus.ACCEPTED)

            order.changeStatus(OrderStatus.DELIVERING)
            assertEquals(OrderStatus.DELIVERING, order.status)
        }

        @Test
        fun `should transition from ACCEPTED to CANCELLED with reason`() {
            val order = createOrder()
            setStatus(order, OrderStatus.ACCEPTED)

            order.changeStatus(OrderStatus.CANCELLED, "Proveedor cancelo")
            assertEquals(OrderStatus.CANCELLED, order.status)
        }

        @Test
        fun `should transition from ACCEPTED to DISPUTED with reason`() {
            val order = createOrder()
            setStatus(order, OrderStatus.ACCEPTED)

            order.changeStatus(OrderStatus.DISPUTED, "Problema con el servicio")
            assertEquals(OrderStatus.DISPUTED, order.status)
        }

        @Test
        fun `should transition from DELIVERING to COMPLETED`() {
            val order = createOrder()
            setStatus(order, OrderStatus.DELIVERING)

            order.changeStatus(OrderStatus.COMPLETED)
            assertEquals(OrderStatus.COMPLETED, order.status)
        }

        @Test
        fun `should transition from DELIVERING to NOT_DELIVERED with reason`() {
            val order = createOrder()
            setStatus(order, OrderStatus.DELIVERING)

            order.changeStatus(OrderStatus.NOT_DELIVERED, "No se encontro al cliente")
            assertAll(
                { assertEquals(OrderStatus.NOT_DELIVERED, order.status) },
                { assertEquals("No se encontro al cliente", order.reason) }
            )
        }

        @Test
        fun `should transition from DELIVERING to CANCELLED with reason`() {
            val order = createOrder()
            setStatus(order, OrderStatus.DELIVERING)

            order.changeStatus(OrderStatus.CANCELLED, "Emergencia")
            assertEquals(OrderStatus.CANCELLED, order.status)
        }

        @Test
        fun `should transition from DELIVERING to DISPUTED with reason`() {
            val order = createOrder()
            setStatus(order, OrderStatus.DELIVERING)

            order.changeStatus(OrderStatus.DISPUTED, "Servicio incompleto")
            assertEquals(OrderStatus.DISPUTED, order.status)
        }

        @Test
        fun `should transition from COMPLETED to DISPUTED with reason`() {
            val order = createOrder()
            setStatus(order, OrderStatus.COMPLETED)

            order.changeStatus(OrderStatus.DISPUTED, "Cobro incorrecto")
            assertEquals(OrderStatus.DISPUTED, order.status)
        }

        @Test
        fun `should transition from DISPUTED to SOLVED with reason`() {
            val order = createOrder()
            setStatus(order, OrderStatus.DISPUTED)

            order.changeStatus(OrderStatus.SOLVED, "Se llego a un acuerdo")
            assertEquals(OrderStatus.SOLVED, order.status)
        }

        @Test
        fun `should transition from DISPUTED to REFUNDED with reason`() {
            val order = createOrder()
            setStatus(order, OrderStatus.DISPUTED)

            order.changeStatus(OrderStatus.REFUNDED, "Reembolso aprobado")
            assertEquals(OrderStatus.REFUNDED, order.status)
        }

        @Test
        fun `should throw INVALID_STATUS_TRANSITION for PENDING to DELIVERING`() {
            val order = createOrder()

            val ex = assertThrows<FatumServiceException> {
                order.changeStatus(OrderStatus.DELIVERING)
            }
            assertEquals(CatalogErrorCode.INVALID_STATUS_TRANSITION, ex.errorCode)
        }

        @Test
        fun `should throw INVALID_STATUS_TRANSITION for COMPLETED to CANCELLED`() {
            val order = createOrder()
            setStatus(order, OrderStatus.COMPLETED)

            val ex = assertThrows<FatumServiceException> {
                order.changeStatus(OrderStatus.CANCELLED, "No se puede")
            }
            assertEquals(CatalogErrorCode.INVALID_STATUS_TRANSITION, ex.errorCode)
        }

        @Test
        fun `should throw INVALID_STATUS_TRANSITION from terminal states`() {
            val terminalStates = listOf(
                OrderStatus.REJECTED, OrderStatus.NOT_DELIVERED,
                OrderStatus.CANCELLED, OrderStatus.SOLVED, OrderStatus.REFUNDED
            )

            terminalStates.forEach { terminal ->
                val order = createOrder()
                setStatus(order, terminal)

                val ex = assertThrows<FatumServiceException> {
                    order.changeStatus(OrderStatus.PENDING, "reason")
                }
                assertEquals(CatalogErrorCode.INVALID_STATUS_TRANSITION, ex.errorCode)
            }
        }

        @Test
        fun `should throw REASON_REQUIRED when reason is null for CANCELLED`() {
            val order = createOrder()

            val ex = assertThrows<FatumServiceException> {
                order.changeStatus(OrderStatus.CANCELLED)
            }
            assertEquals(CatalogErrorCode.REASON_REQUIRED, ex.errorCode)
        }

        @Test
        fun `should throw REASON_REQUIRED when reason is blank for DISPUTED`() {
            val order = createOrder()
            setStatus(order, OrderStatus.ACCEPTED)

            val ex = assertThrows<FatumServiceException> {
                order.changeStatus(OrderStatus.DISPUTED, "   ")
            }
            assertEquals(CatalogErrorCode.REASON_REQUIRED, ex.errorCode)
        }

        @Test
        fun `should throw REASON_REQUIRED for NOT_DELIVERED without reason`() {
            val order = createOrder()
            setStatus(order, OrderStatus.DELIVERING)

            val ex = assertThrows<FatumServiceException> {
                order.changeStatus(OrderStatus.NOT_DELIVERED)
            }
            assertEquals(CatalogErrorCode.REASON_REQUIRED, ex.errorCode)
        }

        @Test
        fun `should throw REASON_REQUIRED for SOLVED without reason`() {
            val order = createOrder()
            setStatus(order, OrderStatus.DISPUTED)

            val ex = assertThrows<FatumServiceException> {
                order.changeStatus(OrderStatus.SOLVED)
            }
            assertEquals(CatalogErrorCode.REASON_REQUIRED, ex.errorCode)
        }

        @Test
        fun `should allow optional reason for status that does not require it`() {
            val order = createOrder()
            order.changeStatus(OrderStatus.ACCEPTED, "Aceptada con gusto")

            assertAll(
                { assertEquals(OrderStatus.ACCEPTED, order.status) },
                { assertEquals("Aceptada con gusto", order.reason) }
            )
        }

        @Test
        fun `should set reason to null when not provided`() {
            val order = createOrder()
            order.changeStatus(OrderStatus.ACCEPTED)

            assertNull(order.reason)
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
        fun `should initialize all confirmations as not responded`() {
            val order = createOrder()

            assertAll(
                { assertFalse(order.startConfirmation.hasResponded(ConfirmingParty.CUSTOMER)) },
                { assertFalse(order.startConfirmation.hasResponded(ConfirmingParty.PROVIDER)) },
                { assertFalse(order.completionConfirmation.hasResponded(ConfirmingParty.CUSTOMER)) },
                { assertFalse(order.completionConfirmation.hasResponded(ConfirmingParty.PROVIDER)) },
                { assertFalse(order.paymentConfirmation.hasResponded(ConfirmingParty.CUSTOMER)) },
                { assertFalse(order.paymentConfirmation.hasResponded(ConfirmingParty.PROVIDER)) }
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

        @Test
        fun `should detect contradiction when one confirms and other denies`() {
            val order = createOrder()
            order.startConfirmation.confirm(ConfirmingParty.CUSTOMER)
            order.startConfirmation.deny(ConfirmingParty.PROVIDER)

            assertTrue(order.startConfirmation.isContradicted)
        }

        @Test
        fun `should detect both denied`() {
            val order = createOrder()
            order.completionConfirmation.deny(ConfirmingParty.CUSTOMER)
            order.completionConfirmation.deny(ConfirmingParty.PROVIDER)

            assertAll(
                { assertTrue(order.completionConfirmation.isBothDenied) },
                { assertFalse(order.completionConfirmation.isContradicted) },
                { assertFalse(order.completionConfirmation.isConfirmed) }
            )
        }

        @Test
        fun `should expose each confirmation by stage`() {
            val order = createOrder()
            order.respondConfirmation(ConfirmationStage.START, ConfirmingParty.CUSTOMER, confirmed = true)

            assertAll(
                { assertTrue(order.confirmationOf(ConfirmationStage.START).hasConfirmed(ConfirmingParty.CUSTOMER)) },
                { assertFalse(order.confirmationOf(ConfirmationStage.COMPLETION).hasResponded(ConfirmingParty.CUSTOMER)) },
                { assertFalse(order.confirmationOf(ConfirmationStage.PAYMENT).hasResponded(ConfirmingParty.CUSTOMER)) }
            )
        }

        @Test
        fun `should dispute when a confirmation contradicts a previous denial`() {
            val order = createOrder()
            setStatus(order, OrderStatus.DELIVERING)
            order.completionConfirmation.confirm(ConfirmingParty.CUSTOMER)
            order.completionConfirmation.confirm(ConfirmingParty.PROVIDER)

            val changedByDenial = order.respondConfirmation(
                ConfirmationStage.PAYMENT, ConfirmingParty.CUSTOMER, confirmed = false, reason = "No me pagaron"
            )
            val changedByConfirmation = order.respondConfirmation(
                ConfirmationStage.PAYMENT, ConfirmingParty.PROVIDER, confirmed = true
            )

            assertAll(
                { assertFalse(changedByDenial) },
                { assertTrue(changedByConfirmation) },
                { assertEquals(OrderStatus.DISPUTED, order.status) },
                { assertEquals("No me pagaron", order.reason) }
            )
        }

        @Test
        fun `should remember the denial reason while waiting for the other party`() {
            val order = createOrder()
            setStatus(order, OrderStatus.DELIVERING)

            order.respondConfirmation(
                ConfirmationStage.COMPLETION, ConfirmingParty.PROVIDER, confirmed = false, reason = "  No termine  "
            )

            assertAll(
                { assertEquals(OrderStatus.DELIVERING, order.status) },
                { assertEquals("No termine", order.reason) }
            )
        }

        @Test
        fun `should throw ALREADY_RESPONDED when a party answers twice`() {
            val order = createOrder()
            setStatus(order, OrderStatus.ACCEPTED)
            order.respondConfirmation(ConfirmationStage.START, ConfirmingParty.CUSTOMER, confirmed = false)

            val ex = assertThrows<FatumServiceException> {
                order.respondConfirmation(ConfirmationStage.START, ConfirmingParty.CUSTOMER, confirmed = true)
            }

            assertEquals(CatalogErrorCode.ALREADY_RESPONDED, ex.errorCode)
        }

        @Test
        fun `should not reuse the reason of an earlier stage for the dispute`() {
            val order = createOrder()
            order.changeStatus(OrderStatus.ACCEPTED, "Acepto con gusto")

            order.respondConfirmation(ConfirmationStage.START, ConfirmingParty.CUSTOMER, confirmed = false)
            order.respondConfirmation(ConfirmationStage.START, ConfirmingParty.PROVIDER, confirmed = true)

            assertAll(
                { assertEquals(OrderStatus.DISPUTED, order.status) },
                { assertEquals("Disputa automática: el cliente negó el inicio y el proveedor lo confirmó", order.reason) }
            )
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
