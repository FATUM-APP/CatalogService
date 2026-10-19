package fatum.catalog.dto.mapper;

import fatum.catalog.dto.CreateCategoryRequest;
import fatum.catalog.model.Category;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static Category toEntity(CreateCategoryRequest categoryRequest, Category parent) {
        return new Category(categoryRequest.getName(), categoryRequest.getSchedulingModel(), null);
    }
}
