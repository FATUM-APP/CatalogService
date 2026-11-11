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
import fatum.catalog.repository.OrderRepository
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
        orderRepository.save(order.toEntity(item, client))
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


    }

    //==========================================CANCEL============================================================

    @Transactional
    fun cancel(orderId : String,reason: String) {
        val order = getOrder(orderId)
        order.changeStatus(OrderStatus.CANCELLED, reason)
        order.update()
        orderRepository.save(order)
    }

    @Transactional
    fun cancelProviderOrders(providerId : String,reason: String) {
        val pendingOrders = orderRepository.findByProviderIdAndStatus(providerId, OrderStatus.PENDING)
        pendingOrders.forEach { it.changeStatus(OrderStatus.REJECTED, reason) }
        orderRepository.saveAll(pendingOrders)

        val acceptedOrders = orderRepository.findByProviderIdAndStatus(providerId, OrderStatus.ACCEPTED)
        acceptedOrders.forEach {
            it.changeStatus(OrderStatus.CANCELLED, reason)
            it.update()
        }
        orderRepository.saveAll(acceptedOrders)
    }

    //==========================================CONFIRMATIONS=====================================================

    @Transactional
    fun confirmStart(orderId: String, party: ConfirmingParty) {
        val order = getOrder(orderId)
        if (order.status != OrderStatus.ACCEPTED)
            throw FatumServiceException(CatalogErrorCode.ORDER_NOT_CONFIRMED)
        validateNotResponded(order.startConfirmation, party)

        order.startConfirmation.confirm(party)

        if (order.startConfirmation.isConfirmed) {
            order.changeStatus(OrderStatus.DELIVERING)
            order.deliveryStartedAt = LocalDateTime.now()
        }
        order.update()
        orderRepository.save(order)
    }

    @Transactional
    fun denyStart(orderId: String, party: ConfirmingParty, reason: String) {
        val order = getOrder(orderId)
        if (order.status != OrderStatus.ACCEPTED)
            throw FatumServiceException(CatalogErrorCode.ORDER_NOT_CONFIRMED)
        validateNotResponded(order.startConfirmation, party)

        order.startConfirmation.deny(party)

        if (order.startConfirmation.isContradicted)
            order.changeStatus(OrderStatus.DISPUTED, reason)
        order.update()
        orderRepository.save(order)
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
    }

    //==========================================READ ONLY=========================================================

    @Transactional(readOnly = true)
    fun findOrdersByProvider(providerId: String, status: OrderStatus? = null): List<Order> =
        if (status != null) orderRepository.findByProviderIdAndStatus(providerId, status)
        else orderRepository.findByProviderId(providerId)

    @Transactional(readOnly = true)
    fun findOrdersByService(serviceId: String, status: OrderStatus? = null): List<Order> =
        if (status != null) orderRepository.findByCatalogItemIdAndStatus(serviceId, status)
        else orderRepository.findByCatalogItemId(serviceId)

    @Transactional(readOnly = true)
    fun findOrdersByClient(clientId: String, status: OrderStatus? = null): List<Order> =
        if (status != null) orderRepository.findByClientIdAndStatus(clientId, status)
        else orderRepository.findByClientId(clientId)

    @Transactional(readOnly = true)
    fun findActiveOrders(
        providerId: String,
        start: LocalDateTime,
        end: LocalDateTime
    ) : List<Order> = getActiveOrders(providerId, start, end)

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
        val providerOrders = orderRepository.findByProviderId(providerId)
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

        val accepted = orderRepository.findByProviderIdAndStatusAndBookingBetween(
            providerId, OrderStatus.ACCEPTED, start, end
        )
        val delivering = orderRepository.findByProviderIdAndStatusAndBookingBetween(
            providerId, OrderStatus.DELIVERING, start, end
        )
        return accepted + delivering
    }

}
