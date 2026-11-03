package fatum.catalog.controller

import fatum.catalog.dto.request.schedule.CreateFullUnavailability
import fatum.catalog.dto.request.schedule.CreateUnavailability
import fatum.catalog.service.ProviderUnavailableRangeService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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







}