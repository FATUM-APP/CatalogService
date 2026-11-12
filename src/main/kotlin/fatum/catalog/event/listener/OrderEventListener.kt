package fatum.catalog.event.listener

import fatum.catalog.dto.mapper.toNotification
import fatum.catalog.event.SseEmitterRegistry
import fatum.catalog.event.payload.order.OrderCancelled
import fatum.catalog.event.payload.order.OrderCreated
import fatum.catalog.event.payload.order.OrderStatusChanged
import fatum.catalog.model.constant.OrderStatus
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class OrderEventListener(
    private val emitterRegistry: SseEmitterRegistry,
    private val eventPublisher: ApplicationEventPublisher
) {

    @EventListener
    fun onOrderCreated(event: OrderCreated) {
        emitterRegistry.sendToUser(
            event.providerId,
            "ORDER_CREATED",
            event.toNotification())
    }

    @EventListener
    fun onStatusChanged(event: OrderStatusChanged) {
        val payload = event.toNotification()
        emitterRegistry.sendToUser(event.clientId, "ORDER_STATUS_CHANGED", payload)

        emitterRegistry.sendToTopic(
            "order:${event.orderId}",
            "ORDER_STATUS_CHANGED",
            event.toNotification()
        )

        if (event.status == "OrderStatus.ACCEPTED") {
            emitterRegistry.sendToTopic(
                "calendar:${event.providerId}",
                "SLOT_BOOKED",
                event.toNotification()
            )
        }
    }

    @EventListener
    fun onOrderCancelled(event: OrderCancelled) {
        emitterRegistry.sendToUser(
            event.providerId,
            "ORDER_CANCELLED",
            event.toNotification()
        )

        emitterRegistry.sendToUser(
            event.clientId,
            "ORDER_CANCELLED",
            event.toNotification()
        )
    }



}