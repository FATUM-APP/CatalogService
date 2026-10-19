package fatum.catalog.model;

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
import java.time.temporal.ChronoUnit;
import java.util.Objects;

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


    public ProviderAvailability(String providerId, Day day, LocalTime initialTime, LocalTime finalTime) {
        if (
                providerId == null ||providerId.isBlank()
                || day == null
        )
            throw new FatumServiceException(FatumServiceException.BLANK);
        this.providerId = providerId;
        this.day = day;
        initialTime = initialTime.withSecond(0).withNano(0);
        finalTime = finalTime.withSecond(0).withNano(0);
        validateTimes(initialTime, finalTime);
        this.initialTime = initialTime;
        this.finalTime = finalTime;
    }

    public ProviderAvailability(String providerId, Day day, LocalTime initialTime) {
        this(providerId, day, initialTime, LocalTime.of(23, 59));
    }

    public ProviderAvailability(String providerId, Day day) {
        this(providerId, day, LocalTime.of(0, 0), LocalTime.of(23, 59));
    }

    public void changeAvailabilityToAllDay() {
        this.initialTime = LocalTime.of(0, 0);
        this.finalTime = LocalTime.of(23, 59);
    }
    public void changeSchedule(LocalTime newInitialTime) {
        changeSchedule(newInitialTime.withSecond(0).withNano(0), this.finalTime);
    }

    public void changeSchedule(LocalTime newInitialTime, LocalTime newFinalTime) {
        validateTimes(newInitialTime, newFinalTime);
        this.initialTime = newInitialTime.withSecond(0).withNano(0);
        this.finalTime = newFinalTime.withSecond(0).withNano(0);
    }

    private void validateTimes(LocalTime start, LocalTime finalTime) {
        if (start.isAfter(finalTime)) {
            throw new FatumServiceException(FatumServiceException.INITIAL_AFTER_FINAL);
        }
    }

}
