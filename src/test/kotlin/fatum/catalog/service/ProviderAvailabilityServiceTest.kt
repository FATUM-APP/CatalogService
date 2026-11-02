package fatum.catalog.service

import fatum.catalog.client.M2User
import fatum.catalog.client.AddressDTO
import fatum.catalog.client.UserServiceClient
import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.ProviderAvailability
import fatum.catalog.repository.ProviderAvailabilityRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@ExtendWith(MockitoExtension::class)
class ProviderAvailabilityServiceTest {

    @Mock lateinit var providerAvailabilityRepository: ProviderAvailabilityRepository
    @Mock lateinit var userServiceClient: UserServiceClient
    @InjectMocks lateinit var service: ProviderAvailabilityService

    private val validUser = M2User(
        userId = "provider-123",
        name = "Juan Provider",
        userName = "juanpro",
        birthDate = LocalDate.of(1990, 1, 1),
        isProfessional = true,
        isVerified = true,
        isActive = true,
        gender = "M",
        principalAddress = AddressDTO("Calle 1", "Casa", "Bogota", "Cundinamarca", "Colombia")
    )

    private val nonProfessionalUser = validUser.copy(isProfessional = false)
    private val inactiveUser = validUser.copy(isActive = false)

    @Nested
    inner class Initialize {

        @Test
        fun `should create availability for all 7 days`() {
            whenever(userServiceClient.getUserById("provider-123")).thenReturn(validUser)
            whenever(providerAvailabilityRepository.save(any<ProviderAvailability>()))
                .thenAnswer { it.arguments[0] }

            service.initialize("provider-123")

            verify(providerAvailabilityRepository, org.mockito.kotlin.times(7)).save(any())
        }

        @Test
        fun `should throw NOT_A_PROFESSIONAL when user is not professional`() {
            whenever(userServiceClient.getUserById("provider-123")).thenReturn(nonProfessionalUser)

            val ex = assertThrows<FatumServiceException> {
                service.initialize("provider-123")
            }

            assertEquals(CatalogErrorCode.NOT_A_PROFESSIONAL, ex.errorCode)
            verify(providerAvailabilityRepository, never()).save(any())
        }

        @Test
        fun `should throw INVALID_USER when user is not active`() {
            whenever(userServiceClient.getUserById("provider-123")).thenReturn(inactiveUser)

            val ex = assertThrows<FatumServiceException> {
                service.initialize("provider-123")
            }

            assertEquals(CatalogErrorCode.INVALID_USER, ex.errorCode)
            verify(providerAvailabilityRepository, never()).save(any())
        }
    }

    @Nested
    inner class ChangeSchedule {

        @Test
        fun `should update existing availability`() {
            val existing = ProviderAvailability("provider-123", DayOfWeek.MONDAY)
            whenever(userServiceClient.getUserById("provider-123")).thenReturn(validUser)
            whenever(providerAvailabilityRepository.findByIdProviderIdAndIdDay("provider-123", DayOfWeek.MONDAY))
                .thenReturn(existing)
            whenever(providerAvailabilityRepository.save(any<ProviderAvailability>()))
                .thenAnswer { it.arguments[0] }

            service.changeSchedule("provider-123", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0))

            assertAll(
                { assertEquals(LocalTime.of(9, 0), existing.initialTime) },
                { assertEquals(LocalTime.of(18, 0), existing.finalTime) }
            )
            verify(providerAvailabilityRepository).save(existing)
        }

        @Test
        fun `should create new availability when none exists`() {
            whenever(userServiceClient.getUserById("provider-123")).thenReturn(validUser)
            whenever(providerAvailabilityRepository.findByIdProviderIdAndIdDay("provider-123", DayOfWeek.SATURDAY))
                .thenReturn(null)
            whenever(providerAvailabilityRepository.save(any<ProviderAvailability>()))
                .thenAnswer { it.arguments[0] }

            service.changeSchedule("provider-123", DayOfWeek.SATURDAY, LocalTime.of(10, 0), LocalTime.of(14, 0))

            verify(providerAvailabilityRepository).save(any())
        }

