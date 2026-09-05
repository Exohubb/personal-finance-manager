package com.syfe.personalfinancemanager.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import tools.jackson.databind.exc.InvalidFormatException
import org.springframework.http.converter.HttpMessageNotReadableException

/**
 * Single global handler that converts every exception the app can throw
 * into the correct HTTP status code and a consistent
 * `{ "message": "..." }` JSON body. Registered via [RestControllerAdvice],
 * which applies it across every `@RestController` in the app without
 * needing a try/catch in each controller method.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    /** Maps [BadRequestException] to HTTP 400. */
    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(ex: BadRequestException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("message" to (ex.message ?: "Bad request")))
    }

    /** Maps [ResourceNotFoundException] to HTTP 404. */
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to (ex.message ?: "Resource not found")))
    }

    /** Maps [AppAccessDeniedException] to HTTP 403. */
    @ExceptionHandler(AppAccessDeniedException::class)
    fun handleAccessDenied(ex: AppAccessDeniedException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to (ex.message ?: "Access denied")))
    }

    /** Maps [ConflictException] to HTTP 409. */
    @ExceptionHandler(ConflictException::class)
    fun handleConflict(ex: ConflictException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to (ex.message ?: "Conflict")))
    }

    /**
     * Maps Spring Security's [AuthenticationException] to HTTP 401, thrown
     * by [org.springframework.security.authentication.AuthenticationManager.authenticate]
     * when login fails. Always returns the same generic message regardless
     * of whether the username didn't exist or the password was wrong, so
     * the API never reveals which email addresses have accounts.
     */
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationFailure(ex: AuthenticationException): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(mapOf("message" to "Invalid username or password"))
    }

    /**
     * Maps `@Valid` validation failures to HTTP 400. Combines every failed
     * field's message into one readable, comma-separated string instead of
     * Spring's default verbose error format.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, String>> {
        val combinedMessage = ex.bindingResult.fieldErrors
            .joinToString(", ") { it.defaultMessage ?: "${it.field} is invalid" }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("message" to combinedMessage))
    }

    /**
     * Maps malformed request bodies to HTTP 400 - unparsable JSON or an
     * invalid enum value (e.g. a category `type` that isn't `INCOME`/
     * `EXPENSE`). Both exception types are thrown by Jackson before our
     * own `@Valid` rules ever get a chance to run.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class, InvalidFormatException::class)
    fun handleMalformedRequest(ex: Exception): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("message" to "Request body is malformed or contains an invalid value"))
    }

    /**
     * Catch-all for any exception type not explicitly handled above.
     * Guarantees the API never returns a raw HTTP 500 with a leaked stack
     * trace for an unanticipated bug - every unhandled failure still comes
     * back as a safe, generic HTTP 400 instead.
     */
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("message" to "The request could not be processed"))
    }
}
