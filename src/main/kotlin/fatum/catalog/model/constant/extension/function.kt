package fatum.catalog.model.constant.extension

import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.model.constant.AvailabilityReason

fun AvailabilityReason.toErrorCode(): CatalogErrorCode = when (this) {
    AvailabilityReason.OVERLAP -> CatalogErrorCode.OVERLAPPING_RANGE
    else -> CatalogErrorCode.PROVIDER_UNAVAILABLE
}