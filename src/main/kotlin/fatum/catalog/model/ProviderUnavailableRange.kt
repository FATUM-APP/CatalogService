package fatum.catalog.model

import fatum.catalog.exception.FatumServiceException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "PROVIDER_UNAVAILABLE_DATES",

    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_unavailable_dates_provider_date",
            columnNames = ["PROVIDER_ID", "START_DATE"]
        )
    ])
class ProviderUnavailableRange(
    @Column(name = "PROVIDER_ID", nullable = false, length = 36)
    val providerId: String,

    @Column(name = "REASON")
    private var _reason: String? = null,

    startDate: LocalDateTime,
    finalDate: LocalDateTime
) {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    var id: String? = null
        protected set

    @Column(name = "START_DATE", nullable = false)
    private var _startDate: LocalDateTime

    @Column(name = "FINAL_DATE", nullable = false)
    private var _finalDate: LocalDateTime

    val startDate: LocalDateTime get() = _startDate
    val finalDate: LocalDateTime get() = _finalDate
    val reason: String? get() = _reason

    init {
        if(providerId.isBlank()) throw FatumServiceException(FatumServiceException.BLANK)
        validateDates(startDate, finalDate)
        _startDate = startDate
        _finalDate = finalDate
    }

    fun changeDates(newStartDate: LocalDateTime = _startDate, newFinalDate: LocalDateTime = _finalDate) {
        validateDates(newStartDate, newFinalDate)
        _startDate = newStartDate
        _finalDate = newFinalDate
    }

    fun changeReason(newReason: String?) {
        _reason = newReason
    }

    private fun validateDates(start: LocalDateTime, final: LocalDateTime) {
        if (start.isAfter(final)) {
            throw FatumServiceException(FatumServiceException.INITIAL_AFTER_FINAL)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ProviderUnavailableRange) return false
        if (this === other) return true
        return this.id == other.id
    }


    override fun hashCode(): Int = this.id?.hashCode() ?: 0


}