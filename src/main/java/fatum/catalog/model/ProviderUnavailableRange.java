package fatum.catalog.model;

import fatum.catalog.exception.FatumServiceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
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

    @Column(name = "PROVIDER_ID", nullable = false, length = 36)
    private String providerId;

    @Column(name = "REASON")
    private String _reason;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "START_DATE", nullable = false)
    private LocalDateTime _startDate;

    @Column(name = "FINAL_DATE", nullable = false)
    private LocalDateTime _finalDate;

    protected ProviderUnavailableRange() {
    }

    public ProviderUnavailableRange(
            String providerId,
            String reason,
            LocalDateTime startDate,
            LocalDateTime finalDate
    ) {
        this.providerId = providerId;
        this._reason = reason;
        if (providerId.isBlank()) {
            throw new FatumServiceException(FatumServiceException.BLANK);
        }
        validateDates(startDate, finalDate);
        _startDate = startDate;
        _finalDate = finalDate;
    }

    public ProviderUnavailableRange(String providerId, LocalDateTime startDate, LocalDateTime finalDate) {
        this(providerId, null, startDate, finalDate);
    }

    public String getProviderId() {
        return providerId;
    }

    public String getId() {
        return id;
    }

    protected void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getStartDate() {
        return _startDate;
    }

    public LocalDateTime getFinalDate() {
        return _finalDate;
    }

    public String getReason() {
        return _reason;
    }

    public void changeDates() {
        changeDates(_startDate, _finalDate);
    }

    public void changeDates(LocalDateTime newStartDate) {
        changeDates(newStartDate, _finalDate);
    }

    public void changeDates(LocalDateTime newStartDate, LocalDateTime newFinalDate) {
        validateDates(newStartDate, newFinalDate);
        _startDate = newStartDate;
        _finalDate = newFinalDate;
    }

    public void changeReason(String newReason) {
        _reason = newReason;
    }

    private void validateDates(LocalDateTime start, LocalDateTime finalDate) {
        if (start.isAfter(finalDate)) {
            throw new FatumServiceException(FatumServiceException.INITIAL_AFTER_FINAL);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ProviderUnavailableRange)) {
            return false;
        }
        if (this == other) {
            return true;
        }
        ProviderUnavailableRange range = (ProviderUnavailableRange) other;
        return Objects.equals(this.id, range.id);
    }

    @Override
    public int hashCode() {
        return this.id == null ? 0 : this.id.hashCode();
    }
}
