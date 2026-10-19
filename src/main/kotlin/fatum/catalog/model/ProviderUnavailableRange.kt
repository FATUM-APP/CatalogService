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
    private var _startDate: LocalDateTime = LocalDateTime.MIN

    @Column(name = "FINAL_DATE", nullable = false)
    private var _finalDate: LocalDateTime = LocalDateTime.MIN

    val startDate: LocalDateTime get() = _startDate
    val finalDate: LocalDateTime get() = _finalDate

    init {
        if (providerId.isBlank()) throw FatumServiceException(CatalogErrorCode.BLANK)
        changeDates(startDate, finalDate)
    }

    constructor(providerId: String, startDate: LocalDateTime, finalDate: LocalDateTime)
            : this(providerId, null, startDate, finalDate)

    constructor(providerId: String, reason: String?, start: LocalDate, end: LocalDate)
            : this(providerId, reason, start.atStartOfDay(), end.atTime(23, 59))

    constructor(providerId: String, start: LocalDate, end: LocalDate)
            : this(providerId, null, start.atStartOfDay(), end.atTime(23, 59))

    fun changeDates(
        newStartDate: LocalDateTime = _startDate,
        newFinalDate: LocalDateTime = _finalDate
    ) {
        validateDates(newStartDate, newFinalDate)
        _startDate = newStartDate
        _finalDate = newFinalDate
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
