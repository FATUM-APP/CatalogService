package fatum.catalog.repository

import fatum.catalog.model.Order
import fatum.catalog.model.constant.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface OrderRepository: JpaRepository<Order, String> {
    fun findByProviderId(providerId: String): List<Order>
    fun findByProviderIdAndStatus(providerId: String, status: OrderStatus): List<Order>
    fun findByCatalogItemId(serviceId: String): List<Order>
    fun findByCatalogItemIdAndStatus(serviceId: String, status: OrderStatus): List<Order>
    fun findByClientId(clientId: String): List<Order>
    fun findByClientIdAndStatus(clientId: String, status: OrderStatus): List<Order>
    fun findByProviderIdAndStatusAndBookingBetween(
        providerId: String,
        status: OrderStatus,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<Order>
}
