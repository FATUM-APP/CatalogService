package fatum.catalog.dto.mapper

import fatum.catalog.dto.request.schedule.ChangeAvailability
import fatum.catalog.dto.request.schedule.FullAvailability
import fatum.catalog.dto.response.schedule.Availability
import fatum.catalog.model.ProviderAvailability

fun ChangeAvailability.toEntity() =
    ProviderAvailability(
        providerId,
        day,
        start,
        end
    )

fun FullAvailability.toEntity() =
    ProviderAvailability(
        providerId,
        day
    )

fun ProviderAvailability.toResponse() =
    Availability(
        providerId = this.providerId,
        day = this.day.name,
        initialTime = this.initialTime,
        finalTime = this.finalTime
    )

