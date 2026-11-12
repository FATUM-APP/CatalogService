package fatum.catalog.realtime

import fatum.catalog.realtime.config.RealtimeProperties
import fatum.catalog.realtime.dto.CalendarChangeReason
import fatum.catalog.realtime.dto.CalendarUpdateMode
import fatum.catalog.realtime.dto.OrderChangeType
import fatum.catalog.realtime.dto.OrderRealtimeUpdate
import fatum.catalog.realtime.dto.ProviderCalendarUpdate
import fatum.catalog.realtime.dto.PublicCalendarUpdate
import fatum.catalog.realtime.dto.RealtimeMessage
import fatum.catalog.realtime.event.CalendarChange
import fatum.catalog.realtime.mapper.toProviderDay
import fatum.catalog.realtime.mapper.toPublicDay
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import java.util.UUID

/**
 * Único punto de salida del canal en tiempo real.
 *
 * Flujo:
 *   1. JPA detecta el cambio (`@PostPersist` / `@PostUpdate` / `@PostRemove`).
 *   2. Aquí se registra en la transacción en curso (sin I/O, sin consultas).
 *   3. Al hacer commit se calcula el parche de calendario (una vez) y se envía por STOMP.
 *
 * Si no hay transacción activa (por ejemplo un test o un job) se publica de inmediato.
 */
@Component
@ConditionalOnProperty(
    prefix = "catalog.realtime",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class RealtimeDispatcher(
    private val messagingTemplate: SimpMessagingTemplate,
    private val calendarPatchService: CalendarPatchService,
    private val properties: RealtimeProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun register() {
        RealtimeHooks.install(this)
    }

    @PreDestroy
    fun unregister() {
        RealtimeHooks.uninstall(this)
    }

    //==========================================ENTRADAS============================================

    fun orderChanged(update: OrderRealtimeUpdate) {
        if (!properties.enabled) return
        val sync = currentSync()
        if (sync == null) {
            dispatchOrder(update)
            dispatchCalendar(calendarChangeOf(update))
            return
        }
        sync.mergeOrder(update)
        sync.mergeCalendar(calendarChangeOf(update))
    }

    fun calendarChanged(change: CalendarChange) {
        if (!properties.enabled) return
        val sync = currentSync()
        if (sync == null) {
            dispatchCalendar(change)
            return
        }
        sync.mergeCalendar(change)
    }

    //==========================================SALIDAS============================================

    private fun dispatchOrder(update: OrderRealtimeUpdate) {
        val message = RealtimeMessage(
            eventId = UUID.randomUUID().toString(),
            type = update.changeType.name,
            occurredAt = Instant.now(),
            payload = update
        )
        // Ambos stakeholders reciben exactamente el mismo estado de la orden.
        sendToUser(update.clientId, RealtimeTopics.USER_ORDERS, message)
        if (update.providerId != update.clientId)
            sendToUser(update.providerId, RealtimeTopics.USER_ORDERS, message)
    }

    private fun dispatchCalendar(change: CalendarChange) {
        val patch = runCatching { calendarPatchService.resolve(change) }
            .getOrElse {
                log.warn(
                    "Could not compute calendar patch for provider {} ({}); sending invalidate",
                    change.providerId, change.reasons, it
                )
                CalendarPatch(CalendarUpdateMode.INVALIDATE, emptyList())
            }

        val publicUpdate = PublicCalendarUpdate(
            providerId = change.providerId,
            reasons = change.reasons,
            mode = patch.mode,
            from = change.from,
            to = change.to,
            day = change.day,
            days = patch.days.map { it.toPublicDay() }
        )
        sendToDestination(
            RealtimeTopics.publicCalendar(change.providerId),
            RealtimeMessage(UUID.randomUUID().toString(), CALENDAR_CHANGED, Instant.now(), payload = publicUpdate)
        )

        val providerUpdate = ProviderCalendarUpdate(
            providerId = change.providerId,
            reasons = change.reasons,
            mode = patch.mode,
            from = change.from,
            to = change.to,
            day = change.day,
            days = patch.days.map { it.toProviderDay() }
        )
        sendToUser(
            change.providerId,
            RealtimeTopics.USER_CALENDAR,
            RealtimeMessage(UUID.randomUUID().toString(), CALENDAR_CHANGED, Instant.now(), payload = providerUpdate)
        )
    }

    //==========================================HELPERS============================================

    private fun calendarChangeOf(update: OrderRealtimeUpdate): CalendarChange {
        val reason = when (update.changeType) {
            OrderChangeType.ORDER_CREATED -> CalendarChangeReason.ORDER_CREATED
            OrderChangeType.ORDER_UPDATED -> CalendarChangeReason.ORDER_UPDATED
            OrderChangeType.ORDER_CANCELLED -> CalendarChangeReason.ORDER_CANCELLED
        }
        val day = update.booking.toLocalDate()
        return CalendarChange.range(update.providerId, reason, day, day)
    }

    private fun sendToUser(userId: String, destination: String, message: RealtimeMessage<*>) {
        runCatching { messagingTemplate.convertAndSendToUser(userId, destination, message) }
            .onFailure { log.warn("Realtime delivery to user {} ({}) failed: {}", userId, destination, it.message) }
    }

    private fun sendToDestination(destination: String, message: RealtimeMessage<*>) {
        runCatching { messagingTemplate.convertAndSend(destination, message) }
            .onFailure { log.warn("Realtime delivery to {} failed: {}", destination, it.message) }
    }

    private fun currentSync(): RealtimeTransactionSync? {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return null
        val existing = TransactionSynchronizationManager.getSynchronizations()
            .filterIsInstance<RealtimeTransactionSync>()
            .firstOrNull()
        if (existing != null) return existing

        val sync = RealtimeTransactionSync { flush(it) }
        TransactionSynchronizationManager.registerSynchronization(sync)
        return sync
    }

    private fun flush(sync: RealtimeTransactionSync) {
        runCatching {
            sync.orders().forEach { dispatchOrder(it) }
            sync.calendars().forEach { dispatchCalendar(it) }
        }.onFailure { log.error("Realtime flush failed", it) }
    }

    private companion object {
        const val CALENDAR_CHANGED = "CALENDAR_CHANGED"
    }
}
