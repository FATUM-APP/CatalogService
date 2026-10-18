package fatum.catalog.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import fatum.catalog.model.constant.JobType;
import fatum.catalog.model.constant.PriceType;
import fatum.catalog.model.vo.FixedQuestion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public final class CreateServiceRequest {

    @NotBlank(message = "Provider id is required")
    @Size(max = 36, message = "Provider id must not exceed 36 characters")
    private final String providerId;

    @NotBlank(message = "Title is required")
    @Size(max = 40, message = "Title must not exceed 40 characters")
    private final String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private final String description;

    @PositiveOrZero(message = "Base price cannot be negative")
    private final BigDecimal basePrice;

    @NotNull(message = "Price type is required")
    private final PriceType priceType;

    private final boolean isNegotiable;

    private final Integer defaultEstimatedTime;

    private final JobType jobType;

    @NotBlank(message = "Category is required")
    @Size(max = 30, message = "Category must not exceed 30 characters")
    private final String category;

    @Valid
    private final Set<FixedQuestion> fixedQuestions;

    @JsonCreator
    public CreateServiceRequest(
            String providerId,
            String title,
            String description,
            BigDecimal basePrice,
            PriceType priceType,
            @JsonSetter(nulls = Nulls.AS_EMPTY) boolean isNegotiable,
            Integer defaultEstimatedTime,
            JobType jobType,
            String category,
            Set<FixedQuestion> fixedQuestions
    ) {
        this.providerId = Objects.requireNonNull(providerId, "providerId");
        this.title = Objects.requireNonNull(title, "title");
        this.description = description;
        this.basePrice = basePrice == null ? BigDecimal.ZERO : basePrice;
        this.priceType = Objects.requireNonNull(priceType, "priceType");
        this.isNegotiable = isNegotiable;
        this.defaultEstimatedTime = defaultEstimatedTime;
        this.jobType = jobType == null ? JobType.PRESENCIAL : jobType;
        this.category = Objects.requireNonNull(category, "category");
        this.fixedQuestions = fixedQuestions == null ? Collections.emptySet() : fixedQuestions;
    }

    public CreateServiceRequest(
            String providerId,
            String title,
            String description,
            BigDecimal basePrice,
            PriceType priceType,
            boolean isNegotiable,
            Integer defaultEstimatedTime,
            JobType jobType,
            String category
    ) {
        this(providerId, title, description, basePrice, priceType, isNegotiable,
                defaultEstimatedTime, jobType, category, Collections.emptySet());
    }

    public CreateServiceRequest(
            String providerId,
            String title,
            String description,
            BigDecimal basePrice,
            PriceType priceType,
            String category
    ) {
        this(providerId, title, description, basePrice, priceType, false,
                null, JobType.PRESENCIAL, category, Collections.emptySet());
    }

    public CreateServiceRequest(
            String providerId,
            String title,
            BigDecimal basePrice,
            PriceType priceType,
            String category
    ) {
        this(providerId, title, null, basePrice, priceType, false,
                null, JobType.PRESENCIAL, category, Collections.emptySet());
    }

    public CreateServiceRequest(
            String providerId,
            String title,
            PriceType priceType,
            String category
    ) {
        this(providerId, title, null, BigDecimal.ZERO, priceType, false,
                null, JobType.PRESENCIAL, category, Collections.emptySet());
    }

    public String getProviderId() {
        return providerId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public PriceType getPriceType() {
        return priceType;
    }

    public boolean isNegotiable() {
        return isNegotiable;
    }

    public Integer getDefaultEstimatedTime() {
        return defaultEstimatedTime;
    }

    public JobType getJobType() {
        return jobType;
    }

    public String getCategory() {
        return category;
    }

    public Set<FixedQuestion> getFixedQuestions() {
        return fixedQuestions;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CreateServiceRequest that)) {
            return false;
        }
        return isNegotiable == that.isNegotiable
                && providerId.equals(that.providerId)
                && title.equals(that.title)
                && Objects.equals(description, that.description)
                && basePrice.equals(that.basePrice)
                && priceType.equals(that.priceType)
                && Objects.equals(defaultEstimatedTime, that.defaultEstimatedTime)
                && jobType.equals(that.jobType)
                && category.equals(that.category)
                && fixedQuestions.equals(that.fixedQuestions);
    }

    @Override
    public int hashCode() {
        int result = providerId.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + basePrice.hashCode();
        result = 31 * result + priceType.hashCode();
        result = 31 * result + Boolean.hashCode(isNegotiable);
        result = 31 * result + (defaultEstimatedTime != null ? defaultEstimatedTime.hashCode() : 0);
        result = 31 * result + jobType.hashCode();
        result = 31 * result + category.hashCode();
        result = 31 * result + fixedQuestions.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "CreateServiceRequest(providerId=" + providerId
                + ", title=" + title
                + ", description=" + description
                + ", basePrice=" + basePrice
                + ", priceType=" + priceType
                + ", isNegotiable=" + isNegotiable
                + ", defaultEstimatedTime=" + defaultEstimatedTime
                + ", jobType=" + jobType
                + ", category=" + category
                + ", fixedQuestions=" + fixedQuestions + ")";
    }
}
