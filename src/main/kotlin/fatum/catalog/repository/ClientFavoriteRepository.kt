package fatum.catalog.repository

import fatum.catalog.model.ClientFavorite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ClientFavoriteRepository : JpaRepository<ClientFavorite, String> {

    @Query("""
        SELECT f FROM ClientFavorite f
        JOIN FETCH f.catalogItem ci
        JOIN FETCH ci.category
        WHERE f.clientId = :clientId
    """)
    fun findByClientId(@Param("clientId") clientId: String): List<ClientFavorite>

    @Query("""
        SELECT f FROM ClientFavorite f
        JOIN FETCH f.catalogItem ci
        JOIN FETCH ci.category c
        WHERE f.clientId = :clientId AND c.name = :categoryName
    """)
    fun findByClientIdAndCategory(
        @Param("clientId") clientId: String,
        @Param("categoryName") categoryName: String
    ): List<ClientFavorite>

    fun existsByClientIdAndCatalogItemId(clientId: String, serviceId: String): Boolean
    fun findByClientIdAndCatalogItemId(clientId: String, serviceId: String): ClientFavorite?
}
