package fatum.catalog.service

import fatum.catalog.client.AddressDTO
import fatum.catalog.client.M2User
import fatum.catalog.client.UserServiceClient
import fatum.catalog.dto.ChangeUnavailabilitySchedule
import fatum.catalog.dto.CreateFullUnavailability
import fatum.catalog.dto.CreateUnavailability
import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.ProviderUnavailableRange
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ProviderUnavailableRangeServiceTest {

    @Mock lateinit var unavailableRangeRepository: UnavailableRangeRepository
    @Mock lateinit var userServiceClient: UserServiceClient
    @InjectMocks lateinit var service: ProviderUnavailableRangeService

    private val validUser = M2User(
        userId = "provider-123",
        birthDate = LocalDate.of(1990, 1, 1),
        isProfessional = true,
        isVerified = true,
        isActive = true,
        gender = "M",
        principalAddress = AddressDTO("Calle 1", "Casa", "Bogota", "Cundinamarca", "Colombia", "juanpro")
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
            whenever(unavailableRangeRepository.save(any<ProviderUnavailableRange>()))
                .thenAnswer { it.arguments[0] }

            service.establishFullUnavailability(request)

            verify(unavailableRangeRepository).save(any())
        }
    }

    @Nested
    inner class ChangeUnavailabilitySchedule {

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
