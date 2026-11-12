package fatum.catalog.realtime.listener

import fatum.catalog.model.Order
import fatum.catalog.model.ProviderAvailability
import fatum.catalog.model.ProviderUnavailableRange
import fatum.catalog.model.constant.OrderStatus
import fatum.catalog.realtime.RealtimeHooks
import fatum.catalog.realtime.dto.CalendarChangeReason
import fatum.catalog.realtime.dto.OrderChangeType
import fatum.catalog.realtime.event.CalendarChange
import fatum.catalog.realtime.mapper.toRealtimeUpdate
import jakarta.persistence.PostPersist
import jakarta.persistence.PostRemove
import jakarta.persistence.PostUpdate
import org.slf4j.LoggerFactory

/**
 * Detecta cambios de órdenes a nivel de persistencia.
 *
 * Así la capa de servicio no publica eventos: cualquier escritura de `Order` (desde este servicio,
 * un job, un endpoint nuevo o incluso una migración) dispara el mensaje, sin riesgo de olvidar
 * una llamada.
 *
 * Estos callbacks corren dentro del `flush` de Hibernate, por eso solo construyen un DTO en
 * memoria: la consulta del calendario y el envío STOMP ocurren después del commit.
 */
class OrderRealtimeListener {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostPersist
    fun onCreated(order: Order) = publish(order, OrderChangeType.ORDER_CREATED)

    @PostUpdate
    fun onUpdated(order: Order) = publish(
        order,
        if (order.status == OrderStatus.CANCELLED) OrderChangeType.ORDER_CANCELLED
        else OrderChangeType.ORDER_UPDATED
    )

    private fun publish(order: Order, changeType: OrderChangeType) {
        val dispatcher = RealtimeHooks.current() ?: return
        runCatching { dispatcher.orderChanged(order.toRealtimeUpdate(changeType)) }
            .onFailure { log.warn("Could not publish realtime update for order {}", order.id, it) }
    }
}

/**
 * El horario semanal de un proveedor aplica a todas las fechas futuras de ese día de la semana:
 * se publica como invalidación para que los clientes recarguen el mes visible.
 */
class ProviderAvailabilityRealtimeListener {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostPersist
    @PostUpdate
    @PostRemove
    fun onChanged(availability: ProviderAvailability) {
        val dispatcher = RealtimeHooks.current() ?: return
        runCatching {
            dispatcher.calendarChanged(
                CalendarChange.invalidate(
                    providerId = availability.providerId,
                    reason = CalendarChangeReason.AVAILABILITY_CHANGED,
                    day = availability.id.day
                )
            )
        }.onFailure {
            log.warn("Could not publish availability change for provider {}", availability.providerId, it)
        }
    }
}

/** Un rango de indisponibilidad afecta fechas concretas: se parchean esos días. */
class ProviderUnavailableRangeRealtimeListener {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostPersist
    fun onCreated(range: ProviderUnavailableRange) = publish(range, CalendarChangeReason.UNAVAILABILITY_CREATED)

    @PostUpdate
    fun onUpdated(range: ProviderUnavailableRange) = publish(range, CalendarChangeReason.UNAVAILABILITY_CHANGED)

    @PostRemove
    fun onDeleted(range: ProviderUnavailableRange) = publish(range, CalendarChangeReason.UNAVAILABILITY_DELETED)

    private fun publish(range: ProviderUnavailableRange, reason: CalendarChangeReason) {
        val dispatcher = RealtimeHooks.current() ?: return
        runCatching {
            dispatcher.calendarChanged(
                CalendarChange.range(
                    providerId = range.providerId,
                    reason = reason,
                    from = range.startDate.toLocalDate(),
                    to = range.finalDate.toLocalDate()
                )
            )
        }.onFailure {
            log.warn("Could not publish unavailability change for provider {}", range.providerId, it)
        }
    }
}
