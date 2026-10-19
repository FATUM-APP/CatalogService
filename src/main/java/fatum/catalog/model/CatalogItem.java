package fatum.catalog.model;

import fatum.catalog.exception.CatalogErrorCode;
import fatum.catalog.exception.FatumServiceException;
import fatum.catalog.model.constant.JobType;
import fatum.catalog.model.constant.PriceType;
import fatum.catalog.model.constant.ServiceStatus;
import fatum.catalog.model.vo.FixedQuestion;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Table(
        name = "SERVICES",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_services_provider_category",
                        columnNames = {"PROVIDER_ID", "CATEGORY"}
                ),
                @UniqueConstraint(
                        name = "uk_services_provider_title",
                        columnNames = {"PROVIDER_ID", "TITLE"}
                )
        }
)
public class CatalogItem {

    @Column(name = "PROVIDER_ID", length = 36, nullable = false)
    private String providerId;

    @Column(name = "TITLE", length = 40, nullable = false)
    private String title;

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT")
    private String description;

    @Column(name = "BASE_PRICE", nullable = false)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "PRICE_TYPE", length = 12, nullable = false)
    private PriceType priceType;

    @Column(name = "IS_NEGOTIABLE", nullable = false)
    private boolean isNegotiable;

    @Column(name = "DEFAULT_TIME")
    private Integer defaultEstimatedTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "JOB_TYPE", length = 10, nullable = false)
    private JobType jobType;

    @ManyToOne
    @JoinColumn(name = "CATEGORY", referencedColumnName = "NAME", nullable = false)
    private Category category;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "SERVICE_ID", nullable = false)
    private List<ServiceImage> serviceCatalog;

    @Column(name = "FIXED_QUESTIONS", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Set<FixedQuestion> fixedQuestions;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 12, nullable = false)
    private ServiceStatus status;

    public static class Builder {
        private String providerId;
        private String title;
        private String description;
        private BigDecimal basePrice;
        private PriceType priceType;
        private boolean isNegotiable;
        private Integer defaultEstimatedTime;
        private JobType jobType;
        private Category category;
        List<ServiceImage> serviceCatalog;
        Set<FixedQuestion> fixedQuestions;

        public Builder providerId(String providerId) { this.providerId = providerId; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder basePrice(BigDecimal basePrice) { this.basePrice = basePrice; return this; }
        public Builder priceType(PriceType priceType) { this.priceType = priceType; return this; }
        public Builder isNegotiable(boolean isNegotiable) { this.isNegotiable = isNegotiable; return this; }
        public Builder defaultEstimatedTime(Integer defaultEstimatedTime) {
            this.defaultEstimatedTime = defaultEstimatedTime; return this;
        }
        public Builder jobType(JobType jobType) { this.jobType = jobType; return this; }
        public Builder category(Category category) { this.category = category; return this; }
        public Builder serviceCatalog(List<ServiceImage> serviceCatalog) {
            this.serviceCatalog = serviceCatalog; return this;
        }
        public Builder fixedQuestions(Set<FixedQuestion> fixedQuestions) {
            this.fixedQuestions = fixedQuestions; return this;
        }

        public CatalogItem build() {
            return new CatalogItem(this);
        }
    }

    private CatalogItem(
            Builder builder
    ) {
        if(builder.providerId == null || builder.providerId.isBlank())
            throw new FatumServiceException(CatalogErrorCode.BLANK);
        if(builder.category == null) throw new FatumServiceException(CatalogErrorCode.SERVICE_WITHOUT_CATEGORY);
        this.providerId = builder.providerId;
        setTitle(builder.title);
        setDescription(builder.description);
        setPrice(builder.basePrice);
        setPriceType(builder.priceType);
        this.isNegotiable = builder.isNegotiable;
        setDefaultEstimatedTime(builder.defaultEstimatedTime);
        setJobType(builder.jobType);
        this.status = ServiceStatus.ACTIVE;
        this.category = builder.category;
        this.serviceCatalog = builder.serviceCatalog == null ? new ArrayList<>() : builder.serviceCatalog;
        setFixedQuestions(builder.fixedQuestions);
        category.addService(this);
    }

    public void setTitle(String newTitle) {
        if(newTitle == null || newTitle.isBlank())
            throw new FatumServiceException(CatalogErrorCode.BLANK);
        this.title = toTitle(newTitle);
    }

    public void setDescription(String newDescription) {
        this.description = newDescription == null ? null : newDescription.trim();
    }

    public void setPrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0)
            throw new FatumServiceException(CatalogErrorCode.NEGATIVE_PRICE);
        this.basePrice = newPrice;
    }

    public void setPriceType(PriceType newPriceType) {
        if(newPriceType == null) throw new FatumServiceException(CatalogErrorCode.BLANK);
        this.priceType = newPriceType;
    }

    public void makeNegotiable() {
        this.isNegotiable = true;
    }

    public void makeNonNegotiable() {
        this.isNegotiable = false;
    }

    public void setDefaultEstimatedTime(Integer newDefaultEstimatedTime) {
        if(newDefaultEstimatedTime != null && newDefaultEstimatedTime < 0)
            throw new FatumServiceException(CatalogErrorCode.NEGATIVE_ESTIMATED_TIME);
        this.defaultEstimatedTime = newDefaultEstimatedTime;
    }

    public void setStatus(ServiceStatus newStatus) {
        if(newStatus == null) throw new FatumServiceException(CatalogErrorCode.BLANK);
        this.status = newStatus;
    }

    public void setJobType(JobType newJobType) {
        if(newJobType == null) throw new FatumServiceException(CatalogErrorCode.BLANK);
        this.jobType = newJobType;
    }

    public void setFixedQuestions(Set<FixedQuestion> newQuestions) {
        if(newQuestions == null) clearFixedQuestions();
        else this.fixedQuestions = newQuestions;
    }

    public void clearFixedQuestions() {
        this.fixedQuestions = Collections.emptySet();
    }

    public void addImage(ServiceImage image) {
        this.serviceCatalog.add(image);
    }

    public void removeImage(ServiceImage image) {
        this.serviceCatalog.remove(image);
    }

    private String toTitle(String newTitle) {
        String[] words = newTitle.trim().split(" ", -1);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(' ');
            }
            String word = words[i].toLowerCase(Locale.ROOT);
            if (!word.isEmpty()) {
                word = word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
            }
            result.append(word);
        }
        return result.toString();
    }

}
