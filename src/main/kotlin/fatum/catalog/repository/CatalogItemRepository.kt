package fatum.catalog.repository

import fatum.catalog.model.CatalogItem
import fatum.catalog.model.constant.PriceType
import fatum.catalog.model.constant.ServiceStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal

interface CatalogItemRepository : JpaRepository<CatalogItem, String> {

    fun existsByTitleAndProviderIdIgnoreCase(title: String, providerId: String): Boolean

    fun existsByCategoryNameAndProviderIdIgnoreCase(categoryName: String, providerId: String): Boolean

    fun findByTitleIgnoreCase(title: String): List<CatalogItem>

    fun findByTitleIgnoreCaseAndStatus(title: String, status: ServiceStatus): List<CatalogItem>

    fun findByTitleContainingIgnoreCase(keyword: String): List<CatalogItem>

    fun findByTitleStartingWithIgnoreCase(prefix: String): List<CatalogItem>

    fun findByBasePriceBetween(minPrice: BigDecimal, maxPrice: BigDecimal): List<CatalogItem>

    fun findByPriceType(priceType: PriceType): List<CatalogItem>

    @Query("""
        SELECT s FROM CatalogItem s
        WHERE UPPER(s.title) LIKE UPPER(CONCAT('%', :keyword, '%'))
           OR UPPER(s.description) LIKE UPPER(CONCAT('%', :keyword, '%'))
    """)
    fun searchInCatalog(@Param("keyword") keyword: String): List<CatalogItem>
}
