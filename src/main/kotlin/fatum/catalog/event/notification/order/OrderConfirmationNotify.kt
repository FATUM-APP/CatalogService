package fatum.catalog.event.notification.order

data class OrderConfirmationNotify(
    val orderId: String,
    val phase: String,
    val party: String,
    val action: String,
    val statusChanged: Boolean,
    val newStatus: String?
)