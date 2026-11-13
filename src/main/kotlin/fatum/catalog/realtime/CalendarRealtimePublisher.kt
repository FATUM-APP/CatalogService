package fatum.catalog.realtime

import fatum.catalog.realtime.sse.SseHub
import org.springframework.stereotype.Component
import java.time.LocalDate

/** Qué cambió en el calendario. El front lo usa para decidir si recarga el mes o solo unos días. */
enum class CalendarChange {
    WEEKLY_SCHEDULE_CHANGED,
    UNAVAILABLE_ADDED,
    UNAVAILABLE_CHANGED,
    UNAVAILABLE_REMOVED,
    ORDER_UPDATED
}

/**
 * Avisa al calendario de un proveedor.
 *
 * <p>Al tema `calendar:<providerId>` se suscriben el proveedor y los clientes que estén mirando sus
 * horarios, así que todos ven el cambio al mismo tiempo.</p>
 */
@Component
class CalendarRealtimePublisher(private val hub: SseHub) {

    fun changed(
        providerId: String,
        change: CalendarChange,
        start: LocalDate? = null,
        end: LocalDate? = null
    ) {
        hub.publish(
            listOf(SseHub.calendarTopic(providerId)),
            "calendar",
            mapOf(
                "type" to "CALENDAR_CHANGED",
                "providerId" to providerId,
                "change" to change.name,
                "start" to start?.toString(),
                "end" to end?.toString()
            )
        )
    }
}
