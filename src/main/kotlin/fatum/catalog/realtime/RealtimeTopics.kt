package fatum.catalog.realtime

import java.time.LocalDateTime

object RealtimeTopics {

    /** Público: cualquiera puede mirar el calendario de un proveedor. */
    const val CALENDAR_TOPIC = "/topic/calendar"

    /** Privado: Spring lo resuelve a la cola exclusiva de cada sesión autenticada. */
    const val ORDERS_QUEUE = "/queue/orders"

    fun calendar(providerId: String) = "$CALENDAR_TOPIC/$providerId"
}

/** Sobre común para todos los mensajes del canal. */
data class RealtimeMessage<T>(
    val type: String,
    val at: LocalDateTime,
    val payload: T
)

/**
 * El calendario no viaja en el mensaje: solo avisa que cambió.
 * El cliente vuelve a pedir el mes que está viendo por REST (una línea en el front).
 */
data class CalendarChangedPayload(
    val providerId: String,
    val reason: String
)
