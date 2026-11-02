package fatum.catalog.dto.mapper

import fatum.catalog.client.M2User
import fatum.catalog.dto.request.CreateServiceRequest
import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Category
import fatum.catalog.model.ServiceImage


fun CreateServiceRequest.toEntity(
    images: List<ServiceImage>,
    category: Category,
    provider: M2User
): CatalogItem {
    return CatalogItem(
        providerId = provider.userId,
        providerName = provider.name,
        providerUsername = provider.userName,
        title = title.trim(),
        description = description?.trim(),
        basePrice = basePrice,
        priceType = priceType,
        isNegotiable = isNegotiable,
        defaultEstimatedTime = defaultEstimatedTime,
        jobType = jobType,
        category = category,
        serviceCatalog = images.toMutableList(),
        fixedQuestions = fixedQuestions
    )
}

