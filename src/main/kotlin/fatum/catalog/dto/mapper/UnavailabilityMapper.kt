package fatum.catalog.dto.mapper

import fatum.catalog.dto.request.schedule.CreateFullUnavailability
import fatum.catalog.dto.request.schedule.CreateUnavailability
import fatum.catalog.dto.response.schedule.Unavailability
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

fun ProviderUnavailableRange.toResponse() =
    Unavailability(
        this.id,
        this.providerId,
        this.reason,
        this.startDate,
        this.finalDate
    )