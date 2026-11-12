package fatum.catalog.dto.response.item

import fatum.catalog.model.vo.FixedQuestion
import jakarta.validation.Valid

data class ItemResponse(
    val id: String?,
    val title: String,
    val status: String,
    val description: String?,
    val providerName: String,
    val providerUsername: String,
    val basePrice: String,
    val priceType: String,
    val isNegotiable: Boolean,
    val defaultEstimatedTime: Int?,
    val jobType: String,
    val category: String,
    val fixedQuestions : Set<FixedQuestion>
)