package fatum.catalog.dto.mapper

import fatum.catalog.dto.request.CreateFullUnavailability
import fatum.catalog.dto.request.CreateUnavailability
import fatum.catalog.model.ProviderUnavailableRange

fun CreateUnavailability.toEntity(): ProviderUnavailableRange {
    return ProviderUnavailableRange(
        providerId.trim(),
        reason?.trim(),
        start,
        end)
}

fun CreateFullUnavailability.toEntity(): ProviderUnavailableRange {
    return ProviderUnavailableRange(
        providerId.trim(),
        reason?.trim(),
        start,
        end
    )
}