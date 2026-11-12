package fatum.catalog.realtime.dto

import fatum.catalog.model.constant.OrderStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class CalendarChangeReason {
    AVAILABILITY_CHANGED,
    UNAVAILABILITY_CREATED,
    UNAVAILABILITY_CHANGED,
    UNAVAILABILITY_DELETED,
    ORDER_CREATED,
    ORDER_UPDATED,
    ORDER_CANCELLED
}

enum class CalendarUpdateMode {
    /** El payload trae los días recalculados: el cliente los reemplaza. */
    DAYS,
    /** Cambio de horizonte abierto (ej. horario semanal): el cliente debe volver a consultar el mes visible. */
    INVALIDATE
}

data class PublicCalendarSlot(
    val start: LocalDateTime,
    val end: LocalDateTime?,
    val status: OrderStatus
)

data class ProviderCalendarSlot(
    val orderId: String,
    val start: LocalDateTime,
    val end: LocalDateTime?,
    val status: OrderStatus
)

data class PublicCalendarDay(
    val date: LocalDate,
    val available: Boolean,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val unavailableReason: String?,
    val slots: List<PublicCalendarSlot>
)

data class ProviderCalendarDay(
    val date: LocalDate,
    val available: Boolean,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val unavailableReason: String?,
    val slots: List<ProviderCalendarSlot>
)

/** Parche público: se publica en `/topic/calendar/{providerId}` para cualquier visitante. */
data class PublicCalendarUpdate(
    val providerId: String,
    val reasons: Set<CalendarChangeReason>,
    val mode: CalendarUpdateMode,
    val from: LocalDate?,
    val to: LocalDate?,
    val day: DayOfWeek?,
    val days: List<PublicCalendarDay>
)

/** Parche privado: se envía solo al proveedor en `/user/queue/calendar`, con ids de orden. */
data class ProviderCalendarUpdate(
    val providerId: String,
    val reasons: Set<CalendarChangeReason>,
    val mode: CalendarUpdateMode,
    val from: LocalDate?,
    val to: LocalDate?,
    val day: DayOfWeek?,
    val days: List<ProviderCalendarDay>
)
