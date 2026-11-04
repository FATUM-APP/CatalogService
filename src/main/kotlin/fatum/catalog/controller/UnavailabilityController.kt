package fatum.catalog.controller

import fatum.catalog.dto.mapper.toResponse
import fatum.catalog.dto.request.schedule.ChangeUnavailabilitySchedule
import fatum.catalog.dto.request.schedule.CreateFullUnavailability
import fatum.catalog.dto.request.schedule.CreateUnavailability
import fatum.catalog.dto.response.schedule.Unavailability
import fatum.catalog.service.ProviderUnavailableRangeService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.Year
import java.time.YearMonth

@RestController
@RequestMapping("/provider/unavailability")
@Validated
class UnavailabilityController(
    private val unavailabilityService: ProviderUnavailableRangeService
) {

    //==========================================CREATE==============================================
    @PostMapping("/create")
    fun createUnavailability(request : CreateUnavailability) : ResponseEntity<Void>{
        unavailabilityService.establishUnavailability(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/create/full")
    fun createFullUnavailability(request : CreateFullUnavailability) : ResponseEntity<Void>{
        unavailabilityService.establishFullUnavailability(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    //==========================================READ==============================================


    @GetMapping
    fun getProviderUnavailability(
        @RequestParam providerId : String
    ) : ResponseEntity<List<Unavailability>> =
        ResponseEntity.status(HttpStatus.OK).body(
            unavailabilityService.findProviderUnavailableRanges(providerId).map { it.toResponse() }
        )

    @GetMapping
    fun getProviderUnavailabilityByMonth(
        @RequestParam providerId : String,
        @RequestParam month : YearMonth
    ) : ResponseEntity<List<Unavailability>> =
        ResponseEntity.status(HttpStatus.OK).body(
            unavailabilityService.findProviderUnavailableRangesByMonth(providerId,month)
                .map { it.toResponse() }
        )

    //==========================================UPDATE==============================================

    @PutMapping("/reschedule")
    fun changeUnavailability(request : ChangeUnavailabilitySchedule) : ResponseEntity<Void> {
        unavailabilityService.changeUnavailabilitySchedule(request)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    //==========================================DELETE==============================================

    @DeleteMapping("/delete/{id}")
    fun deleteUnavailability(
        @PathVariable id : String
    ) : ResponseEntity<Void> {
        unavailabilityService.deleteUnavailability(id)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

}