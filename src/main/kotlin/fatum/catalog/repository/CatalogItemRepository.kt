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

    @Query("""
        SELECT s FROM CatalogItem s
        WHERE s.id = :id
          AND (:status IS NULL OR s.status = :status)
          AND (:categoryIsAble IS NULL OR s.category.isAble = :categoryIsAble)
    """)
    fun findByIdFiltered(
        @Param("id") id: String,
        @Param("status") status: ServiceStatus?,
        @Param("categoryIsAble") categoryIsAble: Boolean?
    ): CatalogItem?


    @Query("""
        SELECT s FROM CatalogItem s
        WHERE UPPER(s.title) = UPPER(:title)
          AND (:status IS NULL OR s.status = :status)
          AND (:categoryIsAble IS NULL OR s.category.isAble = :categoryIsAble)
    """)
    fun findByTitle(
        @Param("title") title: String,
        @Param("status") status: ServiceStatus?,
        @Param("categoryIsAble") categoryIsAble: Boolean?
    ): List<CatalogItem>

    @Query("""
        SELECT s FROM CatalogItem s
        WHERE UPPER(s.title) LIKE UPPER(CONCAT('%', :keyword, '%'))
          AND (:status IS NULL OR s.status = :status)
          AND (:categoryIsAble IS NULL OR s.category.isAble = :categoryIsAble)
    """)
    fun searchByTitle(
        @Param("keyword") keyword: String,
        @Param("status") status: ServiceStatus?,
        @Param("categoryIsAble") categoryIsAble: Boolean?
    ): List<CatalogItem>

    @Query("""
        SELECT s FROM CatalogItem s
        WHERE UPPER(s.title) LIKE UPPER(CONCAT(:prefix, '%'))
          AND (:status IS NULL OR s.status = :status)
          AND (:categoryIsAble IS NULL OR s.category.isAble = :categoryIsAble)
    """)
    fun autocompleteByTitle(
        @Param("prefix") prefix: String,
        @Param("status") status: ServiceStatus?,
        @Param("categoryIsAble") categoryIsAble: Boolean?
    ): List<CatalogItem>

    @Query("""
        SELECT s FROM CatalogItem s
        WHERE (UPPER(s.title) LIKE UPPER(CONCAT('%', :keyword, '%'))
           OR UPPER(s.description) LIKE UPPER(CONCAT('%', :keyword, '%')))
          AND (:status IS NULL OR s.status = :status)
          AND (:categoryIsAble IS NULL OR s.category.isAble = :categoryIsAble)
    """)
    fun searchInCatalog(
        @Param("keyword") keyword: String,
        @Param("status") status: ServiceStatus?,
        @Param("categoryIsAble") categoryIsAble: Boolean?
    ): List<CatalogItem>

    @Query("""
        SELECT s FROM CatalogItem s
        WHERE (:status IS NULL OR s.status = :status)
          AND (:priceType IS NULL OR s.priceType = :priceType)
          AND (:minPrice IS NULL OR s.basePrice >= :minPrice)
          AND (:maxPrice IS NULL OR s.basePrice <= :maxPrice)
          AND (:isNegotiable IS NULL OR s.isNegotiable = :isNegotiable)
          AND (:categoryIsAble IS NULL OR s.category.isAble = :categoryIsAble)
    """)
    fun filterCatalog(
        @Param("priceType") priceType: PriceType?,
        @Param("minPrice") minPrice: BigDecimal?,
        @Param("maxPrice") maxPrice: BigDecimal?,
        @Param("isNegotiable") isNegotiable: Boolean?,
        @Param("status") status: ServiceStatus?,
        @Param("categoryIsAble") categoryIsAble: Boolean?
    ): List<CatalogItem>
}