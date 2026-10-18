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
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
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
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "INITIAL_TIME", nullable = false)
    private LocalTime _initialTime;

    @Column(name = "FINAL_TIME", nullable = false)
    private LocalTime _finalTime;

    protected ProviderAvailability() {
    }

    public ProviderAvailability(String providerId, Day day, LocalTime initialTime, LocalTime finalTime) {
        this.providerId = providerId;
        this.day = day;
        if (providerId.isBlank()) {
            throw new FatumServiceException(FatumServiceException.BLANK);
        }
        validateTimes(initialTime, finalTime);
        _initialTime = initialTime.withSecond(0).withNano(0);
        _finalTime = finalTime.withSecond(0).withNano(0);
    }

    public ProviderAvailability(String providerId, Day day, LocalTime initialTime) {
        this(providerId, day, initialTime, LocalTime.of(23, 59));
    }

    public ProviderAvailability(String providerId, Day day) {
        this(providerId, day, LocalTime.of(0, 0), LocalTime.of(23, 59));
    }

    public String getProviderId() {
        return providerId;
    }

    public Day getDay() {
        return day;
    }

    public String getId() {
        return id;
    }

    protected void setId(String id) {
        this.id = id;
    }

    public LocalTime getInitialTime() {
        return _initialTime;
    }

    public LocalTime getFinalTime() {
        return _finalTime;
    }

    public long getDuration() {
        return ChronoUnit.MINUTES.between(_initialTime, _finalTime);
    }

    public void changeSchedule() {
        changeSchedule(_initialTime, _finalTime);
    }

    public void changeSchedule(LocalTime newInitialTime) {
        changeSchedule(newInitialTime, _finalTime);
    }

    public void changeSchedule(LocalTime newInitialTime, LocalTime newFinalTime) {
        validateTimes(newInitialTime, newFinalTime);
        _initialTime = newInitialTime.withSecond(0).withNano(0);
        _finalTime = newFinalTime.withSecond(0).withNano(0);
    }

    private void validateTimes(LocalTime start, LocalTime finalTime) {
        if (start.isAfter(finalTime)) {
            throw new FatumServiceException(FatumServiceException.INITIAL_AFTER_FINAL);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ProviderAvailability)) {
            return false;
        }
        if (this == other) {
            return true;
        }
        ProviderAvailability availability = (ProviderAvailability) other;
        return Objects.equals(this.id, availability.id);
    }

    @Override
    public int hashCode() {
        return this.id == null ? 0 : this.id.hashCode();
    }
}
