package fatum.catalog.model;

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
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
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
    private String _title;

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT", nullable = false)
    private String _description;

    @Column(name = "BASE_PRICE", nullable = false)
    private BigDecimal _basePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "PRICE_TYPE", length = 12, nullable = false)
    private PriceType _priceType;

    @Column(name = "IS_NEGOTIABLE", nullable = false)
    private boolean _isNegotiable;

    @Column(name = "DEFAULT_TIME")
    private Integer _defaultEstimatedTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "JOB_TYPE", length = 10, nullable = false)
    private JobType _jobType;

    @ManyToOne
    @JoinColumn(name = "CATEGORY", referencedColumnName = "NAME", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "SERVICE_ID", nullable = false)
    private List<ServiceImage> _serviceCatalog = new ArrayList<>();

    @Column(name = "FIXED_QUESTIONS", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Set<FixedQuestion> _fixedQuestions = Collections.emptySet();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", length = 36, nullable = false, updatable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 12, nullable = false)
    private ServiceStatus _status = ServiceStatus.ACTIVE;

    protected CatalogItem() {
    }

    public CatalogItem(
            String providerId,
            String title,
            String description,
            BigDecimal basePrice,
            PriceType priceType,
            boolean isNegotiable,
            Integer defaultEstimatedTime,
            JobType jobType,
            Category category,
            List<ServiceImage> serviceCatalog,
            Set<FixedQuestion> fixedQuestions
    ) {
        this.providerId = providerId;
        this._title = toTitle(title);
        this._description = description == null ? null : description.trim();
        this._basePrice = basePrice;
        this._priceType = priceType;
        this._isNegotiable = isNegotiable;
        this._defaultEstimatedTime = defaultEstimatedTime;
        this._jobType = jobType;
        this.category = category;
        this._serviceCatalog = serviceCatalog;
        this._fixedQuestions = fixedQuestions;

        if (providerId.isBlank()) {
            throw new FatumServiceException(FatumServiceException.BLANK);
        }
        if (_title.isBlank()) {
            throw new FatumServiceException(FatumServiceException.BLANK);
        }
        if (_basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new FatumServiceException(FatumServiceException.NEGATIVE_PRICE);
        }
        category.addService(this);
    }

    public CatalogItem(
            String providerId,
            String title,
            String description,
            BigDecimal basePrice,
            PriceType priceType,
            JobType jobType,
            Category category
    ) {
        this(providerId, title, description, basePrice, priceType, false, null, jobType, category,
                new ArrayList<>(), Collections.emptySet());
    }

    public CatalogItem(
            String providerId,
            String title,
            PriceType priceType,
            JobType jobType,
            Category category
    ) {
        this(providerId, title, null, BigDecimal.ZERO, priceType, false, null, jobType, category,
                new ArrayList<>(), Collections.emptySet());
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

    public String getTitle() {
        return _title;
    }

    public String getDescription() {
        return _description;
    }

    public BigDecimal getBasePrice() {
        return _basePrice;
    }

    public PriceType getPriceType() {
        return _priceType;
    }

    public boolean isNegotiable() {
        return _isNegotiable;
    }

    public Integer getDefaultEstimatedTime() {
        return _defaultEstimatedTime;
    }

    public ServiceStatus getStatus() {
        return _status;
    }

    public JobType getJobType() {
        return _jobType;
    }

    public List<ServiceImage> getServiceCatalog() {
        return new ArrayList<>(_serviceCatalog);
    }

    public Set<FixedQuestion> getFixedQuestions() {
        return _fixedQuestions;
    }

    public void changeTitle(String newTitle) {
        _title = toTitle(newTitle);
    }

    public void changeDescription() {
        changeDescription(null);
    }

    public void changeDescription(String newDescription) {
        _description = String.valueOf(newDescription == null ? null : newDescription.trim());
    }

    public void changePrice(BigDecimal newPrice) {
        if (newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new FatumServiceException(FatumServiceException.NEGATIVE_PRICE);
        }
        _basePrice = newPrice;
    }

    public void changePriceType(PriceType newPriceType) {
        _priceType = newPriceType;
    }

    public void changeNegotiable(boolean newNegotiable) {
        _isNegotiable = newNegotiable;
    }

    public void changeDefaultEstimatedTime(Integer newDefaultEstimatedTime) {
        _defaultEstimatedTime = newDefaultEstimatedTime;
    }

    public void changeStatus(ServiceStatus newStatus) {
        _status = newStatus;
    }

    public void changeJobType(JobType newJobType) {
        _jobType = newJobType;
    }

    public void updateFixedQuestions(Set<FixedQuestion> newQuestions) {
        _fixedQuestions = newQuestions;
    }

    public void clearFixedQuestions() {
        _fixedQuestions = Collections.emptySet();
    }

    public void addImage(ServiceImage image) {
        _serviceCatalog.add(image);
    }

    public ServiceImage removeImage(String imageId) {
        ServiceImage image = _serviceCatalog.stream()
                .filter(it -> Objects.equals(it.getId(), imageId))
                .findFirst()
                .orElse(null);
        if (image != null) {
            _serviceCatalog.remove(image);
        }
        return image;
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

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CatalogItem)) {
            return false;
        }
        if (this == other) {
            return true;
        }
        CatalogItem catalogItem = (CatalogItem) other;
        return Objects.equals(this.id, catalogItem.id);
    }

    @Override
    public int hashCode() {
        return this.id == null ? 0 : this.id.hashCode();
    }
}
