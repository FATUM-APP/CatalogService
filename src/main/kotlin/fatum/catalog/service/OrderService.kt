package fatum.catalog.service

import fatum.catalog.client.UserServiceClient
import fatum.catalog.dto.BookService
import fatum.catalog.dto.mapper.toEntity
import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.constant.ServiceStatus
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





    //==========================================HELPERS==============================================

    private fun getService(serviceId: String) = catalogItemService.findById(serviceId, ServiceStatus.ACTIVE)

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
}