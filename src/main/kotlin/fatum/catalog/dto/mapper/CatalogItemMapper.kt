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
            _basePrice = request.basePrice,
            _priceType = request.priceType,
            _isNegotiable = request.isNegotiable,
            _defaultEstimatedTime = request.defaultEstimatedTime,
            _jobType = request.jobType,
            category = category,
            _serviceCatalog = images.toMutableList(),
            _fixedQuestions = request.fixedQuestions
        )
    }
}
