package com.syfe.personalfinancemanager.controller

import com.syfe.personalfinancemanager.dto.LoginRequest
import com.syfe.personalfinancemanager.dto.MessageResponse
import com.syfe.personalfinancemanager.dto.RegisterRequest
import com.syfe.personalfinancemanager.dto.RegisterResponse
import com.syfe.personalfinancemanager.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Registration, login, and logout. The only controller with endpoints that
 * are reachable without an existing session (`/register` and `/login`) -
 * see [com.syfe.personalfinancemanager.security.SecurityConfig].
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    /** `POST /api/auth/register` - creates a new account. */
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<RegisterResponse> {
        val response = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /** `POST /api/auth/login` - verifies credentials and starts a session, returned as a `JSESSIONID` cookie. */
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<MessageResponse> {
        authService.login(request, httpRequest)
        return ResponseEntity.ok(MessageResponse("Login successful"))
    }

    /**
     * `POST /api/auth/logout` - invalidates the current session. Handled
     * directly here rather than via Spring Security's built-in logout
     * filter, which is disabled in
     * [com.syfe.personalfinancemanager.security.SecurityConfig] so this
     * method actually runs instead of being intercepted beforehand.
     */
    @PostMapping("/logout")
    fun logout(httpRequest: HttpServletRequest): ResponseEntity<MessageResponse> {
        val session = httpRequest.getSession(false)
        session?.invalidate()
        return ResponseEntity.ok(MessageResponse("Logout successful"))
    }
}
