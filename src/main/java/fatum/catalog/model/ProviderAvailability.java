package fatum.catalog.model;

import fatum.catalog.exception.CatalogErrorCode;
import fatum.catalog.exception.FatumServiceException;
import fatum.catalog.model.constant.Day;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Table(
        name = "OWNER_AVAILABILITY",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_availability_provider_day",
                        columnNames = {"PROVIDER_ID", "DAY"}
                )
        }
)
public class ProviderAvailability {

    @Column(name = "PROVIDER_ID", nullable = false, length = 36)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "DAY", length = 10, nullable = false)
    private Day day;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "INITIAL_TIME", nullable = false)
    private LocalTime initialTime;

    @Column(name = "FINAL_TIME", nullable = false)
    private LocalTime finalTime;


    /**
     * Constructor of Provider Availability
     * @param providerId
     * @param day
     * @param initialTime
     * @param finalTime
     */
    public ProviderAvailability(String providerId, Day day, LocalTime initialTime, LocalTime finalTime) {
        if (
                providerId == null ||providerId.isBlank()
                || day == null
        )
            throw new FatumServiceException(CatalogErrorCode.BLANK);
        this.providerId = providerId;
        this.day = day;
        changeSchedule(initialTime,finalTime);
    }

    /**
     * Constructor that indicates just the begining of the day
     * @param providerId
     * @param day
     * @param initialTime
     */
    public ProviderAvailability(String providerId, Day day, LocalTime initialTime) {
        this(providerId, day, initialTime, LocalTime.of(23, 59));
    }

    /**
     * Full day Availablity
     * @param providerId
     * @param day
     */
    public ProviderAvailability(String providerId, Day day) {
        this(providerId, day, LocalTime.of(0, 0), LocalTime.of(23, 59));
    }

    public void changeAvailabilityToAllDay() {
        changeSchedule(LocalTime.of(0, 0),LocalTime.of(23, 59));
    }

    public void changeSchedule(LocalTime newInitialTime, LocalTime newFinalTime) {
        if(newInitialTime == null) newInitialTime = this.initialTime;
        if(newFinalTime == null) newFinalTime = this.finalTime;
        validateTimes(newInitialTime, newFinalTime);
        this.initialTime = newInitialTime;
        this.finalTime = newFinalTime;
    }

    private void validateTimes(LocalTime start, LocalTime finalTime) {
        if (start.isAfter(finalTime)) {
            throw new FatumServiceException(CatalogErrorCode.INITIAL_AFTER_FINAL);
        }
    }

}
