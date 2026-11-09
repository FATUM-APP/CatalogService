package fatum.catalog.dto.mapper

import fatum.catalog.client.M2User
import fatum.catalog.dto.request.item.CreateServiceRequest
import fatum.catalog.dto.response.item.ItemResponse
import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Category
import fatum.catalog.model.ServiceImage


fun CreateServiceRequest.toEntity(
    images: List<ServiceImage>,
    category: Category,
    provider: M2User
): CatalogItem =
    CatalogItem(
        providerId = provider.userId,
        providerName = provider.name,
        providerUsername = provider.username,
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


fun CatalogItem.toDto(): ItemResponse =
    ItemResponse(
        id = this.id,
        title = this.title,
        description = this.description,
        providerName = this.providerName,
        providerUsername = this.providerUsername,
        basePrice = this.basePrice.toPlainString(),
        priceType = this.priceType.name,
        isNegotiable = this.isNegotiable,
        defaultEstimatedTime = this.defaultEstimatedTime,
        jobType = this.jobType.name,
        category = if(this.category.parent != null) "${this.category.parent?.slug} / ${this.category.slug}"
        else this.category.name,
        fixedQuestions = this.fixedQuestions
    )

