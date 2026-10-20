package fatum.catalog.dto.mapper

import fatum.catalog.dto.CreateServiceRequest
import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Category
import fatum.catalog.model.ServiceImage

object CatalogItemMapper {

    fun toEntity(request: CreateServiceRequest, images: List<ServiceImage>, category: Category): CatalogItem {
        return CatalogItem(
            providerId = request.providerId,
            title = request.title,
            description = request.description,
            basePrice = request.basePrice,
            priceType = request.priceType,
            isNegotiable = request.isNegotiable,
            defaultEstimatedTime = request.defaultEstimatedTime,
            jobType = request.jobType,
            category = category,
            serviceCatalog = images.toMutableList(),
            fixedQuestions = request.fixedQuestions
        )
    }
}
