package fatum.catalog.model

import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.vo.ProviderAvailabilityId
import jakarta.persistence.*
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.temporal.ChronoUnit


@Entity
@Table(name = "OWNER_AVAILABILITY")
class ProviderAvailability(

    @EmbeddedId
    val id: ProviderAvailabilityId,

    initialTime: LocalTime = LocalTime.of(0, 0),
    finalTime: LocalTime = LocalTime.of(23, 59)
) {

    // Accesores de conveniencia para no romper el resto del código
    val providerId: String get() = id.providerId
    val day: DayOfWeek get() = id.day

    @Column(name = "INITIAL_TIME", nullable = false)
    lateinit var initialTime: LocalTime
        protected set

    @Column(name = "FINAL_TIME", nullable = false)
    lateinit var finalTime: LocalTime
        protected set

    constructor(
        providerId: String,
        day: DayOfWeek,
        initialTime: LocalTime = LocalTime.of(0, 0),
        finalTime: LocalTime = LocalTime.of(23, 59)
    ) : this(ProviderAvailabilityId(providerId, day), initialTime, finalTime)

    init {
        if (id.providerId.isBlank()) throw FatumServiceException(CatalogErrorCode.BLANK)
        changeSchedule(initialTime, finalTime)
    }

    val duration: Long get() = ChronoUnit.MINUTES.between(
        this.initialTime, this.finalTime)

    fun changeAvailabilityToAllDay() {
        changeSchedule(LocalTime.of(0, 0), LocalTime.of(23, 59))
    }

    fun changeSchedule(
        newInitialTime: LocalTime = this.initialTime,
        newFinalTime: LocalTime = this.finalTime
    ) {
        validateTimes(newInitialTime, newFinalTime)
        this.initialTime = newInitialTime
        this.finalTime = newFinalTime
    }

    private fun validateTimes(start: LocalTime, end: LocalTime) {
        if (start.isAfter(end)) {
            throw FatumServiceException(CatalogErrorCode.INITIAL_AFTER_FINAL)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ProviderAvailability) return false
        if (this === other) return true
        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}