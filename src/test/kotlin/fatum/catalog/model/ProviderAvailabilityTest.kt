package fatum.catalog.model

import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.constant.Day
import fatum.catalog.model.vo.ProviderAvailabilityId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalTime

class ProviderAvailabilityTest {

    private fun createAvailability(
        providerId: String = "provider-123",
        day: Day = Day.MONDAY,
        initialTime: LocalTime = LocalTime.of(8, 0),
        finalTime: LocalTime = LocalTime.of(17, 0)
    ) = ProviderAvailability(providerId, day, initialTime, finalTime)

    @Nested
    inner class Creation {

        @Test
        fun `should create availability with custom times`() {
            val availability = createAvailability()

            assertAll(
                { assertEquals("provider-123", availability.providerId) },
                { assertEquals(Day.MONDAY, availability.day) },
                { assertEquals(LocalTime.of(8, 0), availability.initialTime) },
                { assertEquals(LocalTime.of(17, 0), availability.finalTime) }
            )
        }

        @Test
        fun `should create availability with default all-day times`() {
            val availability = ProviderAvailability("provider-123", Day.FRIDAY)

            assertAll(
                { assertEquals(LocalTime.of(0, 0), availability.initialTime) },
                { assertEquals(LocalTime.of(23, 59), availability.finalTime) }
            )
        }

        @Test
        fun `should create availability using composite id constructor`() {
            val id = ProviderAvailabilityId("provider-123", Day.TUESDAY)
            val availability = ProviderAvailability(id)

            assertAll(
                { assertEquals("provider-123", availability.providerId) },
                { assertEquals(Day.TUESDAY, availability.day) },
                { assertEquals(id, availability.id) }
            )
        }

        @Test
        fun `should throw BLANK on blank providerId`() {
            val ex = assertThrows<FatumServiceException> {
                createAvailability(providerId = "   ")
            }
            assertEquals(CatalogErrorCode.BLANK, ex.errorCode)
        }

        @Test
        fun `should throw INITIAL_AFTER_FINAL when start is after end`() {
            val ex = assertThrows<FatumServiceException> {
                createAvailability(
                    initialTime = LocalTime.of(18, 0),
                    finalTime = LocalTime.of(8, 0)
                )
            }
            assertEquals(CatalogErrorCode.INITIAL_AFTER_FINAL, ex.errorCode)
        }

        @Test
        fun `should allow same start and end time`() {
            val availability = createAvailability(
                initialTime = LocalTime.of(12, 0),
                finalTime = LocalTime.of(12, 0)
            )
            assertEquals(0L, availability.duration)
        }
    }

    @Nested
    inner class ChangeSchedule {

        @Test
        fun `should change schedule to new times`() {
            val availability = createAvailability()
            availability.changeSchedule(LocalTime.of(9, 0), LocalTime.of(18, 0))

            assertAll(
                { assertEquals(LocalTime.of(9, 0), availability.initialTime) },
                { assertEquals(LocalTime.of(18, 0), availability.finalTime) }
            )
        }

        @Test
        fun `should change to all day`() {
            val availability = createAvailability()
            availability.changeAvailabilityToAllDay()

            assertAll(
                { assertEquals(LocalTime.of(0, 0), availability.initialTime) },
                { assertEquals(LocalTime.of(23, 59), availability.finalTime) }
            )
        }

        @Test
        fun `should throw INITIAL_AFTER_FINAL on invalid schedule change`() {
            val availability = createAvailability()

            val ex = assertThrows<FatumServiceException> {
                availability.changeSchedule(LocalTime.of(20, 0), LocalTime.of(8, 0))
            }
            assertEquals(CatalogErrorCode.INITIAL_AFTER_FINAL, ex.errorCode)
        }
    }

    @Nested
    inner class Duration {

        @Test
        fun `should calculate duration in minutes`() {
            val availability = createAvailability(
                initialTime = LocalTime.of(8, 0),
                finalTime = LocalTime.of(17, 0)
            )
            assertEquals(540L, availability.duration)
        }
    }

    @Nested
    inner class Equality {

        @Test
        fun `should be equal when same composite id`() {
            val a = createAvailability(day = Day.MONDAY)
            val b = createAvailability(day = Day.MONDAY,
                initialTime = LocalTime.of(10, 0), finalTime = LocalTime.of(14, 0))
            assertEquals(a, b)
        }

        @Test
        fun `should not be equal when different day`() {
            val a = createAvailability(day = Day.MONDAY)
            val b = createAvailability(day = Day.TUESDAY)
            assertNotEquals(a, b)
        }

        @Test
        fun `should not be equal to non-ProviderAvailability`() {
            val a = createAvailability()
            assertNotEquals(a, "not an availability")
        }
    }
}
