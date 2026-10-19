package fatum.catalog.repository;

import fatum.catalog.model.CatalogItem;
import fatum.catalog.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, String> {
    boolean existsByTittleAndProviderIdIgnoreCase(String tittle, String providerId);
    boolean existsByCategoryIdAndProviderIdIgnoreCase(Category categoryId, String providerId);
    List<CatalogItem> findByTitleIgnoreCase(String title);
    List<CatalogItem> findByTitleContainingIgnoreCase(String keyword);
    List<CatalogItem> findByTitleStartingWithIgnoreCase(String prefix);
    @Query("""
        SELECT s FROM CatalogItem s 
        WHERE UPPER(s.title) LIKE UPPER(CONCAT('%', :keyword, '%')) 
           OR UPPER(s.description) LIKE UPPER(CONCAT('%', :keyword, '%'))
    """)
    List<CatalogItem> searchInCatalog(@Param("keyword") String keyword);
}
