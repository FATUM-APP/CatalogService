package fatum.catalog.event.order


import fatum.catalog.model.constant.OrderStatus
import fatum.catalog.realtime.CalendarChange
import fatum.catalog.realtime.sse.SseHub
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.eventbridgev2.EventBridgeV2Client
import software.amazon.awssdk.services.eventbridgev2.model.PutEventsRequest
import software.amazon.awssdk.services.eventbridgev2.model.PutEventsRequestEntry
import software.amazon.awssdk.services.eventbridgev2.model.PutEventsResponse
import software.amazon.awssdk.services.eventbridgev2.model.PutEventsResultEntry
import tools.jackson.databind.ObjectMapper


@Service
class OrderEventPublisher(
    private val eventBridgeClient: EventBridgeV2Client,
    @Value("\${aws.eventbridge.bus-arn:}") private val eventBusArn: String,
    private val objectMapper: ObjectMapper,
    private val sseHub: SseHub
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun orderCreated(
        orderId : String,
        providerId: String,
        catalogItemTitle: String,
    ) {
        publish(
            OrderEventType.ORDER_CREATED
            , mapOf(
                "orderId" to orderId,
                "providerId" to providerId,
                "serviceTitle" to catalogItemTitle,
            )
        )
    }

    fun statusChanged(
        orderId : String,
        clientId: String,
        providerId: String,
        newStatus: OrderStatus,
        reason: String? = null
    ) {
        publish(
            OrderEventType.ORDER_STATUS_CHANGED,
            mapOf(
                "orderId" to orderId,
                "clientId" to clientId,
                "providerId" to providerId,
                "status" to newStatus,
                "reason" to reason
            )
        )
    }

    fun orderCompleted(
        orderId : String,
        serviceId: String,
        clientId: String,
        providerId: String,
        status: String,
    ) {
        publish(
            OrderEventType.ORDER_COMPLETED,
            mapOf(
                "orderId" to orderId,
                "serviceId" to serviceId,
                "clientId" to clientId,
                "providerId" to providerId,
                "status" to status,
            )
        )
    }

    fun confirmationChanged(
        orderId: String,
        clientId: String,
        providerId: String,
        confirmation: String,
        party: String,
        confirmed: Boolean,
        bothConfirmed: Boolean,
    ) {
        publish(
            OrderEventType.ORDER_CONFIRMATION_CHANGED,
            mapOf(
                "orderId" to orderId,
                "clientId" to clientId,
                "providerId" to providerId,
                "confirmation" to confirmation,
                "party" to party,
                "confirmed" to confirmed,
                "bothConfirmed" to bothConfirmed,
            )
        )
    }



    private fun publish(type: OrderEventType, payload: Map<String, Any?>) {
        val message = linkedMapOf<String, Any?>("type" to type.name).apply { putAll(payload) }

        // Primero al navegador: el usuario ve el cambio sin esperar a EventBridge.
        sseHub.publish(recipientsOf(payload).map(SseHub::userTopic), "order", message)

        // Una orden ocupa o libera un hueco: el calendario del proveedor cambia para todos.
        (payload["providerId"] as? String)?.let { providerId ->
            sseHub.publish(
                listOf(SseHub.calendarTopic(providerId)),
                "calendar",
                mapOf(
                    "type" to "CALENDAR_CHANGED",
                    "providerId" to providerId,
                    "change" to CalendarChange.ORDER_UPDATED.name
                )
            )
        }

        if (eventBusArn.isBlank()) {
            log.debug("aws.eventbridge.bus-arn vacío: no se publica {}", type)
            return
        }

        val entry = PutEventsRequestEntry.builder()
            .source(SOURCE)
            .detailType(type.name)
            .detail(serialize(payload))
            .build()

        val response = eventBridgeClient.putEvents(
            PutEventsRequest.builder()
                .eventBusArn(eventBusArn)
                .entries(entry)
                .build()
        )
        reportFailures(type, response)
    }


    /** Las dos partes de la orden. `ORDER_CREATED` solo trae al proveedor. */
    private fun recipientsOf(payload: Map<String, Any?>): List<String> = listOfNotNull(
        payload["clientId"] as? String,
        payload["providerId"] as? String
    )

    private fun reportFailures(type: OrderEventType, response: PutEventsResponse) {
        if (response.failedEntryCount() == null || response.failedEntryCount() == 0) return
        for (entry: PutEventsResultEntry in response.entries()) {
            if (entry.errorCode() != null) {
                log.error("El evento {} fue rechazado: {} {}", type, entry.errorCode(), entry.errorMessage())
            }
        }
    }

    private fun serialize(payload: Map<String, Any?>): String =
        runCatching { objectMapper.writeValueAsString(payload) }
            .getOrElse { throw IllegalStateException("No se pudo serializar el evento: ${payload.keys}", it) }

    companion object {
        const val SOURCE = "fatum.catalogservice"
    }
}
