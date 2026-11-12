package fatum.catalog.event.notification.order

import java.time.LocalDateTime

data class OrderStatusChangedNotify(
    val orderId: String,
    val status: String,
    val estimatedTime: Int?,
    val booking : LocalDateTime?
)