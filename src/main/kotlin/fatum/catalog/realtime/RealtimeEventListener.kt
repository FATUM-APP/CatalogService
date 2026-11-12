package fatum.catalog.realtime

import fatum.catalog.dto.mapper.toResponse
import fatum.catalog.realtime.event.CalendarChangeReason
import fatum.catalog.realtime.event.CalendarChangedEvent
import fatum.catalog.realtime.event.OrderChangedEvent
import fatum.catalog.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.LocalDateTime

/**
 * Traduce eventos de dominio a mensajes STOMP.
 *
 * Solo se ejecuta si la transacción hizo commit: si hay rollback, el cliente no se entera de nada.
 * Como `open-in-view` está apagado, se vuelve a leer la orden en una transacción nueva para armar
 * el `OrderDTO` con el estado definitivo.
 */
@Component
class RealtimeEventListener(
    private val messagingTemplate: SimpMessagingTemplate,
    private val orderRepository: OrderRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    fun onOrderChanged(event: OrderChangedEvent) {
        runCatching {
            val order = orderRepository.findById(event.orderId).orElse(null) ?: return
            val message = RealtimeMessage(
                type = event.changeType.name,
                at = LocalDateTime.now(),
                payload = order.toResponse()
            )

            // Las dos partes de la orden, cada una en su propia cola.
            messagingTemplate.convertAndSendToUser(order.clientId, RealtimeTopics.ORDERS_QUEUE, message)
            messagingTemplate.convertAndSendToUser(order.providerId, RealtimeTopics.ORDERS_QUEUE, message)

            // Y el calendario del proveedor también cambió.
            notifyCalendar(order.providerId, CalendarChangeReason.valueOf(event.changeType.name))
        }.onFailure {
            log.warn("No se pudo notificar el cambio de la orden {}", event.orderId, it)
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCalendarChanged(event: CalendarChangedEvent) {
        runCatching {
            notifyCalendar(event.providerId, event.reason)
        }.onFailure {
            log.warn("No se pudo notificar el calendario de {}", event.providerId, it)
        }
    }

    private fun notifyCalendar(providerId: String, reason: CalendarChangeReason) {
        messagingTemplate.convertAndSend(
            RealtimeTopics.calendar(providerId),
            RealtimeMessage(
                type = "CALENDAR_CHANGED",
                at = LocalDateTime.now(),
                payload = CalendarChangedPayload(providerId, reason.name)
            )
        )
    }
}
