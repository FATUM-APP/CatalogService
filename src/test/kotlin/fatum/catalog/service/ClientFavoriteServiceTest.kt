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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ClientFavoriteServiceTest {

    @Mock lateinit var favoriteRepository: ClientFavoriteRepository
    @Mock lateinit var catalogItemService: CatalogItemService
    @Mock lateinit var metricsService: MetricsService

    private lateinit var service: ClientFavoriteService
    private lateinit var category: Category
    private lateinit var item: CatalogItem

    @BeforeEach
    fun setUp() {
        service = ClientFavoriteService(favoriteRepository, catalogItemService, metricsService)
        category = Category("Belleza", SchedulingModel.SINGLE_SESSION)
        item = CatalogItem(
            providerId = "provider-1",
            title = "Corte de pelo",
            priceType = PriceType.FIXED,
            providerName = "Provider Test",
            providerUsername = "providertest",
            jobType = JobType.PRESENCIAL,
            category = category
        )
        setItemId(item, "service-1")
    }

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
            whenever(catalogItemService.findById("service-1", ServiceStatus.ACTIVE)).thenReturn(item)
            whenever(favoriteRepository.existsByClientIdAndCatalogItemId("client-1", "service-1")).thenReturn(false)

            service.addFavorite(FavoriteRequest("client-1", "service-1"))

            verify(favoriteRepository).save(any())
        }

        @Test
        fun `should throw ALREADY_FAVORITE when duplicate`() {
            whenever(catalogItemService.findById("service-1", ServiceStatus.ACTIVE)).thenReturn(item)
            whenever(favoriteRepository.existsByClientIdAndCatalogItemId("client-1", "service-1")).thenReturn(true)

            val exception = assertThrows<FatumServiceException> {
                service.addFavorite(FavoriteRequest("client-1", "service-1"))
            }

            assertEquals(CatalogErrorCode.ALREADY_FAVORITE, exception.errorCode)
            verify(favoriteRepository, never()).save(any())
        }
    }

    @Nested
    inner class RemoveFavorite {

        @Test
        fun `should remove favorite successfully`() {
            whenever(favoriteRepository.existsByClientIdAndCatalogItemId("client-1", "service-1")).thenReturn(true)

            service.removeFavorite(FavoriteRequest("client-1", "service-1"))

            verify(favoriteRepository).deleteByClientIdAndCatalogItemId("client-1", "service-1")
        }

        @Test
        fun `should throw FAVORITE_NOT_FOUND when not exists`() {
            whenever(favoriteRepository.existsByClientIdAndCatalogItemId("client-1", "service-1")).thenReturn(false)

            val exception = assertThrows<FatumServiceException> {
                service.removeFavorite(FavoriteRequest("client-1", "service-1"))
            }

            assertEquals(CatalogErrorCode.FAVORITE_NOT_FOUND, exception.errorCode)
            verify(favoriteRepository, never()).deleteByClientIdAndCatalogItemId(any(), any())
        }
    }

    @Nested
    inner class FindFavorites {

        @Test
        fun `should return all favorites by clientId`() {
            val fav1 = ClientFavorite("client-1", item)
            whenever(favoriteRepository.findByClientId("client-1")).thenReturn(listOf(fav1))

            val result = service.findFavorites("client-1")

            assertEquals(1, result.size)
            assertEquals(item, result[0].catalogItem)
            verify(favoriteRepository).findByClientId("client-1")
        }

        @Test
        fun `should return empty list when no favorites`() {
            whenever(favoriteRepository.findByClientId("client-1")).thenReturn(emptyList())

            val result = service.findFavorites("client-1")

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class FindFavoritesByCategory {

        @Test
        fun `should return favorites filtered by category`() {
            val fav1 = ClientFavorite("client-1", item)
            whenever(favoriteRepository.findByClientIdAndCategory("client-1", "Belleza"))
                .thenReturn(listOf(fav1))

            val result = service.findFavoritesByCategory("client-1", "Belleza")

            assertEquals(1, result.size)
            assertEquals("Belleza", result[0].catalogItem.category.name)
            verify(favoriteRepository).findByClientIdAndCategory("client-1", "Belleza")
        }

        @Test
        fun `should return empty list when no favorites in category`() {
            whenever(favoriteRepository.findByClientIdAndCategory("client-1", "Mascotas"))
                .thenReturn(emptyList())

            val result = service.findFavoritesByCategory("client-1", "Mascotas")

            assertTrue(result.isEmpty())
        }
    }
}
