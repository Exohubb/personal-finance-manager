package com.syfe.personalfinancemanager.exception

/**
 * Custom exception types thrown by the service layer whenever a business
 * rule is broken. Each maps to exactly one HTTP status code, handled by
 * [GlobalExceptionHandler] - services never need to know about HTTP status
 * codes themselves, only which of these to throw.
 */

/** Maps to HTTP 400. Validation-style failures not already caught by `@Valid`, e.g. a future-dated transaction or an invalid month number. */
class BadRequestException(message: String) : RuntimeException(message)

/** Maps to HTTP 404. The requested resource, looked up by id or name, doesn't exist. */
class ResourceNotFoundException(message: String) : RuntimeException(message)

/** Maps to HTTP 403. The resource exists but belongs to a different user. */
class AppAccessDeniedException(message: String) : RuntimeException(message)

/** Maps to HTTP 409. The request would conflict with existing data, e.g. a duplicate email or category name. */
class ConflictException(message: String) : RuntimeException(message)
