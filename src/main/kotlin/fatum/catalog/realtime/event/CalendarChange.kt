package fatum.catalog.realtime.event

import fatum.catalog.realtime.dto.CalendarChangeReason
import fatum.catalog.realtime.dto.CalendarUpdateMode
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Cambio pendiente de publicar sobre el calendario de un proveedor.
 *
 * Se agrega por proveedor dentro de la misma transacción (union de rangos y motivos), de forma
 * que un `cancelProviderOrders` que toca 30 órdenes del mismo día produce un solo mensaje.
 */
data class CalendarChange(
    val providerId: String,
    val reasons: Set<CalendarChangeReason>,
    val mode: CalendarUpdateMode,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val day: DayOfWeek? = null
) {

    fun merge(other: CalendarChange): CalendarChange {
        require(other.providerId == providerId) { "Cannot merge changes of different providers" }
        return CalendarChange(
            providerId = providerId,
            reasons = reasons + other.reasons,
            mode = if (mode == CalendarUpdateMode.INVALIDATE || other.mode == CalendarUpdateMode.INVALIDATE)
                CalendarUpdateMode.INVALIDATE else CalendarUpdateMode.DAYS,
            from = listOfNotNull(from, other.from).minOrNull(),
            to = listOfNotNull(to, other.to).maxOrNull(),
            day = day ?: other.day
        )
    }

    companion object {
        /** Cambio acotado a un rango de fechas: se envía parcheado con los días recalculados. */
        fun range(
            providerId: String,
            reason: CalendarChangeReason,
            from: LocalDate,
            to: LocalDate
        ): CalendarChange = CalendarChange(
            providerId = providerId,
            reasons = setOf(reason),
            mode = CalendarUpdateMode.DAYS,
            from = minOf(from, to),
            to = maxOf(from, to)
        )

        /**
         * Cambio de horizonte abierto (ej. horario semanal que aplica a todas las fechas de ese día):
         * el cliente debe volver a consultar el mes que está mostrando.
         */
        fun invalidate(
            providerId: String,
            reason: CalendarChangeReason,
            day: DayOfWeek? = null
        ): CalendarChange = CalendarChange(
            providerId = providerId,
            reasons = setOf(reason),
            mode = CalendarUpdateMode.INVALIDATE,
            day = day
        )
    }
}
