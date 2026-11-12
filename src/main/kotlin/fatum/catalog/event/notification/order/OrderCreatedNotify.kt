package fatum.catalog.event.notification.order

data class OrderCreatedNotify(
    val orderId: String,
    val serviceId: String,
    val clientUsername: String,
)