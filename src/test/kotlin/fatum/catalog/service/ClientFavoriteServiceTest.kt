package fatum.catalog.service

import fatum.catalog.dto.request.item.FavoriteRequest
import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Category
import fatum.catalog.model.ClientFavorite
import fatum.catalog.model.constant.JobType
import fatum.catalog.model.constant.PriceType
import fatum.catalog.model.constant.SchedulingModel
import fatum.catalog.model.constant.ServiceStatus
import fatum.catalog.repository.ClientFavoriteRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ClientFavoriteServiceTest {

    @Mock
    lateinit var favoriteRepository: ClientFavoriteRepository

    @Mock
    lateinit var catalogItemService: CatalogItemService

    @Mock
    lateinit var metricsService: MetricsService

    private lateinit var service: ClientFavoriteService

    private val category = Category("Belleza", SchedulingModel.SINGLE_SESSION)
    private val providerId = "provider-1"
    private val clientId = "client-1"
    private val serviceId = "service-1"

    @BeforeEach
    fun setUp() {
        service = ClientFavoriteService(favoriteRepository, catalogItemService, metricsService)
    }

    private fun buildItem(provider: String = providerId, id: String = serviceId): CatalogItem =
        CatalogItem(
            providerId = provider,
            title = "Corte de pelo",
            priceType = PriceType.FIXED,
            providerName = "Provider Test",
            providerUsername = "providertest",
            jobType = JobType.PRESENCIAL,
            category = category
        ).also { setItemId(it, id) }

    private fun setItemId(catalogItem: CatalogItem, id: String) {
        CatalogItem::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(catalogItem, id)
        }
    }

    @Nested
    inner class AddFavorite {

        @Test
        fun `should add favorite successfully`() {
            val item = buildItem()
            whenever(catalogItemService.findById(serviceId, ServiceStatus.ACTIVE)).thenReturn(item)
            whenever(favoriteRepository.existsByClientIdAndCatalogItemId(clientId, serviceId)).thenReturn(false)

            service.addFavorite(FavoriteRequest(clientId, serviceId))

            val captor = argumentCaptor<ClientFavorite>()
            verify(favoriteRepository).save(captor.capture())
            assertEquals(clientId, captor.firstValue.clientId)
            assertEquals(item, captor.firstValue.catalogItem)
            verify(metricsService).incrementFavorites(serviceId)
        }

        @Test
        fun `should only look for active services`() {
            val item = buildItem()
            whenever(catalogItemService.findById(serviceId, ServiceStatus.ACTIVE)).thenReturn(item)
            whenever(favoriteRepository.existsByClientIdAndCatalogItemId(clientId, serviceId)).thenReturn(false)

            service.addFavorite(FavoriteRequest(clientId, serviceId))

            verify(catalogItemService).findById(serviceId, ServiceStatus.ACTIVE)
        }

        @Test
        fun `should throw PROVIDER_AND_CLIENT when provider favorites own service`() {
            val ownItem = buildItem(provider = clientId)
            whenever(catalogItemService.findById(serviceId, ServiceStatus.ACTIVE)).thenReturn(ownItem)

            val exception = assertThrows<FatumServiceException> {
                service.addFavorite(FavoriteRequest(clientId, serviceId))
            }

            assertEquals(CatalogErrorCode.PROVIDER_AND_CLIENT, exception.errorCode)
            verify(favoriteRepository, never()).save(any())
            verify(metricsService, never()).incrementFavorites(any())
        }

        @Test
        fun `should validate ownership before checking duplicated favorite`() {
            val ownItem = buildItem(provider = clientId)
            whenever(catalogItemService.findById(serviceId, ServiceStatus.ACTIVE)).thenReturn(ownItem)

            assertThrows<FatumServiceException> {
                service.addFavorite(FavoriteRequest(clientId, serviceId))
            }

            verify(favoriteRepository, never()).existsByClientIdAndCatalogItemId(any(), any())
        }

        @Test
        fun `should be idempotent when service is already a favorite`() {
            val item = buildItem()
            whenever(catalogItemService.findById(serviceId, ServiceStatus.ACTIVE)).thenReturn(item)
            whenever(favoriteRepository.existsByClientIdAndCatalogItemId(clientId, serviceId)).thenReturn(true)

            assertDoesNotThrow {
                service.addFavorite(FavoriteRequest(clientId, serviceId))
            }

            verify(favoriteRepository, never()).save(any())
            verify(metricsService, never()).incrementFavorites(any())
        }

        @Test
        fun `should count the favorite only once when it is added twice`() {
            val item = buildItem()
            whenever(catalogItemService.findById(serviceId, ServiceStatus.ACTIVE)).thenReturn(item)
            whenever(favoriteRepository.existsByClientIdAndCatalogItemId(clientId, serviceId))
                .thenReturn(false)
                .thenReturn(true)

            service.addFavorite(FavoriteRequest(clientId, serviceId))
            service.addFavorite(FavoriteRequest(clientId, serviceId))

            verify(favoriteRepository).save(any())
            verify(metricsService).incrementFavorites(serviceId)
        }

        @Test
        fun `should propagate SERVICE_NOT_FOUND and not touch repositories`() {
            whenever(catalogItemService.findById(serviceId, ServiceStatus.ACTIVE))
                .thenThrow(FatumServiceException(CatalogErrorCode.SERVICE_NOT_FOUND))

            val exception = assertThrows<FatumServiceException> {
                service.addFavorite(FavoriteRequest(clientId, serviceId))
            }

            assertEquals(CatalogErrorCode.SERVICE_NOT_FOUND, exception.errorCode)
            verifyNoInteractions(favoriteRepository, metricsService)
        }
    }

    @Nested
    inner class RemoveFavorite {

        @Test
        fun `should remove favorite successfully`() {
            val favorite = ClientFavorite(clientId, buildItem())
            whenever(favoriteRepository.findByClientIdAndCatalogItemId(clientId, serviceId)).thenReturn(favorite)

            service.removeFavorite(FavoriteRequest(clientId, serviceId))

            verify(favoriteRepository).delete(favorite)
            verify(metricsService).decrementFavorites(serviceId)
        }

        @Test
        fun `should not throw FAVORITE_NOT_FOUND when favorite does not exist`() {
            whenever(favoriteRepository.findByClientIdAndCatalogItemId(clientId, serviceId)).thenReturn(null)

            assertDoesNotThrow {
                service.removeFavorite(FavoriteRequest(clientId, serviceId))
            }

            verify(favoriteRepository, never()).delete(any())
            verify(metricsService, never()).decrementFavorites(any())
        }

        @Test
        fun `should decrement favorites only once when removed twice`() {
            val favorite = ClientFavorite(clientId, buildItem())
            whenever(favoriteRepository.findByClientIdAndCatalogItemId(clientId, serviceId))
                .thenReturn(favorite)
                .thenReturn(null)

            service.removeFavorite(FavoriteRequest(clientId, serviceId))
            service.removeFavorite(FavoriteRequest(clientId, serviceId))

            verify(favoriteRepository).delete(favorite)
            verify(metricsService).decrementFavorites(serviceId)
        }

        @Test
        fun `should remove favorite using trimmed ids`() {
            val favorite = ClientFavorite(clientId, buildItem())
            whenever(favoriteRepository.findByClientIdAndCatalogItemId(clientId, serviceId)).thenReturn(favorite)

            service.removeFavorite(FavoriteRequest("  $clientId  ", " $serviceId "))

            verify(favoriteRepository).findByClientIdAndCatalogItemId(clientId, serviceId)
            verify(favoriteRepository).delete(favorite)
            verify(metricsService).decrementFavorites(serviceId)
        }
    }

    @Nested
    inner class FindFavorites {

        @Test
        fun `should return all favorites by clientId`() {
            val fav1 = ClientFavorite(clientId, buildItem())
            whenever(favoriteRepository.findByClientId(clientId)).thenReturn(listOf(fav1))

            val result = service.findFavorites(clientId)

            assertEquals(1, result.size)
            assertEquals(serviceId, result[0].catalogItem.id)
            verify(favoriteRepository).findByClientId(clientId)
        }

        @Test
        fun `should return empty list when no favorites`() {
            whenever(favoriteRepository.findByClientId(clientId)).thenReturn(emptyList())

            val result = service.findFavorites(clientId)

            assertTrue(result.isEmpty())
            verify(favoriteRepository).findByClientId(clientId)
        }
    }

    @Nested
    inner class FindFavoritesByCategory {

        @Test
        fun `should return favorites filtered by category`() {
            val fav1 = ClientFavorite(clientId, buildItem())
            whenever(favoriteRepository.findByClientIdAndCategory(clientId, "Belleza"))
                .thenReturn(listOf(fav1))

            val result = service.findFavoritesByCategory(clientId, "Belleza")

            assertEquals(1, result.size)
            assertEquals("Belleza", result[0].catalogItem.category.name)
            verify(favoriteRepository).findByClientIdAndCategory(clientId, "Belleza")
        }

        @Test
        fun `should return empty list when no favorites in category`() {
            whenever(favoriteRepository.findByClientIdAndCategory(clientId, "Mascotas"))
                .thenReturn(emptyList())

            val result = service.findFavoritesByCategory(clientId, "Mascotas")

            assertTrue(result.isEmpty())
        }
    }
}
