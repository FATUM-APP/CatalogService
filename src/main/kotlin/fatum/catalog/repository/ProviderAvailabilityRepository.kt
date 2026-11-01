package fatum.catalog.repository

import fatum.catalog.model.ProviderAvailability
import fatum.catalog.model.vo.ProviderAvailabilityId
import org.springframework.data.jpa.repository.JpaRepository
import java.time.DayOfWeek

interface ProviderAvailabilityRepository: JpaRepository<ProviderAvailability, ProviderAvailabilityId> {
    fun findByIdProviderIdAndIdDay(providerId: String, day: DayOfWeek): ProviderAvailability?

    fun findByIdProviderId(providerId: String): List<ProviderAvailability>
}