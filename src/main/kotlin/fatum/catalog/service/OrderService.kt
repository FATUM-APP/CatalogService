package fatum.catalog.service

import fatum.catalog.client.UserServiceClient
import fatum.catalog.dto.BookService
import fatum.catalog.dto.OrderResponse
import fatum.catalog.dto.mapper.toEntity
import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.Order
import fatum.catalog.model.constant.ConfirmingParty
import fatum.catalog.model.constant.OrderStatus
import fatum.catalog.model.constant.ServiceStatus
import fatum.catalog.model.vo.DualConfirmation
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
    private val catalogItemService: CatalogItemService
) {
    private val gapTime: Int = 30

    @Transactional
    fun book(order: BookService){
        validateUser(order.clientId)

        val item = getService(order.serviceId)

        if(!providerIsFree(item.providerId, order.booking))
            throw FatumServiceException(CatalogErrorCode.PROVIDER_UNAVAILABLE)

        val client = userServiceClient.getUserById(order.clientId)

        orderRepository.save(order.toEntity(item,client))
    }

    @Transactional
    fun acceptOrReject(response: OrderResponse) {
        if (response.status !in listOf(OrderStatus.ACCEPTED, OrderStatus.REJECTED))
            throw FatumServiceException(CatalogErrorCode.INVALID_RESPONSE)

        if (response.status == OrderStatus.ACCEPTED && response.estimatedTime == null)
            throw FatumServiceException(CatalogErrorCode.INVALID_RESPONSE)



        val order = getOrder(response.orderId)
        if (response.providerId != order.providerId)
            throw FatumServiceException(CatalogErrorCode.FORBIDDEN)


        order.apply {
            status = response.status
            estimatedTime = response.estimatedTime
            reason = if (response.status == OrderStatus.REJECTED) response.reason else null
        }

        orderRepository.save(order)
    }



    //==========================================DUAL===========================================================

    @Transactional
    fun confirmStart(orderId: String, party: ConfirmingParty){
        if(getOrder(orderId).status != OrderStatus.ACCEPTED)
            throw FatumServiceException(CatalogErrorCode.ORDER_NOT_CONFIRMED)
        processConfirmation(orderId, party) { it.startConfirmation }
    }

    @Transactional
    fun confirmCompletion(orderId: String, party: ConfirmingParty){
        if(!getOrder(orderId).startConfirmation.isConfirmed)
            throw FatumServiceException(CatalogErrorCode.DELIVERING_NOT_CONFIRMED)
        processConfirmation(orderId, party) { it.completionConfirmation }
    }

    @Transactional
    fun confirmPayment(orderId: String, party: ConfirmingParty){
        val order = getOrder(orderId)
        if(!order.completionConfirmation.isConfirmed)
            throw FatumServiceException(CatalogErrorCode.COMPLETION_NOT_CONFIRMED)
        processConfirmation(orderId, party) { it.paymentConfirmation }
        order.status = OrderStatus.COMPLETED
    }












    //==========================================HELPERS==============================================

    private fun getService(serviceId: String) = catalogItemService.findById(serviceId, ServiceStatus.ACTIVE)
    private fun getOrder(orderId: String) = orderRepository.findById(orderId)
        .orElseThrow { FatumServiceException(CatalogErrorCode.ORDER_NOT_FOUND) }

    private fun validateUser(userId : String){
        if(!userServiceClient.getUserById(userId).isValid)
            throw FatumServiceException(CatalogErrorCode.INVALID_USER)
    }



    private fun providerIsFree(providerId: String, booking: LocalDateTime): Boolean{
        reviewOrderOverlap(providerId, booking)
        val inUnavailability = providerUnavailableRangeService.scheduledDateInRange(providerId, booking)
        if (
            inUnavailability
            || timeGapPolicy(booking, LocalDateTime.now())
            ) return false
        val providerIsAble =
            providerAvailabilityService.providerIsAvailable(
                providerId,
                booking.dayOfWeek,
                booking.toLocalTime()
            )

        return providerIsAble
    }

    private fun reviewOrderOverlap(providerId: String, booking: LocalDateTime){
        val providerOrders = orderRepository.findByProviderId(providerId)
        val overlap = providerOrders.any{
            order ->
            timeGapPolicy(order.booking, booking)
        }
        if(overlap) throw FatumServiceException(CatalogErrorCode.OVERLAPPING_RANGE)
    }

    private fun timeGapPolicy(time1: LocalDateTime, time2: LocalDateTime) : Boolean{
        return Duration.between(time1, time2).abs().toMinutes() < gapTime
    }

    private fun processConfirmation(
        orderId: String,
        party: ConfirmingParty,
        confirmationSelector: (Order) -> DualConfirmation
    ) {
        val order = getOrder(orderId)
        val confirmation = confirmationSelector(order)

        if (confirmation.hasConfirmed(party)) {
            throw FatumServiceException(CatalogErrorCode.FORBIDDEN)
        }

        confirmation.confirm(party)
        orderRepository.save(order)
    }

}