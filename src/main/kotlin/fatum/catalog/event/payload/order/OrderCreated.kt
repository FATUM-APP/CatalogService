package fatum.catalog.event.payload.order

data class OrderCreated(
    val providerId: String,
    val clientUsername: String,
    val orderId: String,
    val serviceId: String
)