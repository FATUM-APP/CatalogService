package fatum.catalog.dto;

import fatum.catalog.model.constant.SchedulingModel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 30, message = "Name must not exceed 30 characters")
        String name,
        SchedulingModel schedulingModel,
        String parentName
) {}