package fatum.catalog.repository

import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CatalogItemRepository : JpaRepository<CatalogItem, String> {

    fun existsByTitleAndProviderIdIgnoreCase(title: String, providerId: String): Boolean

    fun existsByCategoryAndProviderIdIgnoreCase(category: Category, providerId: String): Boolean

    fun findByTitleIgnoreCase(title: String): List<CatalogItem>

    fun findByTitleContainingIgnoreCase(keyword: String): List<CatalogItem>

    fun findByTitleStartingWithIgnoreCase(prefix: String): List<CatalogItem>

    @Query("""
        SELECT s FROM CatalogItem s
        WHERE UPPER(s.title) LIKE UPPER(CONCAT('%', :keyword, '%'))
           OR UPPER(s.description) LIKE UPPER(CONCAT('%', :keyword, '%'))
    """)
    fun searchInCatalog(@Param("keyword") keyword: String): List<CatalogItem>
}