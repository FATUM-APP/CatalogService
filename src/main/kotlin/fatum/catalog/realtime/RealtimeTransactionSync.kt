package fatum.catalog.realtime

import fatum.catalog.realtime.dto.OrderRealtimeUpdate
import fatum.catalog.realtime.event.CalendarChange
import org.springframework.transaction.support.TransactionSynchronization

/**
 * Acumula los cambios de UNA transacción y los publica solo si el commit fue exitoso.
 *
 * Reglas de agregación:
 *  - Órdenes: gana la última instantánea por `orderId` (si la orden cambió tres veces, el cliente
 *    recibe un único mensaje con el estado final).
 *  - Calendario: se fusionan rangos y motivos por proveedor.
 */
class RealtimeTransactionSync(
    private val flush: (RealtimeTransactionSync) -> Unit
) : TransactionSynchronization {

    private val orderUpdates = LinkedHashMap<String, OrderRealtimeUpdate>()
    private val calendarChanges = LinkedHashMap<String, CalendarChange>()

    fun mergeOrder(update: OrderRealtimeUpdate) {
        orderUpdates[update.orderId] = update
    }

    fun mergeCalendar(change: CalendarChange) {
        calendarChanges.merge(change.providerId, change) { current, incoming -> current.merge(incoming) }
    }

    fun orders(): Collection<OrderRealtimeUpdate> = orderUpdates.values

    fun calendars(): Collection<CalendarChange> = calendarChanges.values

    override fun afterCommit() {
        // El commit ya ocurrió: publicar aquí garantiza que los clientes nunca reciben un estado
        // que la base de datos terminó revirtiendo.
        flush(this)
    }

    override fun afterCompletion(status: Int) {
        orderUpdates.clear()
        calendarChanges.clear()
    }
}
