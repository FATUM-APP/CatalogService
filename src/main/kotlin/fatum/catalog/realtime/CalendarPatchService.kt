package fatum.catalog.realtime

import fatum.catalog.model.vo.CalendarDay
import fatum.catalog.realtime.config.RealtimeProperties
import fatum.catalog.realtime.dto.CalendarUpdateMode
import fatum.catalog.realtime.event.CalendarChange
import fatum.catalog.service.CalendarService
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Resultado del cálculo: o trae días concretos, o pide al cliente invalidar su vista. */
data class CalendarPatch(
    val mode: CalendarUpdateMode,
    val days: List<CalendarDay>
)

/**
 * Recalcula los días afectados por un cambio.
 *
 * El cálculo se hace UNA sola vez por cambio, fuera de la transacción de escritura y antes de
 * enviar el mensaje, y ese mismo payload se entrega a todos los suscriptores del proveedor. El
 * costo no crece con el número de clientes conectados.
 */
@Component
class CalendarPatchService(
    private val calendarService: CalendarService,
    private val properties: RealtimeProperties
) {

    fun resolve(change: CalendarChange): CalendarPatch {
        val from = change.from
        val to = change.to
        if (change.mode == CalendarUpdateMode.INVALIDATE || from == null || to == null) return invalidate()
        if (from.isAfter(to)) return invalidate()

        val horizon = ChronoUnit.DAYS.between(from, to) + 1
        if (horizon > properties.calendar.maxPatchDays) return invalidate()

        return CalendarPatch(CalendarUpdateMode.DAYS, days(change.providerId, from, to))
    }

    fun days(providerId: String, from: LocalDate, to: LocalDate): List<CalendarDay> =
        monthsBetween(from, to)
            .flatMap { month -> calendarService.getMonthCalendar(providerId, month.year, month.monthValue) }
            .filter { !it.date.isBefore(from) && !it.date.isAfter(to) }
            .sortedBy { it.date }

    private fun monthsBetween(from: LocalDate, to: LocalDate): List<YearMonth> =
        generateSequence(YearMonth.from(from)) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(YearMonth.from(to)) }
            .toList()

    private fun invalidate() = CalendarPatch(CalendarUpdateMode.INVALIDATE, emptyList())
}
