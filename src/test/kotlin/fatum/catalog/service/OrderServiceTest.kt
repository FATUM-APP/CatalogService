package fatum.catalog.service

import fatum.catalog.client.AddressDTO
import fatum.catalog.client.M2User
import fatum.catalog.client.UserServiceClient
import fatum.catalog.dto.request.order.BookService
import fatum.catalog.dto.request.order.OrderResponse
import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Category
import fatum.catalog.model.Order
import fatum.catalog.model.constant.ConfirmingParty
import fatum.catalog.model.constant.JobType
import fatum.catalog.model.constant.OrderStatus
import fatum.catalog.model.constant.PriceType
import fatum.catalog.model.constant.QuestionType
import fatum.catalog.model.constant.SchedulingModel
import fatum.catalog.model.constant.ServiceStatus
import fatum.catalog.model.vo.FixedQuestion
import fatum.catalog.model.vo.QuestionAnswer
import fatum.catalog.repository.OrderRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.springframework.context.ApplicationEventPublisher
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class OrderServiceTest {

    @Mock lateinit var userServiceClient: UserServiceClient
    @Mock lateinit var orderRepository: OrderRepository
    @Mock lateinit var providerAvailabilityService: ProviderAvailabilityService
    @Mock lateinit var providerUnavailableRangeService: ProviderUnavailableRangeService
    @Mock lateinit var catalogItemService: CatalogItemService
    @Mock lateinit var eventPublisher: ApplicationEventPublisher
    @InjectMocks lateinit var service: OrderService

    private lateinit var category: Category
    private lateinit var catalogItem: CatalogItem

    private val validUser = M2User(
        userId = "client-456",
        name = "Juan Perez",
        username = "juanperez",
        birthDate = LocalDate.of(1990, 1, 1),
        isProfessional = false,
        isVerified = true,
        isActive = true,
        gender = "M",
        principalAddress = AddressDTO("Calle 123 #45-67", "Casa", "Bogota", "Cundinamarca", "Colombia")
    )

    private val invalidUser = validUser.copy(isActive = false)

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
        setCatalogItemId(catalogItem, "service-1")
    }

    private fun createOrder(
        clientId: String = "client-456",
        clientName: String = "Juan Perez",
        clientUsername: String = "juanperez",
        booking: LocalDateTime = LocalDateTime.of(2027, 6, 15, 10, 0),
        item: CatalogItem = catalogItem,
        negotiatedPrice: BigDecimal = item.basePrice
    ): Order {
        val order = Order(
            clientId = clientId,
            clientName = clientName,
            clientUsername = clientUsername,
            booking = booking,
            catalogItem = item,
            negotiatedPrice = negotiatedPrice,
            deliveryCity = "Bogota",
            deliveryState = "Cundinamarca",
            deliveryCountry = "Colombia",
            deliveryResidence = "Calle 123 #45-67"
        )
        setOrderId(order, "order-1")
        return order
    }

    private fun setOrderId(order: Order, id: String) {
        Order::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(order, id)
        }
    }

    private fun setOrderStatus(order: Order, status: OrderStatus) {
        Order::class.java.getDeclaredField("status").apply {
            isAccessible = true
            set(order, status)
        }
    }

    private fun setCatalogItemId(item: CatalogItem, id: String) {
        CatalogItem::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(item, id)
        }
    }

    //==========================================BOOKING===========================================================

    @Nested
    inner class Book {

        private val booking = LocalDateTime.of(2027, 6, 15, 10, 0)

        private fun stubProviderFree() {
            whenever(orderRepository.findByProviderIdOrderByUpdatedAtDesc("provider-123")).thenReturn(emptyList())
            whenever(providerUnavailableRangeService.scheduledDateInRange("provider-123", booking))
                .thenReturn(false)
            whenever(providerAvailabilityService.providerIsAvailable(
                "provider-123", booking.dayOfWeek, booking.toLocalTime()
            )).thenReturn(true)
        }

        @Test
        fun `should book order successfully`() {
            val request = BookService(
                clientId = "client-456",
                booking = booking,
                serviceId = "service-1",
                answers = emptySet()
            )
            whenever(userServiceClient.getUserById("client-456")).thenReturn(validUser)
            whenever(catalogItemService.findById("service-1", ServiceStatus.ACTIVE)).thenReturn(catalogItem)
            stubProviderFree()
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.book(request)

            val captor = argumentCaptor<Order>()
            verify(orderRepository).save(captor.capture())
            val saved = captor.firstValue

            assertAll(
                { assertEquals("client-456", saved.clientId) },
                { assertEquals("Juan Perez", saved.clientName) },
                { assertEquals("juanperez", saved.clientUsername) },
                { assertEquals("provider-123", saved.providerId) },
                { assertEquals(BigDecimal(100), saved.negotiatedPrice) },
                { assertEquals(OrderStatus.PENDING, saved.status) }
            )
        }

        @Test
        fun `should book order with custom negotiated price`() {
            val request = BookService(
                clientId = "client-456",
                booking = booking,
                serviceId = "service-1",
                negotiatedPrice = BigDecimal(200),
                answers = emptySet()
            )
            whenever(userServiceClient.getUserById("client-456")).thenReturn(validUser)
            whenever(catalogItemService.findById("service-1", ServiceStatus.ACTIVE)).thenReturn(catalogItem)
            stubProviderFree()
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.book(request)

            val captor = argumentCaptor<Order>()
            verify(orderRepository).save(captor.capture())
            assertEquals(BigDecimal(200), captor.firstValue.negotiatedPrice)
        }

        @Test
        fun `should book order with answers when questions are required`() {
            val itemWithQuestions = CatalogItem(
                providerId = "provider-123",
                title = "Servicio Con Preguntas",
                providerName = "Provider Test",
                providerUsername = "providertest",
                priceType = PriceType.FIXED,
                jobType = JobType.PRESENCIAL,
                category = category,
                basePrice = BigDecimal(100),
                fixedQuestions = setOf(
                    FixedQuestion("Tipo de trabajo", QuestionType.SINGLE_CHOICE, setOf("Opcion A", "Opcion B"))
                )
            )
            setCatalogItemId(itemWithQuestions, "service-2")

            val request = BookService(
                clientId = "client-456",
                booking = booking,
                serviceId = "service-2",
                answers = setOf(QuestionAnswer("Tipo de trabajo", setOf("Opcion A")))
            )
            whenever(userServiceClient.getUserById("client-456")).thenReturn(validUser)
            whenever(catalogItemService.findById("service-2", ServiceStatus.ACTIVE)).thenReturn(itemWithQuestions)
            whenever(orderRepository.findByProviderIdOrderByUpdatedAtDesc("provider-123")).thenReturn(emptyList())
            whenever(providerUnavailableRangeService.scheduledDateInRange("provider-123", booking))
                .thenReturn(false)
            whenever(providerAvailabilityService.providerIsAvailable(
                "provider-123", booking.dayOfWeek, booking.toLocalTime()
            )).thenReturn(true)
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.book(request)

            verify(orderRepository).save(any())
        }

        @Test
        fun `should throw INVALID_USER when user is not valid`() {
            val request = BookService(
                clientId = "client-456", booking = booking,
                serviceId = "service-1", answers = emptySet()
            )
            whenever(userServiceClient.getUserById("client-456")).thenReturn(invalidUser)

            val ex = assertThrows<FatumServiceException> { service.book(request) }

            assertEquals(CatalogErrorCode.INVALID_USER, ex.errorCode)
            verify(orderRepository, never()).save(any())
        }

        @Test
        fun `should throw MISSING_ANSWERS when required questions not answered`() {
            val itemWithQuestions = CatalogItem(
                providerId = "provider-123",
                title = "Servicio Con Preguntas",
                providerName = "Provider Test",
                providerUsername = "providertest",
                priceType = PriceType.FIXED,
                jobType = JobType.PRESENCIAL,
                category = category,
                basePrice = BigDecimal(100),
                fixedQuestions = setOf(
                    FixedQuestion("Tipo de trabajo", QuestionType.SINGLE_CHOICE, setOf("Opcion A", "Opcion B"))
                )
            )
            setCatalogItemId(itemWithQuestions, "service-2")

            val request = BookService(
                clientId = "client-456", booking = booking,
                serviceId = "service-2", answers = emptySet()
            )
            whenever(userServiceClient.getUserById("client-456")).thenReturn(validUser)
            whenever(catalogItemService.findById("service-2", ServiceStatus.ACTIVE)).thenReturn(itemWithQuestions)

            val ex = assertThrows<FatumServiceException> { service.book(request) }

            assertEquals(CatalogErrorCode.MISSING_ANSWERS, ex.errorCode)
        }

        @Test
        fun `should not require answers when service has no fixed questions`() {
            val request = BookService(
                clientId = "client-456", booking = booking,
                serviceId = "service-1", answers = emptySet()
            )
            whenever(userServiceClient.getUserById("client-456")).thenReturn(validUser)
            whenever(catalogItemService.findById("service-1", ServiceStatus.ACTIVE)).thenReturn(catalogItem)
            stubProviderFree()
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.book(request)

            verify(orderRepository).save(any())
        }

        @Test
        fun `should throw PROVIDER_UNAVAILABLE when in unavailability range`() {
            val request = BookService(
                clientId = "client-456", booking = booking,
                serviceId = "service-1", answers = emptySet()
            )
            whenever(userServiceClient.getUserById("client-456")).thenReturn(validUser)
            whenever(catalogItemService.findById("service-1", ServiceStatus.ACTIVE)).thenReturn(catalogItem)
            whenever(orderRepository.findByProviderIdOrderByUpdatedAtDesc("provider-123")).thenReturn(emptyList())
            whenever(providerUnavailableRangeService.scheduledDateInRange("provider-123", booking))
                .thenReturn(true)

            val ex = assertThrows<FatumServiceException> { service.book(request) }

            assertEquals(CatalogErrorCode.PROVIDER_UNAVAILABLE, ex.errorCode)
        }

        @Test
        fun `should throw PROVIDER_UNAVAILABLE when provider not available at time`() {
            val request = BookService(
                clientId = "client-456", booking = booking,
                serviceId = "service-1", answers = emptySet()
            )
            whenever(userServiceClient.getUserById("client-456")).thenReturn(validUser)
            whenever(catalogItemService.findById("service-1", ServiceStatus.ACTIVE)).thenReturn(catalogItem)
            whenever(orderRepository.findByProviderIdOrderByUpdatedAtDesc("provider-123")).thenReturn(emptyList())
            whenever(providerUnavailableRangeService.scheduledDateInRange("provider-123", booking))
                .thenReturn(false)
            whenever(providerAvailabilityService.providerIsAvailable(
                "provider-123", booking.dayOfWeek, booking.toLocalTime()
            )).thenReturn(false)

            val ex = assertThrows<FatumServiceException> { service.book(request) }

            assertEquals(CatalogErrorCode.PROVIDER_UNAVAILABLE, ex.errorCode)
        }

        @Test
        fun `should throw OVERLAPPING_RANGE when booking overlaps existing order`() {
            val existingOrder = createOrder(booking = booking.plusMinutes(10))
            val request = BookService(
                clientId = "client-456", booking = booking,
                serviceId = "service-1", answers = emptySet()
            )
            whenever(userServiceClient.getUserById("client-456")).thenReturn(validUser)
            whenever(catalogItemService.findById("service-1", ServiceStatus.ACTIVE)).thenReturn(catalogItem)
            whenever(orderRepository.findByProviderIdOrderByUpdatedAtDesc("provider-123")).thenReturn(listOf(existingOrder))

            val ex = assertThrows<FatumServiceException> { service.book(request) }

            assertEquals(CatalogErrorCode.OVERLAPPING_RANGE, ex.errorCode)
        }
    }

    //==========================================ACCEPT/REJECT=====================================================

    @Nested
    inner class AcceptOrReject {

        @Test
        fun `should accept order with estimated time`() {
            val order = createOrder()
            val response = OrderResponse(
                orderId = "order-1", providerId = "provider-123",
                status = OrderStatus.ACCEPTED, estimatedTime = 45
            )
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.acceptOrReject(response)

            assertAll(
                { assertEquals(OrderStatus.ACCEPTED, order.status) },
                { assertEquals(45, order.estimatedTime) }
            )
            verify(orderRepository).save(order)
        }

        @Test
        fun `should reject order`() {
            val order = createOrder()
            val response = OrderResponse(
                orderId = "order-1", providerId = "provider-123",
                status = OrderStatus.REJECTED
            )
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.acceptOrReject(response)

            assertEquals(OrderStatus.REJECTED, order.status)
        }

        @Test
        fun `should reject order with reason`() {
            val order = createOrder()
            val response = OrderResponse(
                orderId = "order-1", providerId = "provider-123",
                status = OrderStatus.REJECTED, reason = "No tengo disponibilidad"
            )
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.acceptOrReject(response)

            assertAll(
                { assertEquals(OrderStatus.REJECTED, order.status) },
                { assertEquals("No tengo disponibilidad", order.reason) }
            )
        }

        @Test
        fun `should throw INVALID_RESPONSE for status other than ACCEPTED or REJECTED`() {
            val response = OrderResponse(
                orderId = "order-1", providerId = "provider-123",
                status = OrderStatus.DELIVERING
            )

            val ex = assertThrows<FatumServiceException> { service.acceptOrReject(response) }

            assertEquals(CatalogErrorCode.INVALID_RESPONSE, ex.errorCode)
        }

        @Test
        fun `should throw INVALID_RESPONSE when accepting without estimated time`() {
            val response = OrderResponse(
                orderId = "order-1", providerId = "provider-123",
                status = OrderStatus.ACCEPTED
            )

            val ex = assertThrows<FatumServiceException> { service.acceptOrReject(response) }

            assertEquals(CatalogErrorCode.INVALID_RESPONSE, ex.errorCode)
        }

        @Test
        fun `should throw INVALID_STATUS_TRANSITION when order is not PENDING`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.ACCEPTED)

            val response = OrderResponse(
                orderId = "order-1", providerId = "provider-123",
                status = OrderStatus.ACCEPTED, estimatedTime = 30
            )
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))

            val ex = assertThrows<FatumServiceException> { service.acceptOrReject(response) }

            assertEquals(CatalogErrorCode.INVALID_STATUS_TRANSITION, ex.errorCode)
        }

        @Test
        fun `should throw FORBIDDEN when provider does not match`() {
            val order = createOrder()
            val response = OrderResponse(
                orderId = "order-1", providerId = "wrong-provider",
                status = OrderStatus.ACCEPTED, estimatedTime = 30
            )
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))

            val ex = assertThrows<FatumServiceException> { service.acceptOrReject(response) }

            assertEquals(CatalogErrorCode.FORBIDDEN, ex.errorCode)
        }

        @Test
        fun `should throw ORDER_NOT_FOUND when order does not exist`() {
            val response = OrderResponse(
                orderId = "order-999", providerId = "provider-123",
                status = OrderStatus.ACCEPTED, estimatedTime = 30
            )
            whenever(orderRepository.findById("order-999")).thenReturn(Optional.empty())

            val ex = assertThrows<FatumServiceException> { service.acceptOrReject(response) }

            assertEquals(CatalogErrorCode.ORDER_NOT_FOUND, ex.errorCode)
        }
    }

    //==========================================CANCEL============================================================

    @Nested
    inner class Cancel {

        @Test
        fun `should cancel PENDING order`() {
            val order = createOrder()
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.cancel("order-1", "Ya no necesito el servicio")

            assertAll(
                { assertEquals(OrderStatus.CANCELLED, order.status) },
                { assertEquals("Ya no necesito el servicio", order.reason) }
            )
            verify(orderRepository).save(order)
        }

        @Test
        fun `should cancel ACCEPTED order`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.ACCEPTED)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.cancel("order-1", "Cambio de planes")

            assertEquals(OrderStatus.CANCELLED, order.status)
        }

        @Test
        fun `should cancel DELIVERING order`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.DELIVERING)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.cancel("order-1", "Emergencia")

            assertEquals(OrderStatus.CANCELLED, order.status)
        }

        @Test
        fun `should throw INVALID_STATUS_TRANSITION when cancelling COMPLETED order`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.COMPLETED)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))

            val ex = assertThrows<FatumServiceException> {
                service.cancel("order-1", "Cancelar")
            }

            assertEquals(CatalogErrorCode.INVALID_STATUS_TRANSITION, ex.errorCode)
        }
    }

    //==========================================CANCEL PROVIDER ORDERS============================================

    @Nested
    inner class CancelProviderOrders {

        @Test
        fun `should reject pending and cancel accepted orders`() {
            val pending1 = createOrder()
            setOrderId(pending1, "p1")
            val pending2 = createOrder()
            setOrderId(pending2, "p2")
            val accepted1 = createOrder()
            setOrderId(accepted1, "a1")
            setOrderStatus(accepted1, OrderStatus.ACCEPTED)

            whenever(orderRepository.findByProviderIdAndStatusOrderByUpdatedAtDesc("provider-123", OrderStatus.PENDING))
                .thenReturn(listOf(pending1, pending2))
            whenever(orderRepository.findByProviderIdAndStatusOrderByUpdatedAtDesc("provider-123", OrderStatus.ACCEPTED))
                .thenReturn(listOf(accepted1))

            service.cancelProviderOrders("provider-123", "Proveedor no disponible")

            assertAll(
                { assertEquals(OrderStatus.REJECTED, pending1.status) },
                { assertEquals(OrderStatus.REJECTED, pending2.status) },
                { assertEquals(OrderStatus.CANCELLED, accepted1.status) },
                { assertEquals("Proveedor no disponible", pending1.reason) },
                { assertEquals("Proveedor no disponible", accepted1.reason) }
            )
            verify(orderRepository).saveAll(listOf(pending1, pending2))
            verify(orderRepository).saveAll(listOf(accepted1))
        }

        @Test
        fun `should handle empty lists`() {
            whenever(orderRepository.findByProviderIdAndStatusOrderByUpdatedAtDesc("provider-123", OrderStatus.PENDING))
                .thenReturn(emptyList())
            whenever(orderRepository.findByProviderIdAndStatusOrderByUpdatedAtDesc("provider-123", OrderStatus.ACCEPTED))
                .thenReturn(emptyList())

            service.cancelProviderOrders("provider-123", "Motivo")

            verify(orderRepository, org.mockito.Mockito.times(2)).saveAll(emptyList<Order>())
        }
    }

    //==========================================CONFIRM START=====================================================

    @Nested
    inner class ConfirmStart {

        @Test
        fun `should confirm start by one party without transitioning`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.ACCEPTED)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.confirmStart("order-1", ConfirmingParty.CUSTOMER)

            assertAll(
                { assertEquals(OrderStatus.ACCEPTED, order.status) },
                { assertTrue(order.startConfirmation.hasConfirmed(ConfirmingParty.CUSTOMER)) },
                { assertFalse(order.startConfirmation.hasResponded(ConfirmingParty.PROVIDER)) },
                { assertNull(order.deliveryStartedAt) }
            )
            verify(orderRepository).save(order)
        }

        @Test
        fun `should transition to DELIVERING when both parties confirm`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.ACCEPTED)
            order.startConfirmation.confirm(ConfirmingParty.CUSTOMER)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.confirmStart("order-1", ConfirmingParty.PROVIDER)

            assertAll(
                { assertEquals(OrderStatus.DELIVERING, order.status) },
                { assertNotNull(order.deliveryStartedAt) },
                { assertTrue(order.startConfirmation.isConfirmed) }
            )
        }

        @Test
        fun `should throw ORDER_NOT_CONFIRMED when order is not ACCEPTED`() {
            val order = createOrder()
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))

            val ex = assertThrows<FatumServiceException> {
                service.confirmStart("order-1", ConfirmingParty.CUSTOMER)
            }

            assertEquals(CatalogErrorCode.ORDER_NOT_CONFIRMED, ex.errorCode)
        }

        @Test
        fun `should throw ALREADY_RESPONDED when party already responded`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.ACCEPTED)
            order.startConfirmation.confirm(ConfirmingParty.CUSTOMER)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))

            val ex = assertThrows<FatumServiceException> {
                service.confirmStart("order-1", ConfirmingParty.CUSTOMER)
            }

            assertEquals(CatalogErrorCode.ALREADY_RESPONDED, ex.errorCode)
        }
    }

    //==========================================DENY START========================================================

    @Nested
    inner class DenyStart {

        @Test
        fun `should deny start without contradiction`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.ACCEPTED)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.denyStart("order-1", ConfirmingParty.CUSTOMER, "No estoy listo")

            assertAll(
                { assertEquals(OrderStatus.ACCEPTED, order.status) },
                { assertTrue(order.startConfirmation.hasResponded(ConfirmingParty.CUSTOMER)) },
                { assertFalse(order.startConfirmation.hasConfirmed(ConfirmingParty.CUSTOMER)) }
            )
            verify(orderRepository).save(order)
        }

        @Test
        fun `should transition to DISPUTED on contradiction`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.ACCEPTED)
            order.startConfirmation.confirm(ConfirmingParty.PROVIDER)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.denyStart("order-1", ConfirmingParty.CUSTOMER, "El proveedor no llego")

            assertAll(
                { assertEquals(OrderStatus.DISPUTED, order.status) },
                { assertEquals("El proveedor no llego", order.reason) },
                { assertTrue(order.startConfirmation.isContradicted) }
            )
        }

        @Test
        fun `should stay ACCEPTED when both deny`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.ACCEPTED)
            order.startConfirmation.deny(ConfirmingParty.CUSTOMER)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.denyStart("order-1", ConfirmingParty.PROVIDER, "No puedo ir")

            assertAll(
                { assertEquals(OrderStatus.ACCEPTED, order.status) },
                { assertTrue(order.startConfirmation.isBothDenied) }
            )
        }

        @Test
        fun `should throw ORDER_NOT_CONFIRMED when not ACCEPTED`() {
            val order = createOrder()
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))

            val ex = assertThrows<FatumServiceException> {
                service.denyStart("order-1", ConfirmingParty.CUSTOMER, "reason")
            }

            assertEquals(CatalogErrorCode.ORDER_NOT_CONFIRMED, ex.errorCode)
        }

        @Test
        fun `should throw ALREADY_RESPONDED when party already responded`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.ACCEPTED)
            order.startConfirmation.deny(ConfirmingParty.CUSTOMER)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))

            val ex = assertThrows<FatumServiceException> {
                service.denyStart("order-1", ConfirmingParty.CUSTOMER, "reason")
            }

            assertEquals(CatalogErrorCode.ALREADY_RESPONDED, ex.errorCode)
        }
    }

    //==========================================CONFIRM COMPLETION================================================

    @Nested
    inner class ConfirmCompletion {

        @Test
        fun `should confirm completion by one party`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.DELIVERING)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.confirmCompletion("order-1", ConfirmingParty.PROVIDER)

            assertAll(
                { assertEquals(OrderStatus.DELIVERING, order.status) },
                { assertTrue(order.completionConfirmation.hasConfirmed(ConfirmingParty.PROVIDER)) },
                { assertFalse(order.completionConfirmation.isConfirmed) }
            )
        }

        @Test
        fun `should confirm completion by both parties without status change`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.DELIVERING)
            order.completionConfirmation.confirm(ConfirmingParty.CUSTOMER)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.confirmCompletion("order-1", ConfirmingParty.PROVIDER)

            assertAll(
                { assertEquals(OrderStatus.DELIVERING, order.status) },
                { assertTrue(order.completionConfirmation.isConfirmed) }
            )
        }

        @Test
        fun `should throw DELIVERING_NOT_CONFIRMED when not DELIVERING`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.ACCEPTED)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))

            val ex = assertThrows<FatumServiceException> {
                service.confirmCompletion("order-1", ConfirmingParty.CUSTOMER)
            }

            assertEquals(CatalogErrorCode.DELIVERING_NOT_CONFIRMED, ex.errorCode)
        }

        @Test
        fun `should throw ALREADY_RESPONDED when party already responded`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.DELIVERING)
            order.completionConfirmation.confirm(ConfirmingParty.CUSTOMER)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))

            val ex = assertThrows<FatumServiceException> {
                service.confirmCompletion("order-1", ConfirmingParty.CUSTOMER)
            }

            assertEquals(CatalogErrorCode.ALREADY_RESPONDED, ex.errorCode)
        }
    }

    //==========================================DENY COMPLETION===================================================

    @Nested
    inner class DenyCompletion {

        @Test
        fun `should deny without state change when only one party denies`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.DELIVERING)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.denyCompletion("order-1", ConfirmingParty.CUSTOMER, "No se completo")

            assertEquals(OrderStatus.DELIVERING, order.status)
        }

        @Test
        fun `should transition to DISPUTED on contradiction`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.DELIVERING)
            order.completionConfirmation.confirm(ConfirmingParty.PROVIDER)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.denyCompletion("order-1", ConfirmingParty.CUSTOMER, "No se hizo el trabajo")

            assertAll(
                { assertEquals(OrderStatus.DISPUTED, order.status) },
                { assertEquals("No se hizo el trabajo", order.reason) }
            )
        }

        @Test
        fun `should transition to NOT_DELIVERED when both deny`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.DELIVERING)
            order.completionConfirmation.deny(ConfirmingParty.CUSTOMER)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.denyCompletion("order-1", ConfirmingParty.PROVIDER, "No se pudo completar")

            assertAll(
                { assertEquals(OrderStatus.NOT_DELIVERED, order.status) },
                { assertEquals("No se pudo completar", order.reason) }
            )
        }

        @Test
        fun `should throw DELIVERING_NOT_CONFIRMED when not DELIVERING`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.ACCEPTED)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))

            val ex = assertThrows<FatumServiceException> {
                service.denyCompletion("order-1", ConfirmingParty.CUSTOMER, "reason")
            }

            assertEquals(CatalogErrorCode.DELIVERING_NOT_CONFIRMED, ex.errorCode)
        }
    }

    //==========================================CONFIRM PAYMENT===================================================

    @Nested
    inner class ConfirmPayment {

        @Test
        fun `should confirm payment by one party without transitioning`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.DELIVERING)
            order.completionConfirmation.confirm(ConfirmingParty.CUSTOMER)
            order.completionConfirmation.confirm(ConfirmingParty.PROVIDER)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.confirmPayment("order-1", ConfirmingParty.CUSTOMER)

            assertAll(
                { assertEquals(OrderStatus.DELIVERING, order.status) },
                { assertTrue(order.paymentConfirmation.hasConfirmed(ConfirmingParty.CUSTOMER)) },
                { assertFalse(order.paymentConfirmation.isConfirmed) }
            )
        }

        @Test
        fun `should transition to COMPLETED when both confirm payment`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.DELIVERING)
            order.completionConfirmation.confirm(ConfirmingParty.CUSTOMER)
            order.completionConfirmation.confirm(ConfirmingParty.PROVIDER)
            order.paymentConfirmation.confirm(ConfirmingParty.CUSTOMER)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.confirmPayment("order-1", ConfirmingParty.PROVIDER)

            assertAll(
                { assertEquals(OrderStatus.COMPLETED, order.status) },
                { assertTrue(order.paymentConfirmation.isConfirmed) }
            )
        }

        @Test
        fun `should throw DELIVERING_NOT_CONFIRMED when not DELIVERING`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.ACCEPTED)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))

            val ex = assertThrows<FatumServiceException> {
                service.confirmPayment("order-1", ConfirmingParty.CUSTOMER)
            }

            assertEquals(CatalogErrorCode.DELIVERING_NOT_CONFIRMED, ex.errorCode)
        }

        @Test
        fun `should throw COMPLETION_NOT_CONFIRMED when completion not confirmed`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.DELIVERING)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))

            val ex = assertThrows<FatumServiceException> {
                service.confirmPayment("order-1", ConfirmingParty.CUSTOMER)
            }

            assertEquals(CatalogErrorCode.COMPLETION_NOT_CONFIRMED, ex.errorCode)
        }

        @Test
        fun `should throw ALREADY_RESPONDED when party already responded`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.DELIVERING)
            order.completionConfirmation.confirm(ConfirmingParty.CUSTOMER)
            order.completionConfirmation.confirm(ConfirmingParty.PROVIDER)
            order.paymentConfirmation.confirm(ConfirmingParty.CUSTOMER)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))

            val ex = assertThrows<FatumServiceException> {
                service.confirmPayment("order-1", ConfirmingParty.CUSTOMER)
            }

            assertEquals(CatalogErrorCode.ALREADY_RESPONDED, ex.errorCode)
        }
    }

    //==========================================DENY PAYMENT======================================================

    @Nested
    inner class DenyPayment {

        @Test
        fun `should deny payment without state change when only one denies`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.DELIVERING)
            order.completionConfirmation.confirm(ConfirmingParty.CUSTOMER)
            order.completionConfirmation.confirm(ConfirmingParty.PROVIDER)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.denyPayment("order-1", ConfirmingParty.CUSTOMER, "No se realizo el pago")

            assertEquals(OrderStatus.DELIVERING, order.status)
            verify(orderRepository).save(order)
        }

        @Test
        fun `should transition to DISPUTED on payment contradiction`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.DELIVERING)
            order.completionConfirmation.confirm(ConfirmingParty.CUSTOMER)
            order.completionConfirmation.confirm(ConfirmingParty.PROVIDER)
            order.paymentConfirmation.confirm(ConfirmingParty.PROVIDER)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))
            whenever(orderRepository.save(any<Order>())).thenAnswer { it.arguments[0] }

            service.denyPayment("order-1", ConfirmingParty.CUSTOMER, "No me pagaron")

            assertAll(
                { assertEquals(OrderStatus.DISPUTED, order.status) },
                { assertEquals("No me pagaron", order.reason) },
                { assertTrue(order.paymentConfirmation.isContradicted) }
            )
        }

        @Test
        fun `should throw DELIVERING_NOT_CONFIRMED when not DELIVERING`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.COMPLETED)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))

            val ex = assertThrows<FatumServiceException> {
                service.denyPayment("order-1", ConfirmingParty.CUSTOMER, "reason")
            }

            assertEquals(CatalogErrorCode.DELIVERING_NOT_CONFIRMED, ex.errorCode)
        }

        @Test
        fun `should throw COMPLETION_NOT_CONFIRMED when completion not confirmed`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.DELIVERING)
            whenever(orderRepository.findById("order-1")).thenReturn(Optional.of(order))

            val ex = assertThrows<FatumServiceException> {
                service.denyPayment("order-1", ConfirmingParty.CUSTOMER, "reason")
            }

            assertEquals(CatalogErrorCode.COMPLETION_NOT_CONFIRMED, ex.errorCode)
        }
    }

    //==========================================READ ONLY=========================================================

    @Nested
    inner class FindOrdersByProvider {

        @Test
        fun `should return all orders for provider`() {
            val orders = listOf(createOrder(), createOrder())
            whenever(orderRepository.findByProviderIdOrderByUpdatedAtDesc("provider-123")).thenReturn(orders)

            val result = service.findOrdersByProvider("provider-123")

            assertEquals(2, result.size)
        }

        @Test
        fun `should return filtered orders by status`() {
            val order = createOrder()
            setOrderStatus(order, OrderStatus.ACCEPTED)
            whenever(orderRepository.findByProviderIdAndStatusOrderByUpdatedAtDesc("provider-123", OrderStatus.ACCEPTED))
                .thenReturn(listOf(order))

            val result = service.findOrdersByProvider("provider-123", OrderStatus.ACCEPTED)

            assertAll(
                { assertEquals(1, result.size) },
                { assertEquals(OrderStatus.ACCEPTED, result[0].status) }
            )
        }

        @Test
        fun `should return empty list when no orders`() {
            whenever(orderRepository.findByProviderIdOrderByUpdatedAtDesc("provider-999")).thenReturn(emptyList())

            val result = service.findOrdersByProvider("provider-999")

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class FindOrdersByService {

        @Test
        fun `should return all orders for service`() {
            val orders = listOf(createOrder())
            whenever(orderRepository.findByCatalogItemIdOrderByUpdatedAtDesc("service-1")).thenReturn(orders)

            val result = service.findOrdersByService("service-1")

            assertEquals(1, result.size)
        }

        @Test
        fun `should return filtered orders by status`() {
            whenever(orderRepository.findByCatalogItemIdAndStatusOrderByUpdatedAtDesc("service-1", OrderStatus.PENDING))
                .thenReturn(listOf(createOrder()))

            val result = service.findOrdersByService("service-1", OrderStatus.PENDING)

            assertEquals(1, result.size)
        }

        @Test
        fun `should return empty list when no orders for service`() {
            whenever(orderRepository.findByCatalogItemIdOrderByUpdatedAtDesc("service-999")).thenReturn(emptyList())

            val result = service.findOrdersByService("service-999")

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class FindOrdersByClient {

        @Test
        fun `should return all orders for client`() {
            val orders = listOf(createOrder(), createOrder())
            whenever(orderRepository.findByClientIdOrderByUpdatedAtDesc("client-456")).thenReturn(orders)

            val result = service.findOrdersByClient("client-456")

            assertEquals(2, result.size)
        }

        @Test
        fun `should return filtered orders by status`() {
            whenever(orderRepository.findByClientIdAndStatusOrderByUpdatedAtDesc("client-456", OrderStatus.COMPLETED))
                .thenReturn(emptyList())

            val result = service.findOrdersByClient("client-456", OrderStatus.COMPLETED)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `should return empty list when client has no orders`() {
            whenever(orderRepository.findByClientIdOrderByUpdatedAtDesc("client-999")).thenReturn(emptyList())

            val result = service.findOrdersByClient("client-999")

            assertTrue(result.isEmpty())
        }
    }
}
