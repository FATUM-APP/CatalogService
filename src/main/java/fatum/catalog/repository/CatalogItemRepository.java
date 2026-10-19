package fatum.catalog.repository;

import fatum.catalog.model.CatalogItem;
import fatum.catalog.model.Category;
import fatum.catalog.model.constant.PriceType;
import fatum.catalog.model.constant.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, String> {
    boolean existsByTittleAndProviderIdIgnoreCase(String tittle, String providerId);
    boolean existsByCategoryNameAndProviderIdIgnoreCase(String categoryName, String providerId);

    List<CatalogItem> findByTitleIgnoreCase(String title);
    List<CatalogItem> findByTitleIgnoreCaseAndStatus(String title, ServiceStatus status);


    List<CatalogItem> findByTitleContainingIgnoreCaseAndStatusActive(String keyword);
    List<CatalogItem> findByTitleStartingWithIgnoreCaseAndStatusActive(String prefix);


    List<CatalogItem> findByBasePriceBetweenAndStatusActive(BigDecimal minPrice, BigDecimal maxPrice);


    @Query("""
        SELECT s FROM CatalogItem s 
        WHERE UPPER(s.title) LIKE UPPER(CONCAT('%', :keyword, '%')) 
           OR UPPER(s.description) LIKE UPPER(CONCAT('%', :keyword, '%'))
    """)
    List<CatalogItem> searchInCatalog(@Param("keyword") String keyword);
}
