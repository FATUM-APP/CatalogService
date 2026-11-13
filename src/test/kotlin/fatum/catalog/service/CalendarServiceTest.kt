package fatum.catalog.service

import fatum.catalog.model.CatalogItem
import fatum.catalog.model.Category
import fatum.catalog.model.Order
import fatum.catalog.model.ProviderAvailability
import fatum.catalog.model.ProviderUnavailableRange
import fatum.catalog.model.constant.JobType
import fatum.catalog.model.constant.OrderStatus
import fatum.catalog.model.constant.PriceType
import fatum.catalog.model.constant.SchedulingModel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@ExtendWith(MockitoExtension::class)
class CalendarServiceTest {

    @Mock lateinit var providerAvailabilityService: ProviderAvailabilityService
    @Mock lateinit var unavailableRangeService: ProviderUnavailableRangeService
    @Mock lateinit var orderService: OrderService

    private lateinit var calendarService: CalendarService

    private val providerId = "provider-123"
    private val monthStart = LocalDateTime.of(2027, 6, 1, 0, 0)
    private val monthEnd = LocalDateTime.of(2027, 6, 30, 23, 59)

    @BeforeEach
    fun setUp() {
        calendarService = CalendarService(providerAvailabilityService, unavailableRangeService, orderService)
    }

    private fun order(
        booking: LocalDateTime,
        estimatedTime: Int? = 60,
        status: OrderStatus = OrderStatus.PENDING
    ): Order {
        val category = Category("Plomeria", SchedulingModel.SINGLE_SESSION)
        val item = CatalogItem(
            providerId = providerId,
            title = "Reparacion De Tuberias",
            providerName = "Provider Test",
            providerUsername = "providertest",
            priceType = PriceType.FIXED,
            jobType = JobType.PRESENCIAL,
            category = category,
            basePrice = BigDecimal(100),
            defaultEstimatedTime = 60
        )
        val order = Order(
            clientId = "client-456",
            clientName = "Juan Perez",
            clientUsername = "juanperez",
            booking = booking,
            catalogItem = item,
            negotiatedPrice = BigDecimal(100),
            deliveryCity = "Bogota",
            deliveryState = "Cundinamarca",
            deliveryCountry = "Colombia",
            deliveryResidence = "Calle 123 #45-67"
        )
        Order::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(order, "order-1")
        }
        order.estimatedTime = estimatedTime
        if (status != OrderStatus.PENDING) {
            Order::class.java.getDeclaredField("status").apply {
                isAccessible = true
                set(order, status)
            }
        }
        return order
    }

    @Test
    fun `should return one entry per day of the month`() {
        whenever(providerAvailabilityService.findAvailabilityByProvider(providerId)).thenReturn(emptyList())
        whenever(unavailableRangeService.findProviderUnavailableRanges(providerId)).thenReturn(emptyList())
        whenever(orderService.findActiveOrders(eq(providerId), any(), any())).thenReturn(emptyList())

        val calendar = calendarService.getMonthCalendar(providerId, 2027, 6)

        assertEquals(30, calendar.size)
        assertEquals(LocalDate.of(2027, 6, 1), calendar.first().date)
        assertEquals(LocalDate.of(2027, 6, 30), calendar.last().date)
    }

    @Test
    fun `should include booked slots with their end time`() {
        val monday = DayOfWeek.MONDAY
        whenever(providerAvailabilityService.findAvailabilityByProvider(providerId)).thenReturn(
            listOf(ProviderAvailability(providerId, monday, LocalTime.of(8, 0), LocalTime.of(17, 0)))
        )
        whenever(unavailableRangeService.findProviderUnavailableRanges(providerId)).thenReturn(emptyList())
        whenever(orderService.findActiveOrders(eq(providerId), any(), any())).thenReturn(
            listOf(order(LocalDateTime.of(2027, 6, 14, 10, 0), estimatedTime = 90))
        )

        val day = calendarService.getMonthCalendar(providerId, 2027, 6)
            .first { it.date == LocalDate.of(2027, 6, 14) }

        assertAll(
            { assertTrue(day.isAvailable) },
            { assertEquals(LocalTime.of(8, 0), day.initialTime) },
            { assertEquals(LocalTime.of(17, 0), day.finalTime) },
            { assertEquals(1, day.bookedSlots.size) },
            { assertEquals(LocalDateTime.of(2027, 6, 14, 10, 0), day.bookedSlots.first().booking) },
            { assertEquals(LocalDateTime.of(2027, 6, 14, 11, 30), day.bookedSlots.first().endTime) },
            { assertEquals(OrderStatus.PENDING, day.bookedSlots.first().status) }
        )
    }

    @Test
    fun `should use the default duration when the order has no estimated time`() {
        whenever(providerAvailabilityService.findAvailabilityByProvider(providerId)).thenReturn(
            listOf(ProviderAvailability(providerId, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(17, 0)))
        )
        whenever(unavailableRangeService.findProviderUnavailableRanges(providerId)).thenReturn(emptyList())
        whenever(orderService.findActiveOrders(eq(providerId), any(), any())).thenReturn(
            listOf(order(LocalDateTime.of(2027, 6, 14, 10, 0), estimatedTime = null))
        )

        val day = calendarService.getMonthCalendar(providerId, 2027, 6)
            .first { it.date == LocalDate.of(2027, 6, 14) }

        assertEquals(LocalDateTime.of(2027, 6, 14, 10, 30), day.bookedSlots.first().endTime)
    }

    @Test
    fun `should mark every day of an unavailable range as unavailable`() {
        whenever(providerAvailabilityService.findAvailabilityByProvider(providerId)).thenReturn(
            listOf(ProviderAvailability(providerId, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(17, 0)))
        )
        whenever(unavailableRangeService.findProviderUnavailableRanges(providerId)).thenReturn(
            listOf(
                ProviderUnavailableRange(
                    providerId, "Vacaciones",
                    LocalDateTime.of(2027, 6, 20, 0, 0),
                    LocalDateTime.of(2027, 6, 22, 23, 59)
                )
            )
        )

        val calendar = calendarService.getMonthCalendar(providerId, 2027, 6)
        val blocked = calendar.filter { it.date in LocalDate.of(2027, 6, 20)..LocalDate.of(2027, 6, 22) }

        assertEquals(3, blocked.size)
        blocked.forEach {
            assertAll(
                { assertFalse(it.isAvailable) },
                { assertEquals("Vacaciones", it.unavailableReason) }
            )
        }
    }

    @Test
    fun `should mark a day as unavailable when the provider has no schedule for that weekday`() {
        whenever(unavailableRangeService.findProviderUnavailableRanges(providerId)).thenReturn(emptyList())
        whenever(orderService.findActiveOrders(eq(providerId), any(), any())).thenReturn(emptyList())
        whenever(providerAvailabilityService.findAvailabilityByProvider(providerId)).thenReturn(
            listOf(ProviderAvailability(providerId, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(17, 0)))
        )

        val tuesday = calendarService.getMonthCalendar(providerId, 2027, 6)
            .first { it.date == LocalDate.of(2027, 6, 15) }

        assertAll(
            { assertFalse(tuesday.isAvailable) },
            { assertNull(tuesday.initialTime) },
            { assertNull(tuesday.unavailableReason) }
        )
    }
}
