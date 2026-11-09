package fatum.catalog.exception

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.servlet.resource.NoResourceFoundException

@ControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @PostConstruct
    fun init() {
        log.info(">>> GlobalExceptionHandler REGISTERED as @ControllerAdvice bean")
    }

    @ResponseBody
    @ExceptionHandler(FatumServiceException::class)
    fun handleServiceException(ex: FatumServiceException): ResponseEntity<Map<String, String?>> {
        log.warn("Handling FatumServiceException: {}", ex.errorCode, ex)
        val code = ex.errorCode
        val body = mapOf(
            "code" to code.name,
            "message" to ex.message
        )
        return ResponseEntity.status(code.httpStatus).body(body)
    }

    @ResponseBody
    @ExceptionHandler(HttpClientErrorException::class)
    fun handleHttpClientError(ex: HttpClientErrorException): ResponseEntity<Map<String, Any>> {
        val body = mapOf(
            "code" to "UPSTREAM_ERROR",
            "message" to (ex.statusText)
        )
        return ResponseEntity.status(ex.statusCode).body(body)
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResource(ex: NoResourceFoundException): ResponseEntity<Map<String, String?>> {
        val body = mapOf(
            "code" to "NOT_FOUND",
            "message" to ex.message
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body)
    }

    @ResponseBody
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<Map<String, String?>> {
        log.error("Handling unexpected exception", ex)
        val body = mapOf(
            "code" to "INTERNAL_ERROR",
            "message" to ex.message
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body)
    }
}
