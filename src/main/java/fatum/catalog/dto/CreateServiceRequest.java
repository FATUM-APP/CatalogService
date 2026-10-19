package fatum.catalog.dto;

import fatum.catalog.model.constant.JobType;
import fatum.catalog.model.constant.PriceType;
import fatum.catalog.model.vo.FixedQuestion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;

public record CreateServiceRequest(
        @NotBlank(message = "Provider id is required")
        @Size(max = 36, message = "Provider id must not exceed 36 characters")
        String providerId,

        @NotBlank(message = "Title is required")
        @Size(max = 40, message = "Title must not exceed 40 characters")
        String title,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @PositiveOrZero(message = "Base price cannot be negative")
        BigDecimal basePrice,

        @NotNull(message = "Price type is required")
        PriceType priceType,

        boolean isNegotiable,

        Integer defaultEstimatedTime,

        JobType jobType,

        @NotBlank(message = "Category is required")
        @Size(max = 30, message = "Category must not exceed 30 characters")
        String category,

        @Valid
        Set<FixedQuestion> fixedQuestions
) {
    public CreateServiceRequest {
        if (basePrice == null) basePrice = BigDecimal.ZERO;
        if (jobType == null) jobType = JobType.PRESENCIAL;
        if (fixedQuestions == null) fixedQuestions = Set.of();
    }
}