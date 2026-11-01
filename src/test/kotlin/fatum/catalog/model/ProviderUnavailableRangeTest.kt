package fatum.catalog.model

import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class ProviderUnavailableRangeTest {

    private fun createRange(
        providerId: String = "provider-123",
        reason: String? = "Vacaciones",
        startDate: LocalDateTime = LocalDateTime.of(2026, 12, 1, 8, 0),
        finalDate: LocalDateTime = LocalDateTime.of(2026, 12, 5, 18, 0)
    ) = ProviderUnavailableRange(providerId, reason, startDate, finalDate)

    @Nested
    inner class Creation {

        @Test
        fun `should create range with datetime constructor`() {
            val range = createRange()

            assertAll(
                { assertEquals("provider-123", range.providerId) },
                { assertEquals("Vacaciones", range.reason) },
                { assertEquals(LocalDateTime.of(2026, 12, 1, 8, 0), range.startDate) },
                { assertEquals(LocalDateTime.of(2026, 12, 5, 18, 0), range.finalDate) },
                { assertNull(range.id) }
            )
        }

        @Test
        fun `should create range with LocalDate constructor`() {
            val range = ProviderUnavailableRange(
                "provider-123", "Feriado",
                start = LocalDate.of(2026, 12, 25),
                end = LocalDate.of(2026, 12, 26)
            )

            assertAll(
                { assertEquals(LocalDateTime.of(2026, 12, 25, 0, 0), range.startDate) },
                { assertEquals(LocalDateTime.of(2026, 12, 26, 23, 59), range.finalDate) }
            )
        }

        @Test
        fun `should allow null reason`() {
            val range = createRange(reason = null)
            assertNull(range.reason)
        }

        @Test
        fun `should throw BLANK on blank providerId`() {
            val ex = assertThrows<FatumServiceException> {
                createRange(providerId = "   ")
            }
            assertEquals(CatalogErrorCode.BLANK, ex.errorCode)
        }

        @Test
        fun `should throw INITIAL_AFTER_FINAL when start is after end`() {
            val ex = assertThrows<FatumServiceException> {
                createRange(
                    startDate = LocalDateTime.of(2026, 12, 10, 0, 0),
                    finalDate = LocalDateTime.of(2026, 12, 1, 0, 0)
                )
            }
            assertEquals(CatalogErrorCode.INITIAL_AFTER_FINAL, ex.errorCode)
        }

        @Test
        fun `should allow same start and end date`() {
            val sameDate = LocalDateTime.of(2026, 12, 1, 10, 0)
            val range = createRange(startDate = sameDate, finalDate = sameDate)

            assertEquals(range.startDate, range.finalDate)
        }
    }

    @Nested
    inner class ChangeDates {

        @Test
        fun `should change dates successfully`() {
            val range = createRange()
            val newStart = LocalDateTime.of(2026, 12, 10, 8, 0)
            val newEnd = LocalDateTime.of(2026, 12, 15, 18, 0)

            range.changeDates(newStart, newEnd)

            assertAll(
                { assertEquals(newStart, range.startDate) },
                { assertEquals(newEnd, range.finalDate) }
            )
        }

        @Test
        fun `should throw INITIAL_AFTER_FINAL on invalid date change`() {
            val range = createRange()

            val ex = assertThrows<FatumServiceException> {
                range.changeDates(
                    LocalDateTime.of(2026, 12, 20, 0, 0),
                    LocalDateTime.of(2026, 12, 10, 0, 0)
                )
            }
            assertEquals(CatalogErrorCode.INITIAL_AFTER_FINAL, ex.errorCode)
        }
    }

    @Nested
    inner class Equality {

        @Test
        fun `should be equal to itself`() {
            val range = createRange()
            assertEquals(range, range)
        }

        @Test
        fun `two ranges without id should not be equal`() {
            val range1 = createRange()
            val range2 = createRange()
            assertNotEquals(range1, range2)
        }

        @Test
        fun `should not be equal to non-ProviderUnavailableRange`() {
            val range = createRange()
            assertNotEquals(range, "not a range")
        }
    }
}
