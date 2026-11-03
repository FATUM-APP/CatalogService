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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/catalog/category")
@Validated
class CategoryController(
    private val categoryService: CategoryService
) {

    //==========================================CREATE==============================================

    @PostMapping("/create/root")
    fun createRoot(request : CreateCategoryRequest) : ResponseEntity<Void> {
        categoryService.createRoot(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
    @PostMapping("/create/sub")
    fun createSub(request : CreateCategoryRequest) : ResponseEntity<Void> {
        categoryService.createSubCategory(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

  //==========================================READ==============================================

    @GetMapping("/{name}")
    fun getCategory(
        @PathVariable name: String
    ) : ResponseEntity<CategoryResponse> =
        ResponseEntity.status(HttpStatus.OK)
            .body(categoryService.findCategory(name,true).toResponse())

    @GetMapping("/{name}/services")
    fun getCategoryServices(
        @PathVariable name: String
    ) : ResponseEntity<List<ItemResponse>> =
        ResponseEntity.status(HttpStatus.OK)
            .body(categoryService.searchCategoryServices(name).map { it.toDto() })

    @GetMapping("/roots")
    fun getAllRootCategories() : ResponseEntity<List<CategoryResponse>> =
        ResponseEntity.status(HttpStatus.OK)
            .body(categoryService.searchRootCategories(true)
                .map { it.toResponse() })

    @GetMapping("/subs/{root}")
    fun getAllSubCategories(
        @PathVariable root: String
    ) : ResponseEntity<List<CategoryResponse>> =
        ResponseEntity.status(HttpStatus.OK)
            .body(categoryService.searchSubCategories(root,true)
                .map { it.toResponse() })





}