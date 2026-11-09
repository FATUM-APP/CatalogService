package fatum.catalog.service

import fatum.catalog.client.AddressDTO
import fatum.catalog.client.M2User
import fatum.catalog.client.UserServiceClient
import fatum.catalog.dto.request.schedule.ChangeUnavailabilitySchedule
import fatum.catalog.dto.request.schedule.CreateFullUnavailability
import fatum.catalog.dto.request.schedule.CreateUnavailability
import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Category
import fatum.catalog.model.Order
import fatum.catalog.model.ProviderUnavailableRange
import fatum.catalog.model.constant.JobType
import fatum.catalog.model.constant.OrderStatus
import fatum.catalog.model.constant.PriceType
import fatum.catalog.model.constant.SchedulingModel
import fatum.catalog.repository.OrderRepository
import fatum.catalog.repository.UnavailableRangeRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ProviderUnavailableRangeServiceTest {

    @Mock lateinit var unavailableRangeRepository: UnavailableRangeRepository
    @Mock lateinit var userServiceClient: UserServiceClient
    @Mock lateinit var orderRepository: OrderRepository
    @InjectMocks lateinit var service: ProviderUnavailableRangeService

    private val validUser = M2User(
        userId = "provider-123",
        name = "Juan Pro",
        username = "juanpro",
        birthDate = LocalDate.of(1990, 1, 1),
        isProfessional = true,
        isVerified = true,
        isActive = true,
        gender = "M",
        principalAddress = AddressDTO("Calle 1", "Casa", "Bogota", "Cundinamarca", "Colombia")
    )

    private val nonProfessionalUser = validUser.copy(isProfessional = false)
    private val inactiveUser = validUser.copy(isActive = false)

    private fun existingRange(
        id: String = "range-1",
        providerId: String = "provider-123",
        start: LocalDateTime = LocalDateTime.of(2026, 12, 1, 0, 0),
        end: LocalDateTime = LocalDateTime.of(2026, 12, 5, 23, 59)
    ): ProviderUnavailableRange {
        val range = ProviderUnavailableRange(providerId, "Vacaciones", start, end)
        ProviderUnavailableRange::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(range, id)
        }
        return range
    }

    private fun createPendingOrder(booking: LocalDateTime): Order {
        val cat = Category("TestCat", SchedulingModel.SINGLE_SESSION)
        val item = CatalogItem(
            providerId = "provider-123", title = "Test Service",
            providerName = "Provider Test", providerUsername = "providertest",
            priceType = PriceType.FIXED, jobType = JobType.PRESENCIAL, category = cat
        )
        return Order(
            clientId = "client-1", clientName = "Test Client", clientUsername = "testclient",
            booking = booking, catalogItem = item,
            negotiatedPrice = BigDecimal.ZERO,
            deliveryCity = "Bogota", deliveryState = "Cundinamarca",
            deliveryCountry = "Colombia", deliveryResidence = "Calle 1"
        )
    }

    @Nested
    inner class EstablishUnavailability {

        @Test
        fun `should create unavailability with datetime`() {
            val request = CreateUnavailability(
                providerId = "provider-123",
                reason = "Cita medica",
                start = LocalDateTime.of(2026, 12, 10, 8, 0),
                end = LocalDateTime.of(2026, 12, 10, 12, 0)
            )
            whenever(userServiceClient.getUserById("provider-123")).thenReturn(validUser)
            whenever(unavailableRangeRepository.findByProviderId("provider-123"))
                .thenReturn(emptyList())
            whenever(orderRepository.findByProviderIdAndStatusAndBookingBetween(
                any(), any(), any(), any()
            )).thenReturn(emptyList())
            whenever(unavailableRangeRepository.save(any<ProviderUnavailableRange>()))
                .thenAnswer { it.arguments[0] }

            service.establishUnavailability(request)

            verify(unavailableRangeRepository).save(any())
        }

        @Test
        fun `should throw NOT_A_PROFESSIONAL`() {
            val request = CreateUnavailability(
                providerId = "provider-123", start = LocalDateTime.now(), end = LocalDateTime.now().plusHours(1)
            )
            whenever(userServiceClient.getUserById("provider-123")).thenReturn(nonProfessionalUser)

            val ex = assertThrows<FatumServiceException> {
                service.establishUnavailability(request)
            }

            assertEquals(CatalogErrorCode.NOT_A_PROFESSIONAL, ex.errorCode)
            verify(unavailableRangeRepository, never()).save(any())
        }

        @Test
        fun `should throw INVALID_USER when not active`() {
            val request = CreateUnavailability(
                providerId = "provider-123", start = LocalDateTime.now(), end = LocalDateTime.now().plusHours(1)
            )
            whenever(userServiceClient.getUserById("provider-123")).thenReturn(inactiveUser)

            val ex = assertThrows<FatumServiceException> {
                service.establishUnavailability(request)
            }

            assertEquals(CatalogErrorCode.INVALID_USER, ex.errorCode)
        }

        @Test
        fun `should throw OVERLAPPING_RANGE when dates overlap existing`() {
            val existing = existingRange(
                start = LocalDateTime.of(2026, 12, 1, 0, 0),
                end = LocalDateTime.of(2026, 12, 5, 23, 59)
            )
            val request = CreateUnavailability(
                providerId = "provider-123",
                start = LocalDateTime.of(2026, 12, 3, 0, 0),
                end = LocalDateTime.of(2026, 12, 7, 0, 0)
            )
            whenever(userServiceClient.getUserById("provider-123")).thenReturn(validUser)
            whenever(unavailableRangeRepository.findByProviderId("provider-123"))
                .thenReturn(listOf(existing))

            val ex = assertThrows<FatumServiceException> {
                service.establishUnavailability(request)
            }

            assertEquals(CatalogErrorCode.OVERLAPPING_RANGE, ex.errorCode)
            verify(unavailableRangeRepository, never()).save(any())
        }

        @Test
        fun `should allow non-overlapping range`() {
            val existing = existingRange(
                start = LocalDateTime.of(2026, 12, 1, 0, 0),
                end = LocalDateTime.of(2026, 12, 5, 23, 59)
            )
            val request = CreateUnavailability(
                providerId = "provider-123",
                start = LocalDateTime.of(2026, 12, 10, 0, 0),
                end = LocalDateTime.of(2026, 12, 12, 0, 0)
            )
            whenever(userServiceClient.getUserById("provider-123")).thenReturn(validUser)
            whenever(unavailableRangeRepository.findByProviderId("provider-123"))
                .thenReturn(listOf(existing))
            whenever(orderRepository.findByProviderIdAndStatusAndBookingBetween(
                any(), any(), any(), any()
            )).thenReturn(emptyList())
            whenever(unavailableRangeRepository.save(any<ProviderUnavailableRange>()))
                .thenAnswer { it.arguments[0] }

            service.establishUnavailability(request)

            verify(unavailableRangeRepository).save(any())
        }
    }

    @Nested
    inner class EstablishFullUnavailability {

        @Test
        fun `should create full-day unavailability`() {
            val request = CreateFullUnavailability(
                providerId = "provider-123",
                reason = "Feriado",
                start = LocalDate.of(2026, 12, 25),
                end = LocalDate.of(2026, 12, 26)
            )
            whenever(userServiceClient.getUserById("provider-123")).thenReturn(validUser)
            whenever(unavailableRangeRepository.findByProviderId("provider-123"))
                .thenReturn(emptyList())
            whenever(orderRepository.findByProviderIdAndStatusAndBookingBetween(
                any(), any(), any(), any()
            )).thenReturn(emptyList())
            whenever(unavailableRangeRepository.save(any<ProviderUnavailableRange>()))
                .thenAnswer { it.arguments[0] }

            service.establishFullUnavailability(request)

            verify(unavailableRangeRepository).save(any())
        }
    }

    @Nested
    inner class HandleOrdersInRange {

        @Test
        fun `should throw ACCEPTED_ORDERS_EXIST when accepted orders exist in range`() {
            val start = LocalDateTime.of(2026, 12, 10, 8, 0)
            val end = LocalDateTime.of(2026, 12, 10, 18, 0)
            val request = CreateUnavailability(
                providerId = "provider-123", reason = "Vacaciones", start = start, end = end
            )

            whenever(userServiceClient.getUserById("provider-123")).thenReturn(validUser)
            whenever(unavailableRangeRepository.findByProviderId("provider-123"))
                .thenReturn(emptyList())
            whenever(orderRepository.findByProviderIdAndStatusAndBookingBetween(
                "provider-123", OrderStatus.ACCEPTED, start, end
            )).thenReturn(listOf(createPendingOrder(start.plusHours(2))))

            val ex = assertThrows<FatumServiceException> {
                service.establishUnavailability(request)
            }

            assertEquals(CatalogErrorCode.ACCEPTED_ORDERS_EXIST, ex.errorCode)
            verify(unavailableRangeRepository, never()).save(any())
        }

        @Test
        fun `should reject pending orders in range during unavailability establishment`() {
            val start = LocalDateTime.of(2026, 12, 10, 8, 0)
            val end = LocalDateTime.of(2026, 12, 10, 18, 0)
            val request = CreateUnavailability(
                providerId = "provider-123", reason = "Vacaciones", start = start, end = end
            )
            val pendingOrder = createPendingOrder(start.plusHours(1))

            whenever(userServiceClient.getUserById("provider-123")).thenReturn(validUser)
            whenever(unavailableRangeRepository.findByProviderId("provider-123"))
                .thenReturn(emptyList())
            whenever(orderRepository.findByProviderIdAndStatusAndBookingBetween(
                "provider-123", OrderStatus.ACCEPTED, start, end
            )).thenReturn(emptyList())
            whenever(orderRepository.findByProviderIdAndStatusAndBookingBetween(
                "provider-123", OrderStatus.PENDING, start, end
            )).thenReturn(listOf(pendingOrder))
            whenever(unavailableRangeRepository.save(any<ProviderUnavailableRange>()))
                .thenAnswer { it.arguments[0] }

            service.establishUnavailability(request)

            assertAll(
                { assertEquals(OrderStatus.REJECTED, pendingOrder.status) },
                { assertEquals("Provider marked as unavailable", pendingOrder.reason) }
            )
            verify(orderRepository).saveAll(listOf(pendingOrder))
            verify(unavailableRangeRepository).save(any())
        }

        @Test
        fun `should save unavailability when no orders exist in range`() {
            val start = LocalDateTime.of(2026, 12, 10, 8, 0)
            val end = LocalDateTime.of(2026, 12, 10, 18, 0)
            val request = CreateUnavailability(
                providerId = "provider-123", reason = "Descanso", start = start, end = end
            )

            whenever(userServiceClient.getUserById("provider-123")).thenReturn(validUser)
            whenever(unavailableRangeRepository.findByProviderId("provider-123"))
                .thenReturn(emptyList())
            whenever(orderRepository.findByProviderIdAndStatusAndBookingBetween(
                any(), any(), any(), any()
            )).thenReturn(emptyList())
            whenever(unavailableRangeRepository.save(any<ProviderUnavailableRange>()))
                .thenAnswer { it.arguments[0] }

            service.establishUnavailability(request)

            verify(unavailableRangeRepository).save(any())
        }
    }

    @Nested
    inner class ChangeUnavailabilityScheduleTest {

        @Test
        fun `should change dates of existing unavailability`() {
            val existing = existingRange()
            val newStart = LocalDateTime.of(2026, 12, 10, 0, 0)
            val newEnd = LocalDateTime.of(2026, 12, 15, 23, 59)
            val update = ChangeUnavailabilitySchedule(
                _id = "range-1", start = newStart, end = newEnd
            )

            whenever(unavailableRangeRepository.findById("range-1")).thenReturn(Optional.of(existing))
            whenever(unavailableRangeRepository.findByProviderId("provider-123"))
                .thenReturn(listOf(existing))
            whenever(unavailableRangeRepository.save(any<ProviderUnavailableRange>()))
                .thenAnswer { it.arguments[0] }

            service.changeUnavailabilitySchedule(update)

            assertAll(
                { assertEquals(newStart, existing.startDate) },
                { assertEquals(newEnd, existing.finalDate) }
            )
            verify(unavailableRangeRepository).save(existing)
        }

        @Test
        fun `should keep existing start when update start is null`() {
            val existing = existingRange()
            val originalStart = existing.startDate
            val newEnd = LocalDateTime.of(2026, 12, 20, 23, 59)
            val update = ChangeUnavailabilitySchedule(_id = "range-1", start = null, end = newEnd)

            whenever(unavailableRangeRepository.findById("range-1")).thenReturn(Optional.of(existing))
            whenever(unavailableRangeRepository.findByProviderId("provider-123"))
                .thenReturn(listOf(existing))
            whenever(unavailableRangeRepository.save(any<ProviderUnavailableRange>()))
                .thenAnswer { it.arguments[0] }

            service.changeUnavailabilitySchedule(update)

            assertAll(
                { assertEquals(originalStart, existing.startDate) },
                { assertEquals(newEnd, existing.finalDate) }
            )
        }

        @Test
        fun `should not overlap with itself when updating`() {
            val existing = existingRange()
            val update = ChangeUnavailabilitySchedule(
                _id = "range-1",
                start = LocalDateTime.of(2026, 12, 2, 0, 0),
                end = LocalDateTime.of(2026, 12, 4, 23, 59)
            )

            whenever(unavailableRangeRepository.findById("range-1")).thenReturn(Optional.of(existing))
            whenever(unavailableRangeRepository.findByProviderId("provider-123"))
                .thenReturn(listOf(existing))
            whenever(unavailableRangeRepository.save(any<ProviderUnavailableRange>()))
                .thenAnswer { it.arguments[0] }

            service.changeUnavailabilitySchedule(update)

            verify(unavailableRangeRepository).save(existing)
        }

        @Test
        fun `should throw OVERLAPPING_RANGE when update overlaps another range`() {
            val existing = existingRange(id = "range-1",
                start = LocalDateTime.of(2026, 12, 1, 0, 0),
                end = LocalDateTime.of(2026, 12, 5, 23, 59))
            val other = existingRange(id = "range-2",
                start = LocalDateTime.of(2026, 12, 10, 0, 0),
                end = LocalDateTime.of(2026, 12, 15, 23, 59))

            val update = ChangeUnavailabilitySchedule(
                _id = "range-1",
                start = LocalDateTime.of(2026, 12, 9, 0, 0),
                end = LocalDateTime.of(2026, 12, 12, 0, 0)
            )

            whenever(unavailableRangeRepository.findById("range-1")).thenReturn(Optional.of(existing))
            whenever(unavailableRangeRepository.findByProviderId("provider-123"))
                .thenReturn(listOf(existing, other))

            val ex = assertThrows<FatumServiceException> {
                service.changeUnavailabilitySchedule(update)
            }

            assertEquals(CatalogErrorCode.OVERLAPPING_RANGE, ex.errorCode)
            verify(unavailableRangeRepository, never()).save(any())
        }

        @Test
        fun `should throw UNAVAILABILITY_NOT_FOUND when id does not exist`() {
            whenever(unavailableRangeRepository.findById("range-999"))
                .thenReturn(Optional.empty())

            val update = ChangeUnavailabilitySchedule(_id = "range-999")

            val ex = assertThrows<FatumServiceException> {
                service.changeUnavailabilitySchedule(update)
            }

            assertEquals(CatalogErrorCode.UNAVAILABILITY_NOT_FOUND, ex.errorCode)
        }
    }

    @Nested
    inner class FindRanges {

        @Test
        fun `should return all ranges for provider`() {
            val ranges = listOf(existingRange(id = "r1"), existingRange(id = "r2"))
            whenever(unavailableRangeRepository.findByProviderId("provider-123"))
                .thenReturn(ranges)

            val result = service.findProviderUnavailableRanges("provider-123")

            assertEquals(2, result.size)
        }

        @Test
        fun `should return empty list when provider has no ranges`() {
            whenever(unavailableRangeRepository.findByProviderId("provider-999"))
                .thenReturn(emptyList())

            val result = service.findProviderUnavailableRanges("provider-999")

            assertTrue(result.isEmpty())
        }

        @Test
        fun `should return ranges filtered by month`() {
            val range = existingRange()
            val yearMonth = YearMonth.of(2026, 12)
            whenever(unavailableRangeRepository.findByProviderIdAndStartDateBetween(
                "provider-123",
                LocalDateTime.of(2026, 12, 1, 0, 0),
                LocalDateTime.of(2026, 12, 31, 23, 59, 59, 999_999_999)
            )).thenReturn(listOf(range))

            val result = service.findProviderUnavailableRangesByMonth("provider-123", yearMonth)

            assertEquals(1, result.size)
        }

        @Test
        fun `should return empty list when no ranges in month`() {
            val yearMonth = YearMonth.of(2027, 1)
            whenever(unavailableRangeRepository.findByProviderIdAndStartDateBetween(
                "provider-123",
                LocalDateTime.of(2027, 1, 1, 0, 0),
                LocalDateTime.of(2027, 1, 31, 23, 59, 59, 999_999_999)
            )).thenReturn(emptyList())

            val result = service.findProviderUnavailableRangesByMonth("provider-123", yearMonth)

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class ScheduledDateInRange {

        @Test
        fun `should return true when date falls within unavailability range`() {
            val range = existingRange(
                start = LocalDateTime.of(2026, 12, 1, 0, 0),
                end = LocalDateTime.of(2026, 12, 5, 23, 59)
            )
            whenever(unavailableRangeRepository.findByProviderId("provider-123"))
                .thenReturn(listOf(range))

            val result = service.scheduledDateInRange("provider-123", LocalDateTime.of(2026, 12, 3, 10, 0))

            assertTrue(result)
        }

        @Test
        fun `should return true when date is at start boundary`() {
            val range = existingRange(
                start = LocalDateTime.of(2026, 12, 1, 8, 0),
                end = LocalDateTime.of(2026, 12, 5, 18, 0)
            )
            whenever(unavailableRangeRepository.findByProviderId("provider-123"))
                .thenReturn(listOf(range))

            val result = service.scheduledDateInRange("provider-123", LocalDateTime.of(2026, 12, 1, 8, 0))

            assertTrue(result)
        }

        @Test
        fun `should return true when date is at end boundary`() {
            val range = existingRange(
                start = LocalDateTime.of(2026, 12, 1, 8, 0),
                end = LocalDateTime.of(2026, 12, 5, 18, 0)
            )
            whenever(unavailableRangeRepository.findByProviderId("provider-123"))
                .thenReturn(listOf(range))

            val result = service.scheduledDateInRange("provider-123", LocalDateTime.of(2026, 12, 5, 18, 0))

            assertTrue(result)
        }

        @Test
        fun `should return false when date is before all ranges`() {
            val range = existingRange(
                start = LocalDateTime.of(2026, 12, 10, 0, 0),
                end = LocalDateTime.of(2026, 12, 15, 23, 59)
            )
            whenever(unavailableRangeRepository.findByProviderId("provider-123"))
                .thenReturn(listOf(range))

            val result = service.scheduledDateInRange("provider-123", LocalDateTime.of(2026, 12, 5, 10, 0))

            assertFalse(result)
        }

        @Test
        fun `should return false when date is after all ranges`() {
            val range = existingRange(
                start = LocalDateTime.of(2026, 12, 1, 0, 0),
                end = LocalDateTime.of(2026, 12, 5, 23, 59)
            )
            whenever(unavailableRangeRepository.findByProviderId("provider-123"))
                .thenReturn(listOf(range))

            val result = service.scheduledDateInRange("provider-123", LocalDateTime.of(2026, 12, 10, 10, 0))

            assertFalse(result)
        }

        @Test
        fun `should return false when provider has no unavailability ranges`() {
            whenever(unavailableRangeRepository.findByProviderId("provider-123"))
                .thenReturn(emptyList())

            val result = service.scheduledDateInRange("provider-123", LocalDateTime.of(2026, 12, 3, 10, 0))

            assertFalse(result)
        }
    }

    @Nested
    inner class DeleteUnavailability {

        @Test
        fun `should delete existing unavailability`() {
            val existing = existingRange()
            whenever(unavailableRangeRepository.findById("range-1"))
                .thenReturn(Optional.of(existing))

            service.deleteUnavailability("range-1")

            verify(unavailableRangeRepository).delete(existing)
        }

        @Test
        fun `should throw UNAVAILABILITY_NOT_FOUND when deleting non-existent`() {
            whenever(unavailableRangeRepository.findById("range-999"))
                .thenReturn(Optional.empty())

            val ex = assertThrows<FatumServiceException> {
                service.deleteUnavailability("range-999")
            }

            assertEquals(CatalogErrorCode.UNAVAILABILITY_NOT_FOUND, ex.errorCode)
            verify(unavailableRangeRepository, never()).delete(any())
        }
    }
}
