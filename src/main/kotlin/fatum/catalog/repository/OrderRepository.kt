package fatum.catalog.repository

import fatum.catalog.model.Order
import fatum.catalog.model.constant.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
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

    fun countByCatalogItemId(serviceId: String): Long

    @Query("""
    SELECT o.status, COUNT(o) FROM Order o
    WHERE o.catalogItem.id = :serviceId
    GROUP BY o.status
    """)
    fun countByServiceGroupedByStatus(@Param("serviceId") serviceId: String): List<Array<Any>>

    @Query("""
    SELECT COUNT(o) FROM Order o
    WHERE o.catalogItem.id = :serviceId
      AND o.createdAt BETWEEN :start AND :end
""")
    fun countNewOrdersInPeriod(
        @Param("serviceId") serviceId: String,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): Long
}
