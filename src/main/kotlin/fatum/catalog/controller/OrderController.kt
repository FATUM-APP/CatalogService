package fatum.catalog.controller

import fatum.catalog.dto.mapper.toClient
import fatum.catalog.dto.mapper.toProvider
import fatum.catalog.dto.mapper.toResponse
import fatum.catalog.dto.request.order.BookService
import fatum.catalog.dto.request.order.UpdateReason
import fatum.catalog.dto.request.order.OrderResponse
import fatum.catalog.dto.response.order.ClientOrder
import fatum.catalog.dto.response.order.OrderDTO
import fatum.catalog.dto.response.order.ProviderOrder
import fatum.catalog.model.Order
import fatum.catalog.model.constant.ConfirmingParty
import fatum.catalog.model.constant.OrderStatus
import fatum.catalog.service.OrderService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/order")
@Validated
class OrderController(
    private val orderService: OrderService
) {

    //==========================================BOOKING==============================================

    @PostMapping("/book")
    fun book(@Valid @RequestBody request: BookService): ResponseEntity<Void> {
        orderService.book(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    //==========================================UPDATE=======================================

    @PutMapping("/respond")
    fun acceptOrReject(@Valid @RequestBody response: OrderResponse): ResponseEntity<Void> {
        orderService.acceptOrReject(response)
        return ResponseEntity.status(HttpStatus.OK).build()
    }


    @PutMapping("/cancel/{orderId}")
    fun cancel(
        @PathVariable orderId: String,
        @RequestBody body: UpdateReason,
    ): ResponseEntity<Void> {
        orderService.cancel(orderId.trim(),body.reason)
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    @PutMapping("/cancel/provider/{providerId}")
    fun cancelProviderOrders(
        @PathVariable providerId: String,
        @RequestBody body: UpdateReason,
    ): ResponseEntity<Void> {
        orderService.cancelProviderOrders(providerId.trim(), body.reason)
        return ResponseEntity.status(HttpStatus.OK).build()
    }



    @PutMapping("/confirm/start/{orderId}")
    fun confirmStart(
        @PathVariable orderId: String,
        @RequestParam party: ConfirmingParty
    ): ResponseEntity<Void> {
        orderService.confirmStart(orderId, party)
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    @PutMapping("/deny/start/{orderId}")
    fun denyStart(
        @PathVariable orderId: String,
        @RequestParam party: ConfirmingParty,
        @RequestBody body: UpdateReason
    ): ResponseEntity<Void> {
        orderService.denyStart(orderId, party, body.reason)
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    @PutMapping("/confirm/completion/{orderId}")
    fun confirmCompletion(
        @PathVariable orderId: String,
        @RequestParam party: ConfirmingParty
    ): ResponseEntity<Void> {
        orderService.confirmCompletion(orderId, party)
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    @PutMapping("/deny/completion/{orderId}")
    fun denyCompletion(
        @PathVariable orderId: String,
        @RequestParam party: ConfirmingParty,
        @RequestBody body: UpdateReason
    ): ResponseEntity<Void> {
        orderService.denyCompletion(orderId, party, body.reason)
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    @PutMapping("/confirm/payment/{orderId}")
    fun confirmPayment(
        @PathVariable orderId: String,
        @RequestParam party: ConfirmingParty
    ): ResponseEntity<Void> {
        orderService.confirmPayment(orderId, party)
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    @PutMapping("/deny/payment/{orderId}")
    fun denyPayment(
        @PathVariable orderId: String,
        @RequestParam party: ConfirmingParty,
        @RequestBody body: UpdateReason
    ): ResponseEntity<Void> {
        orderService.denyPayment(orderId, party, body.reason)
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    //==========================================READ================================================

    @GetMapping("/provider")
    fun getOrdersByProvider(
        @RequestParam providerId: String,
        @RequestParam(required = false) status: OrderStatus?
    ): ResponseEntity<List<ProviderOrder>> =
        ResponseEntity.status(HttpStatus.OK)
            .body(orderService.findOrdersByProvider(providerId, status).map { it.toProvider() })

    @GetMapping("/service")
    fun getOrdersByService(
        @RequestParam serviceId: String,
        @RequestParam(required = false) status: OrderStatus?
    ): ResponseEntity<List<OrderDTO>> =
        ResponseEntity.status(HttpStatus.OK)
            .body(orderService.findOrdersByService(serviceId, status).map { it.toResponse() })

    @GetMapping("/client")
    fun getOrdersByClient(
        @RequestParam clientId: String,
        @RequestParam(required = false) status: OrderStatus?
    ): ResponseEntity<List<ClientOrder>> =
        ResponseEntity.status(HttpStatus.OK)
            .body(orderService.findOrdersByClient(clientId, status).map { it.toClient() })

    @GetMapping("/{id}")
    fun getOrderById(@PathVariable id: String): ResponseEntity<OrderDTO> =
        ResponseEntity.status(HttpStatus.OK)
            .body(orderService.findOrderById(id).toResponse())
}