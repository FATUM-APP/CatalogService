package fatum.catalog.controller

import fatum.catalog.dto.mapper.toDto
import fatum.catalog.dto.mapper.toResponse
import fatum.catalog.dto.request.category.CreateCategoryRequest
import fatum.catalog.dto.response.category.CategoryResponse
import fatum.catalog.dto.response.item.ItemResponse
import fatum.catalog.service.CategoryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile


@RestController
@RequestMapping("/catalog/category")
@Validated
class CategoryController(
    private val categoryService: CategoryService
) {

    //==========================================CREATE==============================================

    @PostMapping("/create/root", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createRoot(
        @RequestPart("request") request: CreateCategoryRequest,
        @RequestPart("logo", required = false) logo: MultipartFile?
    ): ResponseEntity<Void> {
        categoryService.createRoot(request, logo)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/create/sub", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createSub(
        @RequestPart("request") request: CreateCategoryRequest,
        @RequestPart("logo", required = false) logo: MultipartFile?
    ): ResponseEntity<Void> {
        categoryService.createSubCategory(request, logo)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

  //==========================================READ==============================================

    @GetMapping("/{name}")
    fun getCategory(
        @PathVariable name: String
    ): ResponseEntity<CategoryResponse> {
        val category = categoryService.findCategory(name, true)
        return ResponseEntity.ok(
            category.toResponse(categoryService.getCategoryLogoUrl(category))
        )
    }

    @GetMapping("/services")
    fun getCategoryServices(
        @RequestParam name: String
    ) : ResponseEntity<List<ItemResponse>> =
        ResponseEntity.status(HttpStatus.OK)
            .body(categoryService.searchCategoryServices(name).map { it.toDto() })

    @GetMapping("/roots")
    fun getAllRootCategories() : ResponseEntity<List<CategoryResponse>> =
        ResponseEntity.status(HttpStatus.OK)
            .body(categoryService.searchRootCategories(true)
                .map { it.toResponse(categoryService.getCategoryLogoUrl(it)) })

    @GetMapping("/subs")
    fun getAllSubCategories(
        @RequestParam root: String
    ) : ResponseEntity<List<CategoryResponse>> =
        ResponseEntity.status(HttpStatus.OK)
            .body(categoryService.searchSubCategories(root,true)
                .map { it.toResponse(categoryService.getCategoryLogoUrl(it)) })



    //==========================================UPDATE==============================================

    @PutMapping("/{name}/logo", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateLogo(
        @PathVariable name: String,
        @RequestPart("image") image: MultipartFile
    ): ResponseEntity<Void> {
        categoryService.updateLogo(name, image)
        return ResponseEntity.ok().build()
    }


}