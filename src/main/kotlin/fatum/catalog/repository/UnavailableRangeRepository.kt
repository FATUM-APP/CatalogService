package fatum.catalog.repository

import fatum.catalog.model.ProviderUnavailableRange
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface UnavailableRangeRepository: JpaRepository<ProviderUnavailableRange, String> {
    fun findByProviderId(providerId: String): List<ProviderUnavailableRange>
    fun findByProviderIdAndStartDateBetween(
        providerId: String,
        from: LocalDateTime,
        to: LocalDateTime
    ): List<ProviderUnavailableRange>
}