package fatum.catalog.controller

import fatum.catalog.model.vo.CalendarDay
import fatum.catalog.service.CalendarService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/provider/calendar")
@Validated
class CalendarController(
    private val calendarService: CalendarService
) {

    @GetMapping
    fun getMonthCalendar(
        @RequestParam providerId: String,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<List<CalendarDay>> =
        ResponseEntity.status(HttpStatus.OK)
            .body(calendarService.getMonthCalendar(providerId, year, month))
}
