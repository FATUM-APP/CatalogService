package fatum.catalog.service


import fatum.catalog.dto.request.item.FavoriteRequest
import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.ClientFavorite
import fatum.catalog.model.constant.ServiceStatus
import fatum.catalog.repository.ClientFavoriteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClientFavoriteService(
    private val favoriteRepository: ClientFavoriteRepository,
    private val catalogItemService: CatalogItemService,
) {

    @Transactional
    fun addFavorite(request: FavoriteRequest) {
        val item = catalogItemService.findById(request.serviceId, ServiceStatus.ACTIVE)
        if (favoriteRepository.existsByClientIdAndCatalogItemId(request.clientId, item.id!!))
            throw FatumServiceException(CatalogErrorCode.ALREADY_FAVORITE)
        favoriteRepository.save(ClientFavorite(request.clientId, item))
    }

    @Transactional
    fun removeFavorite(request: FavoriteRequest) {
        if (!favoriteRepository.existsByClientIdAndCatalogItemId(request.clientId, request.serviceId))
            throw FatumServiceException(CatalogErrorCode.FAVORITE_NOT_FOUND)
        favoriteRepository.deleteByClientIdAndCatalogItemId(request.clientId, request.serviceId)
    }

    @Transactional(readOnly = true)
    fun findFavorites(clientId: String): List<ClientFavorite> =
        favoriteRepository.findByClientId(clientId)

    @Transactional(readOnly = true)
    fun findFavoritesByCategory(clientId: String, categoryName: String): List<ClientFavorite> =
        favoriteRepository.findByClientIdAndCategory(clientId, categoryName)
}
