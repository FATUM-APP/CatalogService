package fatum.catalog.repository

import fatum.catalog.model.CatalogItem
import org.springframework.data.jpa.repository.JpaRepository

interface CatalogItemRepository: JpaRepository<CatalogItem, String> {
}