package com.syfe.personalfinancemanager.service

import com.syfe.personalfinancemanager.dto.LoginRequest
import com.syfe.personalfinancemanager.dto.RegisterRequest
import com.syfe.personalfinancemanager.dto.RegisterResponse
import com.syfe.personalfinancemanager.entity.User
import com.syfe.personalfinancemanager.exception.ConflictException
import com.syfe.personalfinancemanager.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager
) {

    fun register(request: RegisterRequest): RegisterResponse {
        if (userRepository.existsByUsername(request.username)) {
            throw ConflictException("An account with this email already exists")
        }

        val user = User(
            username = request.username,
            password = passwordEncoder.encode(request.password)!!,
            fullName = request.fullName,
            phoneNumber = request.phoneNumber
        )

        val saved = userRepository.save(user)

        return RegisterResponse(
            message = "User registered successfully",
            userId = saved.id!!
        )
    }

    fun login(request: LoginRequest, httpRequest: HttpServletRequest) {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )

        val context = SecurityContextHolder.getContext()
        context.authentication = authentication

        val session = httpRequest.getSession(true)
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            context
        )
    }
}
