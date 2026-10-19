package fatum.catalog.repository;

import fatum.catalog.model.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, String> {
}
