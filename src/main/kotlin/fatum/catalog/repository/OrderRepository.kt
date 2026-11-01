package fatum.catalog.repository

import fatum.catalog.model.Order
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository: JpaRepository<Order, String> {
    fun findByProviderId(providerId: String): List<Order>
}