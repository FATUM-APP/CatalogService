package fatum.catalog.service

import fatum.catalog.client.UserServiceClient
import fatum.catalog.dto.request.order.BookService
import fatum.catalog.dto.request.order.OrderResponse
import fatum.catalog.dto.mapper.toEntity

import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Order
import fatum.catalog.model.constant.ConfirmingParty
import fatum.catalog.model.constant.OrderStatus
import fatum.catalog.model.constant.ServiceStatus
import fatum.catalog.model.vo.DualConfirmation
import fatum.catalog.model.vo.QuestionAnswer
import fatum.catalog.realtime.event.OrderChangeType
import fatum.catalog.realtime.event.OrderChangedEvent
import fatum.catalog.repository.OrderRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

@Service
class OrderService(
    private val userServiceClient: UserServiceClient,
    private val orderRepository: OrderRepository,
    private val providerAvailabilityService: ProviderAvailabilityService,
    private val providerUnavailableRangeService: ProviderUnavailableRangeService,
    private val catalogItemService: CatalogItemService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val gapTime: Int = 30

    //==========================================BOOKING===========================================================

    @Transactional
    fun book(order: BookService) {
        validateUser(order.clientId)
        val item = getService(order.serviceId)
        validateAnswers(item, order.answers)

        if (!providerIsFree(item.providerId, order.booking))
            throw FatumServiceException(CatalogErrorCode.PROVIDER_UNAVAILABLE)

        val client = userServiceClient.getUserById(order.clientId)
        val saved = orderRepository.save(order.toEntity(item, client))
        publish(saved, OrderChangeType.ORDER_CREATED)
    }

    //==========================================ACCEPT/REJECT=====================================================

    @Transactional
    fun acceptOrReject(response: OrderResponse) {
        if (response.status !in listOf(OrderStatus.ACCEPTED, OrderStatus.REJECTED))
            throw FatumServiceException(CatalogErrorCode.INVALID_RESPONSE)

        if (response.status == OrderStatus.ACCEPTED && response.estimatedTime == null)
            throw FatumServiceException(CatalogErrorCode.INVALID_RESPONSE)

        val order = getOrder(response.orderId)

        if (order.status != OrderStatus.PENDING)
            throw FatumServiceException(CatalogErrorCode.INVALID_STATUS_TRANSITION)

        if (response.providerId != order.providerId)
            throw FatumServiceException(CatalogErrorCode.FORBIDDEN)

        order.changeStatus(response.status, response.reason)
        order.estimatedTime = response.estimatedTime
        order.update()
        orderRepository.save(order)
        publish(order, OrderChangeType.ORDER_UPDATED)
    }

    //==========================================CANCEL============================================================

    @Transactional
    fun cancel(orderId : String,reason: String) {
        val order = getOrder(orderId)
        order.changeStatus(OrderStatus.CANCELLED, reason)
        order.update()
        orderRepository.save(order)
        publish(order, OrderChangeType.ORDER_CANCELLED)
    }

    @Transactional
    fun cancelProviderOrders(providerId : String,reason: String) {
        val pendingOrders = orderRepository.findByProviderIdAndStatusOrderByUpdatedAtDesc(providerId, OrderStatus.PENDING)
        pendingOrders.forEach {
            it.changeStatus(OrderStatus.REJECTED, reason)
            it.update()
        }
        orderRepository.saveAll(pendingOrders)
        pendingOrders.forEach { publish(it, OrderChangeType.ORDER_UPDATED) }

        val acceptedOrders = orderRepository.findByProviderIdAndStatusOrderByUpdatedAtDesc(providerId, OrderStatus.ACCEPTED)
        acceptedOrders.forEach {
            it.changeStatus(OrderStatus.CANCELLED, reason)
            it.update()
        }
        orderRepository.saveAll(acceptedOrders)
        acceptedOrders.forEach { publish(it, OrderChangeType.ORDER_CANCELLED) }
    }

    //==========================================CONFIRMATIONS=====================================================

    @Transactional
    fun confirmStart(orderId: String, party: ConfirmingParty) {
        val order = getOrder(orderId)
        if (order.status != OrderStatus.ACCEPTED)
            throw FatumServiceException(CatalogErrorCode.ORDER_NOT_CONFIRMED)
        validateNotResponded(order.startConfirmation, party)

        order.startConfirmation.confirm(party)
        val statusChanged = order.startConfirmation.isConfirmed

        if (statusChanged) {
            order.changeStatus(OrderStatus.DELIVERING)
            order.deliveryStartedAt = LocalDateTime.now()
        }
        order.update()
        orderRepository.save(order)
        publish(order, OrderChangeType.ORDER_UPDATED)
    }

    @Transactional
    fun denyStart(orderId: String, party: ConfirmingParty, reason: String) {
        val order = getOrder(orderId)
        if (order.status != OrderStatus.ACCEPTED)
            throw FatumServiceException(CatalogErrorCode.ORDER_NOT_CONFIRMED)
        validateNotResponded(order.startConfirmation, party)

        order.startConfirmation.deny(party)

        val statusChanged = order.startConfirmation.isContradicted

        if (statusChanged)
            order.changeStatus(OrderStatus.DISPUTED, reason)
        order.update()
        orderRepository.save(order)
        publish(order, OrderChangeType.ORDER_UPDATED)
    }

    @Transactional
    fun confirmCompletion(orderId: String, party: ConfirmingParty) {
        val order = getOrder(orderId)
        if (order.status != OrderStatus.DELIVERING)
            throw FatumServiceException(CatalogErrorCode.DELIVERING_NOT_CONFIRMED)
        validateNotResponded(order.completionConfirmation, party)

        order.completionConfirmation.confirm(party)
        order.update()
        orderRepository.save(order)
        publish(order, OrderChangeType.ORDER_UPDATED)
    }

    @Transactional
    fun denyCompletion(orderId: String, party: ConfirmingParty, reason: String) {
        val order = getOrder(orderId)
        if (order.status != OrderStatus.DELIVERING)
            throw FatumServiceException(CatalogErrorCode.DELIVERING_NOT_CONFIRMED)

        validateNotResponded(order.completionConfirmation, party)
        order.completionConfirmation.deny(party)

        if (order.completionConfirmation.isContradicted)
            order.changeStatus(OrderStatus.DISPUTED, reason)
        else if (order.completionConfirmation.isBothDenied)
            order.changeStatus(OrderStatus.NOT_DELIVERED, reason)
        order.update()
        orderRepository.save(order)
        publish(order, OrderChangeType.ORDER_UPDATED)
    }

    @Transactional
    fun confirmPayment(orderId: String, party: ConfirmingParty) {
        val order = getOrder(orderId)
        if (order.status != OrderStatus.DELIVERING)
            throw FatumServiceException(CatalogErrorCode.DELIVERING_NOT_CONFIRMED)
        if (!order.completionConfirmation.isConfirmed)
            throw FatumServiceException(CatalogErrorCode.COMPLETION_NOT_CONFIRMED)
        validateNotResponded(order.paymentConfirmation, party)

        order.paymentConfirmation.confirm(party)

        if (order.paymentConfirmation.isConfirmed)
            order.changeStatus(OrderStatus.COMPLETED)

        order.update()
        orderRepository.save(order)
        publish(order, OrderChangeType.ORDER_UPDATED)
    }

    @Transactional
    fun denyPayment(orderId: String, party: ConfirmingParty, reason: String) {
        val order = getOrder(orderId)
        if (order.status != OrderStatus.DELIVERING)
            throw FatumServiceException(CatalogErrorCode.DELIVERING_NOT_CONFIRMED)
        if (!order.completionConfirmation.isConfirmed)
            throw FatumServiceException(CatalogErrorCode.COMPLETION_NOT_CONFIRMED)
        validateNotResponded(order.paymentConfirmation, party)

        order.paymentConfirmation.deny(party)

        if (order.paymentConfirmation.isContradicted)
            order.changeStatus(OrderStatus.DISPUTED, reason)

        order.update()
        orderRepository.save(order)
        publish(order, OrderChangeType.ORDER_UPDATED)
    }

    //==========================================READ ONLY=========================================================

    @Transactional(readOnly = true)
    fun findOrdersByProvider(providerId: String, status: OrderStatus? = null): List<Order> =
        if (status != null) orderRepository.findByProviderIdAndStatusOrderByUpdatedAtDesc(providerId, status)
        else orderRepository.findByProviderIdOrderByUpdatedAtDesc(providerId)

    @Transactional(readOnly = true)
    fun findOrdersByService(serviceId: String, status: OrderStatus? = null): List<Order> =
        if (status != null) orderRepository.findByCatalogItemIdAndStatusOrderByUpdatedAtDesc(serviceId, status)
        else orderRepository.findByCatalogItemIdOrderByUpdatedAtDesc(serviceId)

    @Transactional(readOnly = true)
    fun findOrdersByClient(clientId: String, status: OrderStatus? = null): List<Order> =
        if (status != null) orderRepository.findByClientIdAndStatusOrderByUpdatedAtDesc(clientId, status)
        else orderRepository.findByClientIdOrderByUpdatedAtDesc(clientId)

    @Transactional(readOnly = true)
    fun findActiveOrders(
        providerId: String,
        start: LocalDateTime,
        end: LocalDateTime
    ) : List<Order> = getActiveOrders(providerId, start, end)

    @Transactional(readOnly = true)
    fun findOrderById(orderId: String) : Order = getOrder(orderId)

    //==========================================METRICS============================================================

    @Transactional(readOnly = true)
    fun countByService(serviceId: String): Long =
        orderRepository.countByCatalogItemId(serviceId)

    @Transactional(readOnly = true)
    fun countByServiceGroupedByStatus(serviceId: String): Map<OrderStatus, Int> =
        orderRepository.countByServiceGroupedByStatus(serviceId)
            .associate { row -> (row[0] as OrderStatus) to (row[1] as Long).toInt() }

    @Transactional(readOnly = true)
    fun countNewOrdersInPeriod(serviceId: String, start: LocalDateTime, end: LocalDateTime): Long =
        orderRepository.countNewOrdersInPeriod(serviceId, start, end)

    //==========================================HELPERS============================================================

    /**
     * Publica el hecho de negocio. El evento se entrega a [fatum.catalog.realtime.RealtimeEventListener]
     * solo cuando la transacción hace commit; si algo falla, no se notifica nada.
     */
    private fun publish(order: Order, changeType: OrderChangeType) {
        order.id?.let { eventPublisher.publishEvent(OrderChangedEvent(it, changeType)) }
    }

    private fun getService(serviceId: String) = catalogItemService.findById(serviceId, ServiceStatus.ACTIVE)

    private fun getOrder(orderId: String) = orderRepository.findById(orderId)
        .orElseThrow { FatumServiceException(CatalogErrorCode.ORDER_NOT_FOUND) }

    private fun validateUser(userId: String) {
        if (!userServiceClient.getUserById(userId).isValid)
            throw FatumServiceException(CatalogErrorCode.INVALID_USER)
    }

    private fun validateNotResponded(confirmation: DualConfirmation, party: ConfirmingParty) {
        if (confirmation.hasResponded(party))
            throw FatumServiceException(CatalogErrorCode.ALREADY_RESPONDED)
    }

    private fun validateAnswers(item: CatalogItem, answers: Set<QuestionAnswer>) {
        val requiredQuestions = item.fixedQuestions.map { it.question.lowercase() }.toSet()
        if (requiredQuestions.isEmpty()) return
        val providedQuestions = answers.map { it.question.lowercase() }.toSet()
        if (!providedQuestions.containsAll(requiredQuestions))
            throw FatumServiceException(CatalogErrorCode.MISSING_ANSWERS)
    }

    private fun providerIsFree(providerId: String, booking: LocalDateTime): Boolean {
        reviewOrderOverlap(providerId, booking)
        val inUnavailability = providerUnavailableRangeService.scheduledDateInRange(providerId, booking)
        if (inUnavailability || timeGapPolicy(booking, LocalDateTime.now())) return false
        return providerAvailabilityService.providerIsAvailable(
            providerId, booking.dayOfWeek, booking.toLocalTime()
        )
    }

    private fun reviewOrderOverlap(providerId: String, booking: LocalDateTime) {
        val providerOrders = orderRepository.findByProviderIdOrderByUpdatedAtDesc(providerId)
        val overlap = providerOrders.any { timeGapPolicy(it.booking, booking) }
        if (overlap) throw FatumServiceException(CatalogErrorCode.OVERLAPPING_RANGE)
    }

    private fun timeGapPolicy(time1: LocalDateTime, time2: LocalDateTime): Boolean {
        return Duration.between(time1, time2).abs().toMinutes() < gapTime
    }

    private fun getActiveOrders(
        providerId: String,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<Order> {

        val accepted = orderRepository.findByProviderIdAndStatusAndBookingBetweenOrderByUpdatedAtDesc(
            providerId, OrderStatus.ACCEPTED, start, end
        )
        val delivering = orderRepository.findByProviderIdAndStatusAndBookingBetweenOrderByUpdatedAtDesc(
            providerId, OrderStatus.DELIVERING, start, end
        )
        return accepted + delivering
    }

}
