package fatum.catalog.service;

import fatum.catalog.repository.CatalogItemRepository;
import org.springframework.stereotype.Service;

@Service
public class CatalogItemService {

    private final CatalogItemRepository catalogItemRepository;

    public CatalogItemService(CatalogItemRepository catalogItemRepository) {
        this.catalogItemRepository = catalogItemRepository;

    }

}
