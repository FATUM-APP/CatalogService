package fatum.catalog.realtime.event

/**
 * Eventos de dominio. No saben nada de WebSocket: solo describen lo que pasó en el negocio.
 * El que los traduce a STOMP es `RealtimeEventListener`.
 */

enum class OrderChangeType {
    ORDER_CREATED,
    ORDER_UPDATED,
    ORDER_CANCELLED
}

data class OrderChangedEvent(
    val orderId: String,
    val changeType: OrderChangeType
)

enum class CalendarChangeReason {
    AVAILABILITY_CHANGED,
    UNAVAILABILITY_CHANGED,
    ORDER_CREATED,
    ORDER_UPDATED,
    ORDER_CANCELLED
}

data class CalendarChangedEvent(
    val providerId: String,
    val reason: CalendarChangeReason
)
