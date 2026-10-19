package fatum.catalog.dto.mapper;

import fatum.catalog.dto.CreateCategoryRequest;
import fatum.catalog.model.Category;

public class CategoryMapper {

    private CategoryMapper() {}

    public static Category toEntity(CreateCategoryRequest request, Category parent) {
        if (parent != null) {
            return new Category(request.name(), parent);
        }
        return new Category(request.name(), request.schedulingModel());
    }
}
