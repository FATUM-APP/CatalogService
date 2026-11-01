package fatum.catalog.repository

import fatum.catalog.model.ProviderAvailability
import fatum.catalog.model.vo.ProviderAvailabilityId
import fatum.catalog.model.constant.Day
import org.springframework.data.jpa.repository.JpaRepository

interface ProviderAvailabilityRepository: JpaRepository<ProviderAvailability, ProviderAvailabilityId> {
    fun findByIdProviderIdAndIdDay(providerId: String, day: Day): ProviderAvailability?

    fun findByIdProviderId(providerId: String): List<ProviderAvailability>
}