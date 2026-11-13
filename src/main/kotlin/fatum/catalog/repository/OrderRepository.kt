package fatum.catalog.repository

import fatum.catalog.model.Order
import fatum.catalog.model.constant.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface OrderRepository: JpaRepository<Order, String> {

    fun findByProviderIdOrderByUpdatedAtDesc(providerId: String): List<Order>
    fun findByProviderIdAndStatusOrderByUpdatedAtDesc(providerId: String, status: OrderStatus): List<Order>
    fun findByCatalogItemIdOrderByUpdatedAtDesc(serviceId: String): List<Order>
    fun findByCatalogItemIdAndStatusOrderByUpdatedAtDesc(serviceId: String, status: OrderStatus): List<Order>
    fun findByClientIdOrderByUpdatedAtDesc(clientId: String): List<Order>
    fun findByClientIdAndStatusOrderByUpdatedAtDesc(clientId: String, status: OrderStatus): List<Order>
    fun findByProviderIdAndStatusAndBookingBetweenOrderByUpdatedAtDesc(
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
