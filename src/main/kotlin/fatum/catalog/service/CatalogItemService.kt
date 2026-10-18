package fatum.catalog.service

import fatum.catalog.model.CatalogItem
import fatum.catalog.repository.CatalogItemRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class CatalogItemService(
    val catalogItemRepository: CatalogItemRepository
){

}