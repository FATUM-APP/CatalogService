package fatum.catalog.model

import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "PROVIDER_UNAVAILABLE_DATES",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_unavailable_dates_provider_date",
            columnNames = ["PROVIDER_ID", "START_DATE"]
        )
    ]
)
class ProviderUnavailableRange(
    @Column(name = "PROVIDER_ID", nullable = false, length = 36)
    val providerId: String,

    @Column(name = "REASON")
    var reason: String? = null,

    startDate: LocalDateTime,
    finalDate: LocalDateTime
) {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    var id: String? = null
        protected set

    @Column(name = "START_DATE", nullable = false)
    lateinit var startDate: LocalDateTime
        protected  set

    @Column(name = "FINAL_DATE", nullable = false)
    lateinit var finalDate: LocalDateTime
        protected set



    init {
        if (providerId.isBlank()) throw FatumServiceException(CatalogErrorCode.BLANK)
        changeDates(startDate, finalDate)
    }


    constructor(providerId: String, start: LocalDate, end: LocalDate)
            : this(providerId, null,
        start.atStartOfDay(),
        end.atTime(23, 59))

    constructor(providerId: String, start: LocalDateTime, end: LocalDate)
            : this(providerId, null,
        start,
        end.atTime(23, 59))

    constructor(providerId: String, start: LocalDate, end: LocalDateTime)
            : this(providerId, null,
        start.atStartOfDay(),
        end)

    fun changeDates(
        newStartDate: LocalDateTime = this.startDate,
        newFinalDate: LocalDateTime = this.finalDate
    ) {
        validateDates(newStartDate, newFinalDate)
        this.startDate = newStartDate
        this.finalDate = newFinalDate
    }

    private fun validateDates(start: LocalDateTime, end: LocalDateTime) {
        if (start.isAfter(end)) {
            throw FatumServiceException(CatalogErrorCode.INITIAL_AFTER_FINAL)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ProviderUnavailableRange) return false
        if (this === other) return true
        return this.id == other.id
    }

    override fun hashCode(): Int = this.id?.hashCode() ?: 0
}
