package fatum.catalog.model

import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.constant.Day
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(
    name = "OWNER_AVAILABILITY",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_availability_provider_day",
            columnNames = ["PROVIDER_ID", "DAY"]
        )
    ]
)
class ProviderAvailability(
    @Column(name = "PROVIDER_ID", nullable = false, length = 36)
    val providerId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "DAY", length = 10, nullable = false)
    val day: Day,

    initialTime: LocalTime = LocalTime.of(0, 0),

    finalTime: LocalTime = LocalTime.of(23, 59)
) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    var id: String? = null
        protected set

    @Column(name = "INITIAL_TIME", nullable = false)
    private var _initialTime: LocalTime

    @Column(name = "FINAL_TIME", nullable = false)
    private var _finalTime: LocalTime

    init {
        if(providerId.isBlank()) throw FatumServiceException(FatumServiceException.BLANK)
        validateTimes(initialTime, finalTime)
        _initialTime = initialTime.withSecond(0).withNano(0)
        _finalTime = finalTime.withSecond(0).withNano(0)
    }

    val initialTime: LocalTime get() = _initialTime
    val finalTime: LocalTime get() = _finalTime
    val duration: Long get() = ChronoUnit.MINUTES.between(
        _initialTime,
        _finalTime
    )

    fun changeSchedule(newInitialTime: LocalTime = _initialTime,
                       newFinalTime: LocalTime = _finalTime) {

       validateTimes(newInitialTime, newFinalTime)
        _initialTime = newInitialTime.withSecond(0).withNano(0)
        _finalTime = newFinalTime.withSecond(0).withNano(0)
    }

    private fun validateTimes(start: LocalTime, final: LocalTime) {
        if (start.isAfter(final)) {
            throw FatumServiceException(FatumServiceException.INITIAL_AFTER_FINAL)
        }
    }


    override fun equals(other: Any?): Boolean {
        if (other !is ProviderAvailability) return false
        if (this === other) return true
        return this.id == other.id
    }


    override fun hashCode(): Int = this.id?.hashCode() ?: 0


}