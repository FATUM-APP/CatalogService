package fatum.catalog.event.listener

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.JsonNode
import fatum.catalog.event.EventBridgeEnvelope
import fatum.catalog.service.ProviderAvailabilityService
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class UserEventListener(
    private val objectMapper: ObjectMapper,
    private val providerAvailabilityService: ProviderAvailabilityService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @SqsListener("\${fatum.sqs.catalog-events-queue-name}")
    fun handleUserEvent(message: String) {
        val envelope = objectMapper.readValue(message, EventBridgeEnvelope::class.java)

        when (envelope.detailType) {
            "USER_BECAME_PROFESSIONAL" -> handleBecameProfessional(envelope.detail)
            "USER_ACTIVE_STATUS_CHANGED" -> handleActiveStatusChanged(envelope.detail)
            else -> logger.warn("Unhandled event type: ${envelope.detailType}")
        }
    }

    private fun handleBecameProfessional(detail: JsonNode) {
        val userId = detail.get("userId").asString()
        val userEmail = detail.get("userEmail").asString()
        val userRole = detail.get("userRole").asString()
        logger.info("Received BECAME_PROFESSIONAL: userId=$userId, userEmail=$userEmail, userRole=$userRole")
        providerAvailabilityService.initialize(userId)
    }

    private fun handleActiveStatusChanged(detail: JsonNode) {
        val userId = detail.get("userId").asString()
        val userEmail = detail.get("userEmail").asString()
        val userRole = detail.get("userRole").asString()
        val isActive = detail.get("isActive").asBoolean()
        logger.info("Received ACTIVE_STATUS_CHANGED: userId=$userId, isActive=$isActive, userRole=$userRole")
        // TODO: implement catalog-side logic later
    }
}