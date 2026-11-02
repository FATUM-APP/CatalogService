package fatum.catalog.listener

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener


@Component
class OrderEventListener (
    private val messagingTemplate: SimpMessagingTemplate
) {
    @TransactionalEventListener
    fun onOrderStatusChanged(event: OrderStatusChangedEvent) {
        val notification = OrderNotification(
            orderId = event.orderId,
            status = event.status,
            reason = event.reason,
            estimatedTime = event.estimatedTime
        )
        // Envia al canal del cliente
        messagingTemplate.convertAndSend(
            "/catalog-items/client/${event.clientId}/orders", notification
        )
        // Envia al canal del proveedor
        messagingTemplate.convertAndSend(
            "/catalog-items/provider/${event.providerId}/orders", notification
        )
    }
}