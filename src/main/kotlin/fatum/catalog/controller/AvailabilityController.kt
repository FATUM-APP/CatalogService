package fatum.catalog.controller

import fatum.catalog.dto.mapper.toResponse
import fatum.catalog.dto.request.schedule.ChangeAvailability
import fatum.catalog.dto.request.schedule.FullAvailability
import fatum.catalog.dto.response.schedule.Availability
import fatum.catalog.service.ProviderAvailabilityService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.DayOfWeek


@RestController
@RequestMapping("/provider/availability")
@Validated
class AvailabilityController(
    private val availabilityService: ProviderAvailabilityService
) {

    //==========================================UPDATE==============================================



    @PutMapping("/reschedule")
    fun rescheduleAvailability(
        @RequestBody request: ChangeAvailability
    ): ResponseEntity<Void> {
        availabilityService.changeSchedule(request)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @PutMapping("/reschedule/full-day")
    fun rescheduleFullDayAvailability(
        @RequestBody request: FullAvailability
    ): ResponseEntity<Void> {
        availabilityService.fullDayAvailability(request)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    //==========================================READ==============================================

    @GetMapping(params = ["providerId", "day"])
    fun getAvailability(
        @RequestParam providerId : String,
        @RequestParam day : DayOfWeek
        ) :  ResponseEntity<Availability> =
        ResponseEntity.status(HttpStatus.OK)
            .body(availabilityService.findAvailability(providerId, day)
                .toResponse())

    @GetMapping(params = ["providerId"])
    fun getProviderAvailability(
        @RequestParam providerId : String,
    ) :  ResponseEntity<List<Availability>> =
        ResponseEntity.status(HttpStatus.OK)
            .body(availabilityService.findAvailabilityByProvider(providerId)
                .map { it.toResponse() })

    // =====================================DELETE=====================================================

    @DeleteMapping("/delete/{providerId}/{day}")
    fun deleteAvailability(
        @PathVariable providerId : String,
        @PathVariable day : DayOfWeek
    ) : ResponseEntity<Void> {
        availabilityService.deleteAvailability(providerId, day)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}