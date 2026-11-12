package fatum.catalog.event.listener

import fatum.catalog.dto.mapper.toNotification
import fatum.catalog.event.SseEmitterRegistry
import fatum.catalog.event.notification.order.OrderStatusChangedNotify
import fatum.catalog.event.payload.order.OrderCancelled
import fatum.catalog.event.payload.order.OrderConfirmationUpdated
import fatum.catalog.event.payload.order.OrderCreated
import fatum.catalog.event.payload.order.OrderStatusChanged
import fatum.catalog.model.constant.OrderStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class OrderEventListener(
    private val emitterRegistry: SseEmitterRegistry
) {

    @TransactionalEventListener
    fun onOrderCreated(event: OrderCreated) {
        emitterRegistry.sendToUser(
            event.providerId,
            "ORDER_CREATED",
            event.toNotification()
        )
    }

    @TransactionalEventListener
    fun onStatusChanged(event: OrderStatusChanged) {
        val payload = event.toNotification()
        emitterRegistry.sendToUser(event.clientId, "ORDER_STATUS_CHANGED", payload)

        emitterRegistry.sendToTopic(
            "order:${event.orderId}",
            "ORDER_STATUS_CHANGED",
            payload
        )

        if (event.status == "ACCEPTED") {
            emitterRegistry.sendToTopic(
                "calendar:${event.providerId}",
                "SLOT_BOOKED",
                payload
            )
        }
    }

    @TransactionalEventListener
    fun onOrderCancelled(event: OrderCancelled) {
        val payload = event.toNotification()
        emitterRegistry.sendToUser(event.providerId, "ORDER_CANCELLED", payload)
        emitterRegistry.sendToUser(event.clientId, "ORDER_CANCELLED", payload)
    }

    @TransactionalEventListener
    fun onConfirmationUpdated(event: OrderConfirmationUpdated) {
        val payload = event.toNotification()

        emitterRegistry.sendToUser(event.providerId, "ORDER_CONFIRMATION_UPDATED", payload)
        emitterRegistry.sendToUser(event.clientId, "ORDER_CONFIRMATION_UPDATED", payload)

        if (event.statusChanged && event.newStatus != null) {
            emitterRegistry.sendToTopic(
                "order:${event.orderId}",
                "ORDER_STATUS_CHANGED",
                OrderStatusChangedNotify(
                    orderId = event.orderId,
                    status = event.newStatus,
                    estimatedTime = null,
                    booking = null
                )
            )
        }
    }
}