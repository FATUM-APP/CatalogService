package fatum.catalog.service;

import fatum.catalog.dto.CreateServiceRequest;
import fatum.catalog.dto.mapper.CatalogItemMapper;
import fatum.catalog.exception.FatumServiceException;
import fatum.catalog.model.CatalogItem;
import fatum.catalog.model.Category;
import fatum.catalog.model.ServiceImage;
import fatum.catalog.repository.CatalogItemRepository;
import io.github.fatumapp.storage.FileStorageClient;
import io.github.fatumapp.storage.StoredFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class CatalogItemService {

    private final CatalogItemRepository catalogItemRepository;
    private final CategoryService categoryService;
    private final FileStorageClient fileStorageClient;
    private final String storageRoute;
    private static final int MAX_IMAGES = 10;

    public CatalogItemService(CatalogItemRepository catalogItemRepository,
                              CategoryService categoryService,
                              FileStorageClient fileStorageClient,
                              @Value("${catalog.images.storage-route}" ) String storageRoute) {
        this.catalogItemRepository = catalogItemRepository;
        this.categoryService = categoryService;
        this.fileStorageClient = fileStorageClient;
        this.storageRoute = storageRoute;
    }

    @Transactional
    public CatalogItem createCatalogItem(
            CreateServiceRequest request,
            List<MultipartFile> images
    ){
        validateImages(images);
        validateUniqueness(request);
        List<ServiceImage> serviceImages = new ArrayList<>();
        if(images.size() > MAX_IMAGES) throw new FatumServiceException(FatumServiceException.TOO_MANY_IMAGES);
        if(images == null || images.isEmpty()) throw new FatumServiceException(FatumServiceException.NO_SERVICE_IMAGES);
        if (images != null) {
            for (MultipartFile image : images) {
                StoredFile uploaded = fileStorageClient.upload(image, storageRoute);
                ServiceImage serviceImage = new ServiceImage(
                        uploaded.getKey(),
                        uploaded.getOriginalFilename(),
                        uploaded.getContentType(),
                        uploaded.getSize()
                );
                serviceImages.add(serviceImage);
            }
        }
        Category category = categoryService.findCategory(request.category());
        return catalogItemRepository.save(CatalogItemMapper.toEntity(request, serviceImages, category));
    }

    public List<CatalogItem> findByTitle(String title){
        return catalogItemRepository.findByTitleIgnoreCase(title);
    }

    public List<CatalogItem> findByTitleContaining(String keyword){
        return catalogItemRepository.findByTitleContainingIgnoreCase(keyword);
    }

    public List<CatalogItem> findByTitleStartingWith(String prefix){
        return catalogItemRepository.findByTitleStartingWithIgnoreCase(prefix);
    }

    public List<CatalogItem> searchInCatalog(String keyword){
        return catalogItemRepository.searchInCatalog(keyword);
    }



    public List<CatalogItem> findAll(){
        return catalogItemRepository.findAll();
    }



    private void validateUniqueness(CreateServiceRequest request){
        Category category = categoryService.findCategory(request.category());
        if(catalogItemRepository.existsByCategoryIdAndProviderIdIgnoreCase(
                category, request.providerId()))
            throw new FatumServiceException(FatumServiceException.UK_SERVICES_CATEGORY_PROVIDER);
        if(catalogItemRepository.existsByTittleAndProviderIdIgnoreCase(request.title(), request.providerId()))
            throw new FatumServiceException(FatumServiceException.UK_SERVICES_TITLE_PROVIDER);
    }



    //==================================== IMAGES ======================================
    private void validateImages(List<MultipartFile> images) {
        long maxImageSIze = 3L * 1024 * 1024;
        Set<String> allowedTypes = Set.of("image/jpeg", "image/png", "image/webp");

        if (images == null || images.isEmpty()) {
            return;
        }

        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                throw new FatumServiceException(FatumServiceException.INVALID_IMAGE);
            }

            if (image.getSize() > maxImageSIze) {
                throw new FatumServiceException(FatumServiceException.IMAGE_TOO_LARGE);
            }

            String contentType = image.getContentType();
            if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
                throw new FatumServiceException(FatumServiceException.INVALID_IMAGE_FORMAT);
            }
        }
    }


}
