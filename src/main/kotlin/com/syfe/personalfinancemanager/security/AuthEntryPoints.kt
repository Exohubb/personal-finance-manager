package com.syfe.personalfinancemanager.security

import tools.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

/**
 * Produces the JSON `401` response returned whenever an unauthenticated
 * request hits a protected endpoint, replacing Spring Security's default
 * behaviour of redirecting to an HTML login page - not appropriate for a
 * pure JSON API.
 */
@Component
class RestAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.writer.write(
            objectMapper.writeValueAsString(mapOf("message" to "Authentication required"))
        )
    }
}

/**
 * Produces the JSON `403` response for access denials raised by Spring
 * Security itself. Most of this project's `403` responses actually come
 * from the service layer throwing
 * [com.syfe.personalfinancemanager.exception.AppAccessDeniedException]
 * instead; this handler covers denials Spring Security's own filters
 * raise before a request even reaches a controller.
 */
@Component
class RestAccessDeniedHandler(
    private val objectMapper: ObjectMapper
) : AccessDeniedHandler {

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = "application/json"
        response.writer.write(
            objectMapper.writeValueAsString(mapOf("message" to "Access denied"))
        )
    }
}
