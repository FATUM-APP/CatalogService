package fatum.catalog.controller

import fatum.catalog.service.OrderService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/order")
@Validated
class OrderController(
    private val orderService: OrderService
) {

}