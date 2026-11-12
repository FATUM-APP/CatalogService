package fatum.catalog.dto.mapper

import fatum.catalog.client.M2User
import fatum.catalog.dto.request.order.BookService
import fatum.catalog.dto.response.order.DualConfirmationResponse
import fatum.catalog.dto.response.order.OrderDTO
import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Order
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
