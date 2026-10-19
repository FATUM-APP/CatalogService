package fatum.catalog.dto.mapper;

import fatum.catalog.dto.CreateServiceRequest;
import fatum.catalog.model.CatalogItem;
import fatum.catalog.model.Category;
import fatum.catalog.model.ServiceImage;

import java.util.List;

public class CatalogItemMapper {
    private CatalogItemMapper() {}

    public static CatalogItem toEntity(CreateServiceRequest req, List<ServiceImage> images, Category category) {
        return new CatalogItem.Builder()
                .providerId(req.providerId())
                .title(req.title())
                .description(req.description())
                .basePrice(req.basePrice())
                .priceType(req.priceType())
                .isNegotiable(req.isNegotiable())
                .defaultEstimatedTime(req.defaultEstimatedTime())
                .jobType(req.jobType())
                .category(category)
                .serviceCatalog(images)
                .fixedQuestions(req.fixedQuestions())
                .build();
    }
}
