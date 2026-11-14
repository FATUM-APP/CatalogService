package fatum.catalog.dto.response.order

import fatum.catalog.model.vo.QuestionAnswer
import java.time.LocalDateTime

data class OrderDTO(
    val id: String?,
    val serviceTitle: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val clientId: String,
    val clientName: String,
    val clientUsername: String,
    val providerId: String,
    val providerName: String,
    val providerUsername: String,
    val catalogItemId: String?,
    val booking: LocalDateTime,
    val negotiatedPrice: String,
    val estimatedTime: Int?,
    val deliveryCity: String,
    val deliveryState: String,
    val deliveryCountry: String,
    val deliveryResidence: String,
    val answers : Set<QuestionAnswer>,
    val requirements: String? = null,
    val deliveryStartedAt: LocalDateTime? = null,
    val startConfirmation: DualConfirmationResponse,
    val completionConfirmation: DualConfirmationResponse,
    val paymentConfirmation: DualConfirmationResponse,
    val reason: String? = null,
    val status: String,
)
