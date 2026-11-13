package fatum.catalog.service

import fatum.catalog.client.UserServiceClient
import fatum.catalog.dto.request.order.BookService
import fatum.catalog.dto.request.order.OrderResponse
import fatum.catalog.dto.mapper.toEntity
import fatum.catalog.dto.response.order.AvailabilityResponse

import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.event.order.OrderEventPublisher
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Order
import fatum.catalog.model.constant.AvailabilityReason
import fatum.catalog.model.constant.ConfirmationStage
import fatum.catalog.model.constant.ConfirmingParty
import fatum.catalog.model.constant.OrderStatus
import fatum.catalog.model.constant.ServiceStatus
import fatum.catalog.model.constant.extension.toErrorCode
import fatum.catalog.model.vo.QuestionAnswer
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
    private val orderEventPublisher: OrderEventPublisher,
) {
    private val gapTime: Int = 30

    //==========================================BOOKING===========================================================

    @Transactional
    fun book(order: BookService) {
        validateUser(order.clientId)
        val item = getService(order.serviceId)
        validateAnswers(item, order.answers)
        if (order.clientId == item.providerId)
            throw FatumServiceException(CatalogErrorCode.PROVIDER_AND_CLIENT)

        val unavailable = unavailableReason(item.providerId, order.booking, item.defaultEstimatedTime ?: gapTime)
        if (unavailable != null)
            throw FatumServiceException(unavailable.toErrorCode())


        val client = userServiceClient.getUserById(order.clientId)
        val saved = orderRepository.save(order.toEntity(item, client))
        orderEventPublisher.orderCreated(
            orderId = saved.id!!,
            providerId = saved.providerId,
            catalogItemTitle = saved.catalogItem.title
        )
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

        order.estimatedTime = response.estimatedTime
        if (response.status == OrderStatus.ACCEPTED)
            ensureNoOverlap(order)
        order.changeStatus(response.status, response.reason)
        order.update()
        orderRepository.save(order)

        orderEventPublisher.statusChanged(
            orderId = order.id!!,
            clientId = order.clientId,
            providerId = order.providerId,
            newStatus = order.status,
            reason = response.reason
        )
    }


    //==========================================CANCEL============================================================

    @Transactional
    fun cancel(orderId : String, reason: String?) {
        val order = getOrder(orderId)
        order.changeStatus(OrderStatus.CANCELLED, reason)
        order.update()
        orderRepository.save(order)

        orderEventPublisher.statusChanged(
            orderId = order.id!!,
            clientId = order.clientId,
            providerId = order.providerId,
            newStatus = order.status,
            reason = reason
        )

    }

    @Transactional
    fun cancelProviderOrders(providerId : String, reason: String?) {
        val pendingOrders = orderRepository.findByProviderIdAndStatusOrderByUpdatedAtDesc(providerId, OrderStatus.PENDING)
        pendingOrders.forEach {
            it.changeStatus(OrderStatus.REJECTED, reason)
            it.update()
        }
        orderRepository.saveAll(pendingOrders)

        val acceptedOrders = orderRepository.findByProviderIdAndStatusOrderByUpdatedAtDesc(providerId, OrderStatus.ACCEPTED)
        acceptedOrders.forEach {
            it.changeStatus(OrderStatus.CANCELLED, reason)
            it.update()
        }
        orderRepository.saveAll(acceptedOrders)

        (pendingOrders + acceptedOrders).forEach {
            orderEventPublisher.statusChanged(
                orderId = it.id!!,
                clientId = it.clientId,
                providerId = it.providerId,
                newStatus = it.status,
                reason = reason
            )
        }


    }

    //==========================================CONFIRMATIONS=====================================================

    @Transactional
    fun confirmStart(orderId: String, party: ConfirmingParty, reason: String? = null) {
        val order = getOrder(orderId)
        ensureStartStage(order)
        respond(order, ConfirmationStage.START, party, confirmed = true, reason = reason)
    }

    @Transactional
    fun denyStart(orderId: String, party: ConfirmingParty, reason: String?) {
        val order = getOrder(orderId)
        ensureStartStage(order)
        respond(order, ConfirmationStage.START, party, confirmed = false, reason = reason)
    }

    @Transactional
    fun confirmCompletion(orderId: String, party: ConfirmingParty, reason: String? = null) {
        val order = getOrder(orderId)
        ensureCompletionStage(order)
        respond(order, ConfirmationStage.COMPLETION, party, confirmed = true, reason = reason)
    }

    @Transactional
    fun denyCompletion(orderId: String, party: ConfirmingParty, reason: String?) {
        val order = getOrder(orderId)
        ensureCompletionStage(order)
        respond(order, ConfirmationStage.COMPLETION, party, confirmed = false, reason = reason)
    }

    @Transactional
    fun confirmPayment(orderId: String, party: ConfirmingParty, reason: String? = null) {
        val order = getOrder(orderId)
        ensurePaymentStage(order)
        respond(order, ConfirmationStage.PAYMENT, party, confirmed = true, reason = reason)
    }

    @Transactional
    fun denyPayment(orderId: String, party: ConfirmingParty, reason: String?) {
        val order = getOrder(orderId)
        ensurePaymentStage(order)
        respond(order, ConfirmationStage.PAYMENT, party, confirmed = false, reason = reason)
    }

    /**
     * Único camino de una respuesta a un paso de confirmación. El estado lo decide el modelo a
     * partir de las dos respuestas; aquí solo quedan los eventos: siempre el de la confirmación y,
     * si de verdad cambió el estado, también `statusChanged` (también cuando la disputa nace de un
     * "confirmo" que contradice una negación previa).
     */
    private fun respond(
        order: Order,
        stage: ConfirmationStage,
        party: ConfirmingParty,
        confirmed: Boolean,
        reason: String?
    ) {
        val previousStatus = order.status
        order.respondConfirmation(stage, party, confirmed, reason)
        order.update()
        orderRepository.save(order)

        orderEventPublisher.confirmationChanged(
            orderId = order.id!!,
            clientId = order.clientId,
            providerId = order.providerId,
            confirmation = stage.name,
            party = party.name,
            confirmed = confirmed,
            bothConfirmed = order.confirmationOf(stage).isConfirmed
        )

        if (order.status != previousStatus)
            orderEventPublisher.statusChanged(
                orderId = order.id!!,
                clientId = order.clientId,
                providerId = order.providerId,
                newStatus = order.status,
                reason = order.reason
            )
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

    private fun getService(serviceId: String) = catalogItemService.findById(serviceId, ServiceStatus.ACTIVE)

    private fun getOrder(orderId: String) = orderRepository.findById(orderId)
        .orElseThrow { FatumServiceException(CatalogErrorCode.ORDER_NOT_FOUND) }

    private fun validateUser(userId: String) {
        if (!userServiceClient.getUserById(userId).isValid)
            throw FatumServiceException(CatalogErrorCode.INVALID_USER)
    }

    /** Precondiciones del paso: el estado tiene que permitir esa confirmación. */
    private fun ensureStartStage(order: Order) {
        if (order.status != OrderStatus.ACCEPTED)
            throw FatumServiceException(CatalogErrorCode.ORDER_NOT_CONFIRMED)
    }

    private fun ensureCompletionStage(order: Order) {
        if (order.status != OrderStatus.DELIVERING)
            throw FatumServiceException(CatalogErrorCode.DELIVERING_NOT_CONFIRMED)
    }

    private fun ensurePaymentStage(order: Order) {
        ensureCompletionStage(order)
        if (!order.completionConfirmation.isConfirmed)
            throw FatumServiceException(CatalogErrorCode.COMPLETION_NOT_CONFIRMED)
    }

    private fun validateAnswers(item: CatalogItem, answers: Set<QuestionAnswer>) {
        val requiredQuestions = item.fixedQuestions.map { it.question.lowercase() }.toSet()
        if (requiredQuestions.isEmpty()) return
        val providedQuestions = answers.map { it.question.lowercase() }.toSet()
        if (!providedQuestions.containsAll(requiredQuestions))
            throw FatumServiceException(CatalogErrorCode.MISSING_ANSWERS)
    }


    @Transactional(readOnly = true)
    fun checkAvailability(
        providerId: String,
        booking: LocalDateTime,
        durationMinutes: Int? = null
    ): AvailabilityResponse {
        val duration = durationMinutes?.takeIf { it > 0 } ?: gapTime
        val reason = unavailableReason(providerId, booking, duration)
        return AvailabilityResponse(
            providerId = providerId,
            booking = booking,
            durationMinutes = duration,
            endsAt = booking.plusMinutes(duration.toLong()),
            available = reason == null,
            reason = reason
        )
    }


    private fun unavailableReason(
        providerId: String,
        booking: LocalDateTime,
        durationMinutes: Int
    ): AvailabilityReason? {
        if (booking.isBefore(LocalDateTime.now().plusMinutes(gapTime.toLong())))
            return AvailabilityReason.LEAD_TIME
        if (providerUnavailableRangeService.scheduledDateInRange(providerId, booking))
            return AvailabilityReason.UNAVAILABLE_RANGE
        if (hasOverlap(providerId, booking, durationMinutes))
            return AvailabilityReason.OVERLAP
        if (!fitsSchedule(providerId, booking, durationMinutes))
            return AvailabilityReason.OUT_OF_SCHEDULE
        return null
    }

    private fun fitsSchedule(providerId: String, booking: LocalDateTime, durationMinutes: Int): Boolean {
        val end = booking.plusMinutes(durationMinutes.toLong())
        // Un turno que cruza la medianoche no entra en el horario de ningún día.
        if (end.toLocalDate() != booking.toLocalDate()) return false
        return providerAvailabilityService.providerIsAvailable(
            providerId, booking.dayOfWeek, booking.toLocalTime(), end.toLocalTime()
        )
    }


    private fun hasOverlap(
        providerId: String,
        booking: LocalDateTime,
        durationMinutes: Int,
        excludeOrderId: String? = null
    ): Boolean {
        val requestedEnd = booking.plusMinutes(durationMinutes.toLong())
        return orderRepository.findByProviderIdOrderByUpdatedAtDesc(providerId).any { existing ->
            existing.id != excludeOrderId &&
                    existing.status in BLOCKING_STATUSES &&
                    existing.booking.isBefore(requestedEnd) &&
                    booking.isBefore(existing.endsAt)
        }
    }


    private fun ensureNoOverlap(order: Order) {
        val duration = order.estimatedTime ?: gapTime
        if (hasOverlap(order.providerId, order.booking, duration, excludeOrderId = order.id))
            throw FatumServiceException(CatalogErrorCode.OVERLAPPING_RANGE)
    }

    private fun getActiveOrders(
        providerId: String,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<Order> = VISIBLE_STATUSES
        .flatMap { status ->
            orderRepository.findByProviderIdAndStatusAndBookingBetweenOrderByUpdatedAtDesc(
                providerId, status, start, end
            )
        }
        .sortedByDescending { it.updatedAt }

    companion object {

        private val BLOCKING_STATUSES = setOf(OrderStatus.ACCEPTED, OrderStatus.DELIVERING)

        private val VISIBLE_STATUSES = setOf(OrderStatus.PENDING, OrderStatus.ACCEPTED, OrderStatus.DELIVERING)
    }
}



