package fatum.catalog.dto.mapper

import fatum.catalog.dto.CreateServiceRequest
import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Category
import fatum.catalog.model.ServiceImage


fun CreateServiceRequest.toEntity(images: List<ServiceImage>, category: Category): CatalogItem {
    return CatalogItem(
        providerId = providerId.trim(),
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

