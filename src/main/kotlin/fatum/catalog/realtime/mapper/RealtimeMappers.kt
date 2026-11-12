package fatum.catalog.realtime.mapper

import fatum.catalog.model.Order
import fatum.catalog.model.vo.BookedSlot
import fatum.catalog.model.vo.CalendarDay
import fatum.catalog.model.vo.DualConfirmation
import fatum.catalog.realtime.dto.ConfirmationState
import fatum.catalog.realtime.dto.ConfirmationView
import fatum.catalog.realtime.dto.OrderChangeType
import fatum.catalog.realtime.dto.OrderRealtimeUpdate
import fatum.catalog.realtime.dto.ProviderCalendarDay
import fatum.catalog.realtime.dto.ProviderCalendarSlot
import fatum.catalog.realtime.dto.PublicCalendarDay
import fatum.catalog.realtime.dto.PublicCalendarSlot
import java.time.LocalDateTime

/**
 * Construye la instantánea que viaja por el canal.
 *
 * Se ejecuta dentro de la transacción (en el callback de JPA), así que solo toca campos ya
 * cargados: `catalogItem` es `@ManyToOne` EAGER y `providerId` es columna propia de `Order`.
 */
fun Order.toRealtimeUpdate(changeType: OrderChangeType): OrderRealtimeUpdate =
    OrderRealtimeUpdate(
        changeType = changeType,
        orderId = requireNotNull(id) { "Order must be persisted before publishing" },
        status = status,
        booking = booking,
        estimatedTime = estimatedTime,
        reason = reason,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deliveryStartedAt = deliveryStartedAt,
        clientId = clientId,
        clientName = clientName,
        clientUsername = clientUsername,
        providerId = providerId,
        providerName = catalogItem.providerName,
        providerUsername = catalogItem.providerUsername,
        serviceId = catalogItem.id,
        startConfirmation = startConfirmation.toView(),
        completionConfirmation = completionConfirmation.toView(),
        paymentConfirmation = paymentConfirmation.toView()
    )

fun DualConfirmation.toView(): ConfirmationView =
    ConfirmationView(byCustomer = byCustomer, byProvider = byProvider, state = toState())

private fun DualConfirmation.toState(): ConfirmationState = when {
    isConfirmed -> ConfirmationState.CONFIRMED
    isContradicted -> ConfirmationState.CONTRADICTED
    isBothDenied -> ConfirmationState.BOTH_DENIED
    byCustomer == true || byProvider == true -> ConfirmationState.PARTIAL
    else -> ConfirmationState.PENDING
}

/** Vista pública: sin `orderId`, apta para cualquier visitante del perfil del proveedor. */
fun CalendarDay.toPublicDay(): PublicCalendarDay =
    PublicCalendarDay(
        date = date,
        available = isAvailable,
        startTime = initialTime,
        endTime = finalTime,
        unavailableReason = unavailableReason,
        slots = bookedSlots.map { it.toPublicSlot() }
    )

/** Vista del proveedor: incluye el id de la orden para que pueda abrirla directamente. */
fun CalendarDay.toProviderDay(): ProviderCalendarDay =
    ProviderCalendarDay(
        date = date,
        available = isAvailable,
        startTime = initialTime,
        endTime = finalTime,
        unavailableReason = unavailableReason,
        slots = bookedSlots.map { it.toProviderSlot() }
    )

private fun BookedSlot.toPublicSlot() =
    PublicCalendarSlot(start = booking, end = booking.plusMinutesOrNull(estimatedTime), status = status)

private fun BookedSlot.toProviderSlot() =
    ProviderCalendarSlot(
        orderId = orderId,
        start = booking,
        end = booking.plusMinutesOrNull(estimatedTime),
        status = status
    )

private fun LocalDateTime.plusMinutesOrNull(minutes: Int?): LocalDateTime? =
    minutes?.let { plusMinutes(it.toLong()) }
