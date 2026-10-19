package fatum.catalog.service;

import fatum.catalog.dto.CreateCategoryRequest;
import fatum.catalog.dto.mapper.CategoryMapper;
import fatum.catalog.exception.CatalogErrorCode;
import fatum.catalog.exception.FatumServiceException;
import fatum.catalog.model.CatalogItem;
import fatum.catalog.model.Category;
import fatum.catalog.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Category create(CreateCategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new FatumServiceException(CatalogErrorCode.CATEGORY_EXISTS);
        }

        Category parent = request.parentName() != null ? getCategory(request.parentName()) : null;
        Category category = CategoryMapper.toEntity(request, parent);
        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public Category findCategory(String name) {
        return getCategory(name);
    }

    @Transactional(readOnly = true)
    public Set<CatalogItem> getCategoryServices(String category){
        return  getCategory(category).getServices();
    }

    @Transactional(readOnly = true)
    public Set<Category> getSubCategories(String category){
        return getCategory(category).getSubCategories();
    }

    @Transactional(readOnly = true)
    public List<Category> getRootCategories(){
        return categoryRepository.findByParentIsNull();
    }

    @Transactional
    public boolean disableCategory(String name) {
        Category category = getCategory(name);
        category.disable();
        categoryRepository.save(category);
        return category.isAble();
    }

    @Transactional
    public boolean enableCategory(String name) {
        Category category = getCategory(name);
        category.enable();
        categoryRepository.save(category);
        return category.isAble();
    }

    @Transactional
    public void deleteCategory(String name) {
        Category category = getCategory(name);
        if (category.hasServices()) {
            category.disable();
            categoryRepository.save(category);
        } else {
            categoryRepository.delete(category);
        }
    }


    private Category getCategory(String name) {
        return categoryRepository.findByNameIgnoreCase(name).orElseThrow(
                () -> new FatumServiceException(CatalogErrorCode.CATEGORY_NOT_FOUND)
        );
    }
}