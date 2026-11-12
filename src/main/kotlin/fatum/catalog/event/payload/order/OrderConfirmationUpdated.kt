package fatum.catalog.event.payload.order

data class OrderConfirmationUpdated(
    val orderId: String,
    val phase: String,
    val party: String,
    val action: String,
    val statusChanged: Boolean,
    val newStatus: String?,
    val providerId: String,
    val clientId: String
)