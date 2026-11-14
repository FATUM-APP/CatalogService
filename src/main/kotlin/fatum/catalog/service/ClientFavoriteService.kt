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
    private val metricsService: MetricsService
) {

    @Transactional
    fun addFavorite(request: FavoriteRequest) {
        val item = catalogItemService.findById(request.serviceId, ServiceStatus.ACTIVE)
        if(item.providerId == request.clientId)
            throw FatumServiceException(CatalogErrorCode.PROVIDER_AND_CLIENT)
        if (!favoriteRepository.existsByClientIdAndCatalogItemId(request.clientId, item.id!!)){
            favoriteRepository.save(ClientFavorite(request.clientId, item))
            metricsService.incrementFavorites(item.id!!)
        }

    }

    @Transactional
    fun removeFavorite(request: FavoriteRequest) {
        val favorite = favoriteRepository.findByClientIdAndCatalogItemId(request.clientId, request.serviceId)
        favorite?.let {
            favoriteRepository.delete(it)
            metricsService.decrementFavorites(request.serviceId)
        }

    }

    @Transactional(readOnly = true)
    fun findFavorites(clientId: String): List<ClientFavorite> =
        favoriteRepository.findByClientId(clientId)

    @Transactional(readOnly = true)
    fun findFavoritesByCategory(clientId: String, categoryName: String): List<ClientFavorite> =
        favoriteRepository.findByClientIdAndCategory(clientId, categoryName)
}
