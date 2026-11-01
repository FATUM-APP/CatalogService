package fatum.catalog.model.vo

import fatum.catalog.model.constant.Day
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
data class ProviderAvailabilityId(

    @Column(name = "PROVIDER_ID", nullable = false, length = 36)
    val providerId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "DAY", length = 10, nullable = false)
    val day: Day
)
