package fatum.catalog.repository;

import fatum.catalog.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, String> {
    boolean existsByNameIgnoreCase(String name);

    Optional<Category> findByNameIgnoreCase(String name);
    Optional<Category> findByNameIgnoreCaseAndIsAbleTrue(String name);

    List<Category> findByParentIsNull();
    List<Category> findByParentIsNullAndIsAbleTrue();
}
