package fatum.catalog.realtime.dto

import java.time.Instant

/**
 * Sobre común de todos los mensajes del canal.
 *
 * - `eventId` permite deduplicar entregas (el broker puede reenviar en reconexiones).
 * - `version` permite evolucionar el payload sin romper clientes antiguos.
 * - `occurredAt` permite descartar mensajes fuera de orden (`updatedAt` del payload manda).
 */
data class RealtimeMessage<T>(
    val eventId: String,
    val type: String,
    val occurredAt: Instant,
    val version: Int = 1,
    val payload: T
)
