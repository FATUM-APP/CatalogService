package fatum.catalog.model.constant

/**
 * Los tres pasos de confirmación doble de una orden, en el orden en que ocurren.
 *
 * Los dos nombres son los que viajan en el evento `ORDER_CONFIRMATION_CHANGED`, así que cambiarlos
 * es un cambio de contrato con quien consuma el bus.
 */
enum class ConfirmationStage {
    START,
    COMPLETION,
    PAYMENT
}
