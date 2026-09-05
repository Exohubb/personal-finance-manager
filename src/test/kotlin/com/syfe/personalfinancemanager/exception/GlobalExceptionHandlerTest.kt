package com.syfe.personalfinancemanager.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.HttpMediaTypeNotSupportedException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `handleBadRequest returns 400 with the exception message`() {
        val response = handler.handleBadRequest(BadRequestException("Amount must be positive"))

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Amount must be positive", response.body?.get("message"))
    }

    @Test
    fun `handleNotFound returns 404 with the exception message`() {
        val response = handler.handleNotFound(ResourceNotFoundException("Goal not found: 5"))

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("Goal not found: 5", response.body?.get("message"))
    }

    @Test
    fun `handleAccessDenied returns 403 with the exception message`() {
        val response = handler.handleAccessDenied(AppAccessDeniedException("Not your resource"))

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertEquals("Not your resource", response.body?.get("message"))
    }

    @Test
    fun `handleConflict returns 409 with the exception message`() {
        val response = handler.handleConflict(ConflictException("Category already exists"))

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("Category already exists", response.body?.get("message"))
    }

    @Test
    fun `handleAuthenticationFailure returns a generic 401 message regardless of the underlying cause`() {
        val response = handler.handleAuthenticationFailure(BadCredentialsException("wrong password"))

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals("Invalid username or password", response.body?.get("message"))
    }

    @Test
    fun `handleMalformedRequest returns 400 for an unreadable request body`() {
        val response = handler.handleMalformedRequest(HttpMediaTypeNotSupportedException("bad body"))

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `handleUnexpected converts any unknown exception into a safe 400 response`() {
        val response = handler.handleUnexpected(RuntimeException("something we did not plan for"))

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("The request could not be processed", response.body?.get("message"))
    }
}
