package fatum.catalog.dto.mapper

import fatum.catalog.client.M2User
import fatum.catalog.dto.request.BookService
import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Order

fun BookService.toEntity(
    catalogItem: CatalogItem,
    customer: M2User
) : Order{
    return Order(
        clientId = customer.userId,
        clientName = customer.name,
        clientUsername = customer.userName,
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