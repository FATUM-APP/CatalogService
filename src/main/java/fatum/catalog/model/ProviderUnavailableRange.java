package fatum.catalog.model;

import fatum.catalog.exception.FatumServiceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Table(
        name = "PROVIDER_UNAVAILABLE_DATES",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_unavailable_dates_provider_date",
                        columnNames = {"PROVIDER_ID", "START_DATE"}
                )
        }
)
public class ProviderUnavailableRange {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "PROVIDER_ID", nullable = false, length = 36)
    private String providerId;

    @Column(name = "REASON")
    private String reason;

    @Column(name = "START_DATE", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "FINAL_DATE", nullable = false)
    private LocalDateTime finalDate;


    public ProviderUnavailableRange(String providerId,String reason, LocalDateTime startDate, LocalDateTime finalDate) {
        if (
                providerId == null
                || providerId.isBlank()
                || startDate == null
                || finalDate == null
        )
            throw new FatumServiceException(FatumServiceException.BLANK);

        startDate = startDate.withSecond(0).withNano(0);
        finalDate = finalDate.withSecond(0).withNano(0);
        validateDates(startDate, finalDate);
        this.providerId = providerId;
        this.reason = reason;
        this.startDate = startDate;
        this.finalDate = finalDate;
    }

    public ProviderUnavailableRange(String providerId, LocalDateTime startDate, LocalDateTime finalDate) {
        this(providerId, null, startDate, finalDate);
    }


    public void changeDates(LocalDateTime newStartDate, LocalDateTime newFinalDate) {
        if(newStartDate == null) newStartDate = this.startDate;
        if(newFinalDate == null) newFinalDate = this.finalDate;
        validateDates(newStartDate, newFinalDate);
        this.startDate = newStartDate;
        this.finalDate = newFinalDate;
    }

    public void changeReason(String newReason) {
        this.reason = newReason;
    }

    private void validateDates(LocalDateTime start, LocalDateTime finalDate) {
        if (start.isAfter(finalDate)) {
            throw new FatumServiceException(FatumServiceException.INITIAL_AFTER_FINAL);
        }
    }

}
