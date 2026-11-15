package fatum.catalog.event.order

/**
 * Cada hecho viaja bajo su propio `detail-type`, igual que en UserService: así una regla de
 * EventBridge puede filtrar uno solo sin leer el payload y adivinar.
 */
enum class OrderEventType {
    /** Un cliente pidió un servicio. */
    ORDER_CREATED,

    /** La orden cambió de estado (aceptada, rechazada, cancelada, en entrega, completada...). */
    ORDER_STATUS_CHANGED,

    /** Una de las dos partes confirmó o negó un paso (inicio, entrega, pago). */
    ORDER_CONFIRMATION_CHANGED,

    ORDER_COMPLETED
}
