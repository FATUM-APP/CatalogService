package fatum.catalog.model.vo

import fatum.catalog.model.constant.OrderStatus
import java.time.LocalDateTime

data class BookedSlot(
    val orderId: String,
    val booking: LocalDateTime,
    val estimatedTime: Int?,
    val status: OrderStatus
)
