package fatum.catalog.service;

import fatum.catalog.dto.CreateCategoryRequest;
import fatum.catalog.dto.mapper.CategoryMapper;
import fatum.catalog.exception.FatumServiceException;
import fatum.catalog.model.Category;
import fatum.catalog.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Category create(CreateCategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name()))
            throw new FatumServiceException(FatumServiceException.CATEGORY_EXISTS);

        Category parent = request.parentName() != null ? findCategory(request.parentName()) : null;
        Category category = CategoryMapper.toEntity(request, parent);
        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public Category findCategory(String name){
        return categoryRepository.findByNameIgnoreCase(name).orElseThrow(
                () -> new FatumServiceException(FatumServiceException.CATEGORY_NOT_FOUND)
        );
    }

    @Transactional
    public boolean disableCategory(String name){
        Category category = findCategory(name);
        category.disable();
        categoryRepository.save(category);
        return category.isAble();
    }

    @Transactional
    public boolean enableCategory(String name){
        Category category = findCategory(name);
        category.enable();
        categoryRepository.save(category);
        return category.isAble();
    }

    @Transactional
    public void deleteCategory(String name){
        Category category = findCategory(name);
        if(category.hasServices()) disableCategory(name);
        else categoryRepository.delete(category);
    }
}
