package fatum.catalog.controller

import fatum.catalog.dto.mapper.toDto
import fatum.catalog.dto.request.item.FavoriteRequest
import fatum.catalog.dto.response.item.FavoritesByCategory
import fatum.catalog.dto.response.item.ItemResponse
import fatum.catalog.service.ClientFavoriteService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/catalog/favorites")
@Validated
class FavoriteController(
    private val favoriteService: ClientFavoriteService
) {

    @PostMapping("/add")
    fun addFavorite(@RequestBody request: FavoriteRequest): ResponseEntity<Void> {
        favoriteService.addFavorite(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @DeleteMapping("/remove/{serviceId}")
    fun removeFavorite(
        @PathVariable serviceId: String,
        @RequestParam clientId: String
    ): ResponseEntity<Void> {
        favoriteService.removeFavorite(FavoriteRequest(clientId, serviceId))
        return ResponseEntity.noContent().build()
    }


    @GetMapping
    fun getFavorites(
        @RequestParam clientId: String,
        @RequestParam(required = false) category: String?
    ): ResponseEntity<List<*>> {
        if (category != null) {
            val items: List<ItemResponse> = favoriteService
                .findFavoritesByCategory(clientId, category)
                .map { it.catalogItem.toDto() }
            return ResponseEntity.ok(items)
        }

        val grouped: List<FavoritesByCategory> = favoriteService.findFavorites(clientId)
            .groupBy { it.catalogItem.category }
            .map { (cat, favs) ->
                FavoritesByCategory(
                    category = cat.name,
                    categorySlug = cat.slug,
                    services = favs.map { it.catalogItem.toDto() }
                )
            }
        return ResponseEntity.ok(grouped)
    }
}
