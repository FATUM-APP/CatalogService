package fatum.catalog.event.payload.order

import java.time.LocalDateTime

data class OrderStatusChanged(
    val orderId: String,
    val status: String,
    val estimatedTime: Int? = null,
    val booking: LocalDateTime,
    val providerId: String,
    val clientId : String
)