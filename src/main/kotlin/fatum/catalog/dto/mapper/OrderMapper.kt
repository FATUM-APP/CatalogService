package fatum.catalog.dto.mapper

import fatum.catalog.client.M2User
import fatum.catalog.dto.request.order.BookService
import fatum.catalog.dto.response.order.DualConfirmationResponse
import fatum.catalog.dto.response.order.OrderDTO
import fatum.catalog.event.notification.order.OrderCancelledNotify
import fatum.catalog.event.notification.order.OrderConfirmationNotify
import fatum.catalog.event.notification.order.OrderCreatedNotify
import fatum.catalog.event.notification.order.OrderStatusChangedNotify
import fatum.catalog.event.payload.order.OrderCancelled
import fatum.catalog.event.payload.order.OrderConfirmationUpdated
import fatum.catalog.event.payload.order.OrderCreated
import fatum.catalog.event.payload.order.OrderStatusChanged
import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Order
import fatum.catalog.model.constant.ConfirmingParty
import fatum.catalog.model.vo.DualConfirmation

fun BookService.toEntity(
    catalogItem: CatalogItem,
    customer: M2User
) : Order{
    return Order(
        clientId = customer.userId,
        clientName = customer.name,
        clientUsername = customer.username,
        booking = booking,
        catalogItem = catalogItem,
        negotiatedPrice = negotiatedPrice ?: catalogItem.basePrice,
        deliveryCity = customer.principalAddress.city,
        deliveryState = customer.principalAddress.state,
        deliveryCountry = customer.principalAddress.country,
        deliveryResidence = customer.principalAddress.residence,
        answers = answers,
        requirements = requirements
    )
}

fun DualConfirmation.toResponse() : DualConfirmationResponse =
    DualConfirmationResponse(
        byCustomer = this.byCustomer,
        byProvider = this.byProvider
    )

fun Order.toResponse() : OrderDTO =
    OrderDTO(
        id = this.id,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        clientId = this.clientId,
        clientName = this.clientName,
        clientUsername = this.clientUsername,
        providerId = this.catalogItem.providerId,
        providerName = this.catalogItem.providerName,
        providerUsername = this.catalogItem.providerUsername,
        catalogItemId = this.catalogItem.id,
        booking = this.booking,
        negotiatedPrice = this.negotiatedPrice.toPlainString(),
        estimatedTime = this.estimatedTime,
        deliveryCity = this.deliveryCity,
        deliveryState = this.deliveryState,
        deliveryCountry = this.deliveryCountry,
        deliveryResidence = this.deliveryResidence,
        answers = this.answers,
        requirements = this.requirements,
        deliveryStartedAt = this.deliveryStartedAt,
        startConfirmation = this.startConfirmation.toResponse(),
        completionConfirmation = this.completionConfirmation.toResponse(),
        paymentConfirmation = this.paymentConfirmation.toResponse(),
        reason = this.reason,
        status = this.status.name
    )

// ======================== Event mappers ========================

fun Order.toCreatedEvent() =
    OrderCreated(
        providerId = this.catalogItem.providerId,
        clientUsername = this.clientUsername,
        orderId = this.id!!,
        serviceId = this.catalogItem.id!!
    )

fun OrderCreated.toNotification() =
    OrderCreatedNotify(
        orderId = this.orderId,
        serviceId = this.serviceId,
        clientUsername = this.clientUsername
    )

fun Order.toStatusChangedEvent() =
    OrderStatusChanged(
        orderId = this.id!!,
        status = this.status.name,
        estimatedTime = this.estimatedTime,
        providerId = this.catalogItem.providerId,
        clientId = this.clientId,
        booking = this.booking
    )

fun OrderStatusChanged.toNotification() =
    OrderStatusChangedNotify(
        orderId = this.orderId,
        status = this.status,
        estimatedTime = this.estimatedTime,
        booking = this.booking
    )

fun Order.toOrderCancelled() =
    OrderCancelled(
        orderId = this.id!!,
        reason = this.reason!!,
        providerId = this.catalogItem.providerId,
        clientId = this.clientId
    )

fun OrderCancelled.toNotification() =
    OrderCancelledNotify(
        orderId = this.orderId,
        reason = this.reason
    )

fun Order.toConfirmationEvent(phase: String, party: ConfirmingParty, action: String, statusChanged: Boolean) =
    OrderConfirmationUpdated(
        orderId = this.id!!,
        phase = phase,
        party = party.name,
        action = action,
        statusChanged = statusChanged,
        newStatus = if (statusChanged) this.status.name else null,
        providerId = this.providerId,
        clientId = this.clientId
    )

fun OrderConfirmationUpdated.toNotification() =
    OrderConfirmationNotify(
        orderId = this.orderId,
        phase = this.phase,
        party = this.party,
        action = this.action,
        statusChanged = this.statusChanged,
        newStatus = this.newStatus
    )