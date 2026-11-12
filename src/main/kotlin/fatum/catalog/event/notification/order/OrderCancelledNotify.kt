package fatum.catalog.event.notification.order

data class OrderCancelledNotify(
    val orderId: String,
    val reason: String,
)
