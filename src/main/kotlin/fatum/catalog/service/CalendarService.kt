package fatum.catalog.service


import fatum.catalog.model.vo.BookedSlot
import fatum.catalog.model.vo.CalendarDay
import fatum.catalog.model.Order
import fatum.catalog.model.ProviderAvailability
import fatum.catalog.model.ProviderUnavailableRange
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

@Service
class CalendarService(
    private val providerAvailabilityService: ProviderAvailabilityService,
    private val unavailableRangeService: ProviderUnavailableRangeService,
    private val orderService: OrderService
) {

    @Transactional(readOnly = true)
    fun getMonthCalendar(providerId: String, year: Int, month: Int): List<CalendarDay> {
        val yearMonth = YearMonth.of(year, month)
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()
        val startDateTime = startDate.atStartOfDay()
        val endDateTime = endDate.atTime(23, 59)

        val availabilities = providerAvailabilityService.findAvailabilityByProvider(providerId)
        val unavailableRanges = unavailableRangeService.findProviderUnavailableRanges(providerId)
        val activeOrders = orderService.findActiveOrders(providerId, startDateTime, endDateTime)

        return generateSequence(startDate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(endDate) }
            .map { date -> buildCalendarDay(date, availabilities, unavailableRanges, activeOrders) }
            .toList()
    }


    private fun buildCalendarDay(
        date: LocalDate,
        availabilities: List<ProviderAvailability>,
        unavailableRanges: List<ProviderUnavailableRange>,
        orders: List<Order>
    ): CalendarDay {
        val unavailableRange = unavailableRanges.find { range ->
            !date.isBefore(range.startDate.toLocalDate()) && !date.isAfter(range.finalDate.toLocalDate())
        }
        if (unavailableRange != null) {
            return CalendarDay(
                date = date,
                isAvailable = false,
                unavailableReason = unavailableRange.reason
            )
        }

        val dayAvailability = availabilities.find { it.id.day == date.dayOfWeek }
            ?: return CalendarDay(date = date, isAvailable = false)

        val dayOrders = orders.filter { it.booking.toLocalDate() == date }
        val bookedSlots = dayOrders.map { order ->
            BookedSlot(
                orderId = order.id!!,
                booking = order.booking,
                estimatedTime = order.estimatedTime,
                status = order.status
            )
        }

        return CalendarDay(
            date = date,
            isAvailable = true,
            initialTime = dayAvailability.initialTime,
            finalTime = dayAvailability.finalTime,
            bookedSlots = bookedSlots
        )
    }
}