        @Test
        fun `should throw NOT_A_PROFESSIONAL`() {
            whenever(userServiceClient.getUserById("provider-123")).thenReturn(nonProfessionalUser)

            val ex = assertThrows<FatumServiceException> {
                service.changeSchedule("provider-123", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0))
            }

            assertEquals(CatalogErrorCode.NOT_A_PROFESSIONAL, ex.errorCode)
            verify(providerAvailabilityRepository, never()).save(any())
        }
    }

    @Nested
    inner class FullDayAvailability {

        @Test
        fun `should set existing availability to all day`() {
            val existing = ProviderAvailability("provider-123", DayOfWeek.WEDNESDAY,
                LocalTime.of(9, 0), LocalTime.of(17, 0))
            whenever(userServiceClient.getUserById("provider-123")).thenReturn(validUser)
            whenever(providerAvailabilityRepository.findByIdProviderIdAndIdDay("provider-123", DayOfWeek.WEDNESDAY))
                .thenReturn(existing)
            whenever(providerAvailabilityRepository.save(any<ProviderAvailability>()))
                .thenAnswer { it.arguments[0] }

            service.fullDayAvailability("provider-123", DayOfWeek.WEDNESDAY)

            assertAll(
                { assertEquals(LocalTime.of(0, 0), existing.initialTime) },
                { assertEquals(LocalTime.of(23, 59), existing.finalTime) }
            )
            verify(providerAvailabilityRepository).save(existing)
        }

        @Test
        fun `should create new all-day availability when none exists`() {
            whenever(userServiceClient.getUserById("provider-123")).thenReturn(validUser)
            whenever(providerAvailabilityRepository.findByIdProviderIdAndIdDay("provider-123", DayOfWeek.SUNDAY))
                .thenReturn(null)
            whenever(providerAvailabilityRepository.save(any<ProviderAvailability>()))
                .thenAnswer { it.arguments[0] }

            service.fullDayAvailability("provider-123", DayOfWeek.SUNDAY)

            verify(providerAvailabilityRepository).save(any())
        }

        @Test
        fun `should throw INVALID_USER when user is not valid`() {
            whenever(userServiceClient.getUserById("provider-123")).thenReturn(inactiveUser)

            val ex = assertThrows<FatumServiceException> {
                service.fullDayAvailability("provider-123", DayOfWeek.MONDAY)
            }

            assertEquals(CatalogErrorCode.INVALID_USER, ex.errorCode)
            verify(providerAvailabilityRepository, never()).save(any())
        }
    }

    @Nested
    inner class FindAvailability {

        @Test
        fun `should return availability when found`() {
            val existing = ProviderAvailability("provider-123", DayOfWeek.MONDAY)
            whenever(providerAvailabilityRepository.findByIdProviderIdAndIdDay("provider-123", DayOfWeek.MONDAY))
                .thenReturn(existing)

            val result = service.findAvailability("provider-123", DayOfWeek.MONDAY)

            assertSame(existing, result)
        }

        @Test
        fun `should throw AVAILABILITY_NOT_FOUND when not found`() {
            whenever(providerAvailabilityRepository.findByIdProviderIdAndIdDay("provider-123", DayOfWeek.SUNDAY))
                .thenReturn(null)

            val ex = assertThrows<FatumServiceException> {
                service.findAvailability("provider-123", DayOfWeek.SUNDAY)
            }

            assertEquals(CatalogErrorCode.AVAILABILITY_NOT_FOUND, ex.errorCode)
        }
    }

    @Nested
    inner class ProviderIsAvailable {

        @Test
        fun `should return true when time is within availability range`() {
            val existing = ProviderAvailability("provider-123", DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(17, 0))
            whenever(providerAvailabilityRepository.findByIdProviderIdAndIdDay("provider-123", DayOfWeek.MONDAY))
                .thenReturn(existing)

            val result = service.providerIsAvailable("provider-123", DayOfWeek.MONDAY, LocalTime.of(10, 0))

            assertTrue(result)
        }

        @Test
        fun `should return true when time is at start boundary`() {
            val existing = ProviderAvailability("provider-123", DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(17, 0))
            whenever(providerAvailabilityRepository.findByIdProviderIdAndIdDay("provider-123", DayOfWeek.MONDAY))
                .thenReturn(existing)

            val result = service.providerIsAvailable("provider-123", DayOfWeek.MONDAY, LocalTime.of(8, 0))

            assertTrue(result)
        }

        @Test
        fun `should return true when time is at end boundary`() {
            val existing = ProviderAvailability("provider-123", DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(17, 0))
            whenever(providerAvailabilityRepository.findByIdProviderIdAndIdDay("provider-123", DayOfWeek.MONDAY))
                .thenReturn(existing)

            val result = service.providerIsAvailable("provider-123", DayOfWeek.MONDAY, LocalTime.of(17, 0))

            assertTrue(result)
        }

        @Test
        fun `should return false when time is before availability range`() {
            val existing = ProviderAvailability("provider-123", DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(17, 0))
            whenever(providerAvailabilityRepository.findByIdProviderIdAndIdDay("provider-123", DayOfWeek.MONDAY))
                .thenReturn(existing)

            val result = service.providerIsAvailable("provider-123", DayOfWeek.MONDAY, LocalTime.of(7, 0))

            assertFalse(result)
        }

        @Test
        fun `should return false when time is after availability range`() {
            val existing = ProviderAvailability("provider-123", DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(17, 0))
            whenever(providerAvailabilityRepository.findByIdProviderIdAndIdDay("provider-123", DayOfWeek.MONDAY))
                .thenReturn(existing)

            val result = service.providerIsAvailable("provider-123", DayOfWeek.MONDAY, LocalTime.of(18, 0))

            assertFalse(result)
        }

        @Test
        fun `should return false when no availability exists for day`() {
            whenever(providerAvailabilityRepository.findByIdProviderIdAndIdDay("provider-123", DayOfWeek.SUNDAY))
                .thenReturn(null)

            val result = service.providerIsAvailable("provider-123", DayOfWeek.SUNDAY, LocalTime.of(10, 0))

            assertFalse(result)
        }
    }

    @Nested
    inner class FindAvailabilityByProvider {

        @Test
        fun `should return all availabilities for provider`() {
            val list = listOf(
                ProviderAvailability("provider-123", DayOfWeek.MONDAY),
                ProviderAvailability("provider-123", DayOfWeek.TUESDAY)
            )
            whenever(providerAvailabilityRepository.findByIdProviderId("provider-123"))
                .thenReturn(list)

            val result = service.findAvailabilityByProvider("provider-123")

            assertEquals(2, result.size)
        }

        @Test
        fun `should return empty list when provider has no availability`() {
            whenever(providerAvailabilityRepository.findByIdProviderId("provider-999"))
                .thenReturn(emptyList())

            val result = service.findAvailabilityByProvider("provider-999")

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class DeleteAvailability {

        @Test
        fun `should delete existing availability`() {
            val existing = ProviderAvailability("provider-123", DayOfWeek.FRIDAY)
            whenever(providerAvailabilityRepository.findByIdProviderIdAndIdDay("provider-123", DayOfWeek.FRIDAY))
                .thenReturn(existing)

            service.deleteAvailability("provider-123", DayOfWeek.FRIDAY)

            verify(providerAvailabilityRepository).delete(existing)
        }

        @Test
        fun `should throw AVAILABILITY_NOT_FOUND when deleting non-existent`() {
            whenever(providerAvailabilityRepository.findByIdProviderIdAndIdDay("provider-123", DayOfWeek.SUNDAY))
                .thenReturn(null)

            val ex = assertThrows<FatumServiceException> {
                service.deleteAvailability("provider-123", DayOfWeek.SUNDAY)
            }

            assertEquals(CatalogErrorCode.AVAILABILITY_NOT_FOUND, ex.errorCode)
            verify(providerAvailabilityRepository, never()).delete(any())
        }
    }
}
