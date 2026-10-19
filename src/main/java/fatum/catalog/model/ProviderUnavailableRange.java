package fatum.catalog.model;

import fatum.catalog.exception.CatalogErrorCode;
import fatum.catalog.exception.FatumServiceException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

import java.time.LocalDate;
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

    @Setter
    @Column(name = "REASON")
    private String reason;

    @Column(name = "START_DATE", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "FINAL_DATE", nullable = false)
    private LocalDateTime finalDate;


    /**
     * Constructor of a ProviderUnavalableRange
     * @param providerId, id of provider
     * @param reason, Why he/she in unable in that range
     * @param startDate, from
     * @param finalDate, to
     */
    public ProviderUnavailableRange(String providerId,
                                    String reason,
                                    LocalDateTime startDate,
                                    LocalDateTime finalDate) {
        if (
                providerId == null
                || providerId.isBlank()
                || startDate == null
                || finalDate == null
        )
            throw new FatumServiceException(CatalogErrorCode.BLANK);
        changeDates(startDate,finalDate);
        this.providerId = providerId;
        this.reason = reason;
    }

    /**
     * Contructor of a Unable range without reason
     * @param providerId
     * @param startDate
     * @param finalDate
     */
    public ProviderUnavailableRange(String providerId, LocalDateTime startDate, LocalDateTime finalDate) {
        this(providerId, null, startDate, finalDate);
    }

    /**
     * Contructor that determines hole day unable
     * @param providerId
     * @param reason
     * @param start
     * @param end
     */
    public ProviderUnavailableRange(String providerId, String reason, LocalDate start, LocalDate end) {
        LocalDateTime startWithTime = start.atStartOfDay();
        LocalDateTime endWithTime = end.atTime(23,59);
        this(providerId,reason,startWithTime,endWithTime);
    }

    /**
     * Contructor that determines hole day unable without reason
     * @param providerId
     * @param start
     * @param end
     */
    public ProviderUnavailableRange(String providerId, LocalDate start, LocalDate end) {
        LocalDateTime startWithTime = start.atStartOfDay();
        LocalDateTime endWithTime = end.atTime(23,59);
        this(providerId,null,startWithTime,endWithTime);
    }

    public void changeDates(LocalDateTime newStartDate, LocalDateTime newFinalDate) {
        if(newStartDate == null) newStartDate = this.startDate;
        if(newFinalDate == null) newFinalDate = this.finalDate;
        validateDates(newStartDate, newFinalDate);
        this.startDate = newStartDate;
        this.finalDate = newFinalDate;
    }

    private void validateDates(LocalDateTime start, LocalDateTime finalDate) {
        if (start.isAfter(finalDate)) {
            throw new FatumServiceException(CatalogErrorCode.INITIAL_AFTER_FINAL);
        }
    }

}
