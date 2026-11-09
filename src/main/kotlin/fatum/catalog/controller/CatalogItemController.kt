package fatum.catalog.controller

import fatum.catalog.dto.mapper.toDto
import fatum.catalog.dto.request.item.CatalogImage
import fatum.catalog.dto.request.item.CreateServiceRequest
import fatum.catalog.dto.request.item.ActivateCatalogItem
import fatum.catalog.dto.request.item.UpdateServiceRequest
import fatum.catalog.dto.response.item.ItemResponse
import fatum.catalog.model.constant.PriceType
import fatum.catalog.model.constant.ServiceStatus
import fatum.catalog.service.CatalogItemService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal


@RestController
@RequestMapping("/catalog/item")
@Validated
class CatalogItemController(
    private val catalogItemService: CatalogItemService
) {

    //==========================================CREATE==============================================

    @PostMapping("/create", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createItem(
        @Valid @RequestPart("request") request: CreateServiceRequest,
        @RequestPart("images") images: List<MultipartFile>
    ): ResponseEntity<Void> {
        catalogItemService.createCatalogItem(request, images)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    //==========================================READ==============================================

    @GetMapping("/{id}")
    fun getById(@PathVariable id: String): ResponseEntity<ItemResponse> =
        ResponseEntity.status(HttpStatus.OK).body(
            (catalogItemService.findById(id, ServiceStatus.ACTIVE).toDto())
        )

    @GetMapping(params = ["title"])
    fun getByTitle(@RequestParam title: String): ResponseEntity<List<ItemResponse>> =
        ResponseEntity.status(HttpStatus.OK).body(
            catalogItemService.findByTitle(title).map { it.toDto() }
        )

    @GetMapping(params = ["keyword"])
    fun getByTitleContaining(@RequestParam keyword: String): ResponseEntity<List<ItemResponse>> =
        ResponseEntity.status(HttpStatus.OK).body(
            catalogItemService.findByTitleContaining(keyword).map { it.toDto() }
        )

    @GetMapping(params = ["prefix"])
    fun getByTitleStartingWith(@RequestParam prefix: String): ResponseEntity<List<ItemResponse>> =
        ResponseEntity.status(HttpStatus.OK).body(
            catalogItemService.findByTitleStartingWith(prefix).map { it.toDto() }
        )

    @GetMapping(params = ["filterKeyword"])
    fun searchInCatalog(@RequestParam filterKeyword: String): ResponseEntity<List<ItemResponse>> =
        ResponseEntity.status(HttpStatus.OK).body(
            catalogItemService.searchInCatalog(filterKeyword, ServiceStatus.ACTIVE).map { it.toDto() }
        )

    @GetMapping("/filter")
    fun filterCatalog(
        @RequestParam(required = false) priceType: PriceType?,
        @RequestParam(required = false, defaultValue = "0") minPrice: BigDecimal?,
        @RequestParam(required = false) maxPrice: BigDecimal?,
        @RequestParam(required = false) isNegotiable: Boolean?,
        @RequestParam(required = false) categoryName: String?,
    ): ResponseEntity<List<ItemResponse>> =
        ResponseEntity.status(HttpStatus.OK).body(
            catalogItemService.filterCatalog(
                priceType,
                minPrice,
                maxPrice,
                isNegotiable,
                categoryName,
                ServiceStatus.ACTIVE
            ).map { it.toDto() }
        )

    @GetMapping("/all")
    fun findAll(): ResponseEntity<List<ItemResponse>> {
        return ResponseEntity.status(HttpStatus.OK).body(
            catalogItemService.findAll(ServiceStatus.ACTIVE).map { it.toDto() }
        )
    }

    @GetMapping("/images")
    fun seeCatalogImages(
        @RequestParam id: String,
    ): ResponseEntity<List<CatalogImage>> {
        return ResponseEntity.ok(catalogItemService.seeCatalogImages(id))
    }

    @GetMapping("/provider")
    fun getProviderCatalogItems(
        @RequestParam providerId: String,
        @RequestParam(required = false) category: String?
    ) : ResponseEntity<List<ItemResponse>> =
        ResponseEntity.status(HttpStatus.OK).body(
            catalogItemService.findByProviderAndCategory(providerId,category).map { it.toDto() }
        )

    //==========================================UPDATE==============================================

    @PutMapping("/update", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateItem(
        @Valid @RequestPart("request") request: UpdateServiceRequest,
        @RequestPart("images") images: List<MultipartFile>
    ): ResponseEntity<Void> {
        catalogItemService.updateCatalogItem(request, images)
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    @PutMapping("/deactivate")
    fun desactivateItem(@RequestBody request : ActivateCatalogItem) : ResponseEntity<Void> {
        catalogItemService.deactivate(request)
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    @PutMapping("/activate")
    fun activateItem(@RequestBody request : ActivateCatalogItem) : ResponseEntity<Void> {
        catalogItemService.activate(request)
        return ResponseEntity.status(HttpStatus.OK).build()
    }
















}