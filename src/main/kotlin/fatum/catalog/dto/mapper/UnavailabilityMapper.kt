package fatum.catalog.dto.mapper

import fatum.catalog.dto.CreateFullUnavailability
import fatum.catalog.dto.CreateUnavailability
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