package fatum.catalog.realtime.dto

import fatum.catalog.model.constant.OrderStatus
import java.time.LocalDateTime

enum class OrderChangeType {
    ORDER_CREATED,
    ORDER_UPDATED,
    ORDER_CANCELLED
}

enum class ConfirmationState {
    PENDING,
    PARTIAL,
    CONFIRMED,
    CONTRADICTED,
    BOTH_DENIED
}

data class ConfirmationView(
    val byCustomer: Boolean?,
    val byProvider: Boolean?,
    val state: ConfirmationState
)

/**
 * Instantánea de una orden en el momento del commit.
 *
 * Es deliberadamente "delgada": estado, agenda y confirmaciones. Los detalles pesados
 * (respuestas del cliente, dirección exacta, requisitos) se consultan por REST cuando el
 * cliente abre la orden, de modo que el canal en tiempo real nunca transporta más PII de la
 * necesaria y el mensaje cabe en un frame pequeño.
 */
data class OrderRealtimeUpdate(
    val changeType: OrderChangeType,
    val orderId: String,
    val status: OrderStatus,
    val booking: LocalDateTime,
    val estimatedTime: Int?,
    val reason: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deliveryStartedAt: LocalDateTime?,
    val clientId: String,
    val clientName: String,
    val clientUsername: String,
    val providerId: String,
    val providerName: String?,
    val providerUsername: String?,
    val serviceId: String?,
    val startConfirmation: ConfirmationView,
    val completionConfirmation: ConfirmationView,
    val paymentConfirmation: ConfirmationView
)
