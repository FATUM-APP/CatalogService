package fatum.catalog.event.payload.order

data class OrderCancelled(
    val orderId: String,
    val reason: String,
    val providerId: String,
    val clientId : String
)
