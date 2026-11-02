package fatum.catalog.dto.event

import fatum.catalog.model.constant.OrderStatus

data class orderStatusChangedEvent(
    val orderId: String,
    val orderStatus: OrderStatus,
    )