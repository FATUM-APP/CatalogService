package fatum.catalog.dto.request.order

import fatum.catalog.model.constant.OrderStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

data class OrderResponse(
    @field:NotBlank(message = "Order id is required")
    @field:Size(max = 36, message = "Order id must not exceed 36 characters")
    var orderId: String,

    @field:NotBlank(message = "Provider id is required")
    @field:Size(max = 36, message = "Provider id must not exceed 36 characters")
    var providerId: String,

    @field:NotNull(message = "Order status is required")
    val status: OrderStatus,

    @field:PositiveOrZero(message = "Estimated time cannot be negative")
    val estimatedTime: Int? = null,

    @field:Size(max=500, message = "Reason must not exceed 500 characters")
    var reason: String? = null
){
    init {
        orderId = orderId.trim()
        providerId = providerId.trim()
        reason = reason?.trim()
    }



}