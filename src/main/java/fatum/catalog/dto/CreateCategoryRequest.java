package fatum.catalog.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import fatum.catalog.model.constant.SchedulingModel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;

public final class CreateCategoryRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 30, message = "Name must not exceed 30 characters")
    private final String name;

    private final SchedulingModel schedulingModel;

    private final String parentName;

    @JsonCreator
    public CreateCategoryRequest(String name, SchedulingModel schedulingModel, String parentName) {
        this.name = Objects.requireNonNull(name, "name");
        this.schedulingModel = schedulingModel;
        this.parentName = parentName;
    }

    public CreateCategoryRequest(String name, SchedulingModel schedulingModel) {
        this(name, schedulingModel, null);
    }

    public CreateCategoryRequest(String name) {
        this(name, null, null);
    }

    public String getName() {
        return name;
    }

    public SchedulingModel getSchedulingModel() {
        return schedulingModel;
    }

    public String getParentName() {
        return parentName;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CreateCategoryRequest that)) {
            return false;
        }
        return name.equals(that.name)
                && Objects.equals(schedulingModel, that.schedulingModel)
                && Objects.equals(parentName, that.parentName);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (schedulingModel != null ? schedulingModel.hashCode() : 0);
        result = 31 * result + (parentName != null ? parentName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CreateCategoryRequest(name=" + name
                + ", schedulingModel=" + schedulingModel
                + ", parentName=" + parentName + ")";
    }
}
