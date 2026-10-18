package fatum.catalog.service;

import fatum.catalog.dto.CreateCategoryRequest;
import fatum.catalog.dto.mapper.CategoryMapper;
import fatum.catalog.exception.FatumServiceException;
import fatum.catalog.model.Category;
import fatum.catalog.repository.CategoryRepository;
import org.springframework.transaction.annotation.Transactional;

public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryRepository getCategoryRepository() {
        return categoryRepository;
    }

    @Transactional
    public Category createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsById(request.getName())) {
            throw new FatumServiceException(FatumServiceException.CATEGORY_EXISTS);
        }
        Category parent = categoryRepository.findById(request.getParentName())
                .orElseThrow(() -> new FatumServiceException(FatumServiceException.PARENT_CATEGORY_NOT_FOUND));
        Category category = CategoryMapper.toEntity(request, parent);
        return category;
    }
}
