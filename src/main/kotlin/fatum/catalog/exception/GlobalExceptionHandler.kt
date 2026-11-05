package fatum.catalog.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpClientErrorException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(FatumServiceException::class)
    fun handleServiceException(ex: FatumServiceException): ResponseEntity<Map<String, String?>> {
        val code = ex.errorCode
        val body = mapOf(
            "code" to code.name,
            "message" to ex.message
        )
        return ResponseEntity.status(code.httpStatus).body(body)
    }

    @ExceptionHandler(HttpClientErrorException::class)
    fun handleHttpClientError(ex: HttpClientErrorException): ResponseEntity<Map<String, Any>> {
        val body = mapOf(
            "code" to "UPSTREAM_ERROR",
            "message" to (ex.statusText)
        )
        return ResponseEntity.status(ex.statusCode).body(body)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<Map<String, String?>> {
        val body = mapOf(
            "code" to "INTERNAL_ERROR",
            "message" to ex.message
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body)
    }


}
