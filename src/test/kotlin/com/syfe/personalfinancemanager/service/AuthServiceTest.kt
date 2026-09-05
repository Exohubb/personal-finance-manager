package com.syfe.personalfinancemanager.service

import com.syfe.personalfinancemanager.dto.LoginRequest
import com.syfe.personalfinancemanager.dto.RegisterRequest
import com.syfe.personalfinancemanager.entity.User
import com.syfe.personalfinancemanager.exception.ConflictException
import com.syfe.personalfinancemanager.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var authenticationManager: AuthenticationManager

    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        authService = AuthService(userRepository, passwordEncoder, authenticationManager)
    }

    @Test
    fun `register rejects an already-used email`() {
        val request = RegisterRequest(
            username = "existing@example.com",
            password = "password123",
            fullName = "Existing User",
            phoneNumber = "+1234567890"
        )
        whenever(userRepository.existsByUsername("existing@example.com")).thenReturn(true)

        assertThrows(ConflictException::class.java) {
            authService.register(request)
        }
    }

    @Test
    fun `register hashes the password before saving and returns the new user id`() {
        val request = RegisterRequest(
            username = "new@example.com",
            password = "plainPassword",
            fullName = "New User",
            phoneNumber = "+1234567890"
        )
        whenever(userRepository.existsByUsername("new@example.com")).thenReturn(false)
        whenever(passwordEncoder.encode("plainPassword")).thenReturn("hashed-password")
        whenever(userRepository.save(any<User>())).thenAnswer {
            (it.arguments[0] as User).apply { id = 42L }
        }

        val response = authService.register(request)

        assertEquals(42L, response.userId)
        verify(userRepository).save(
            org.mockito.kotlin.argThat { user -> user.password == "hashed-password" }
        )
    }

    @Test
    fun `login delegates to the authentication manager and rethrows on bad credentials`() {
        val request = LoginRequest(username = "user@example.com", password = "wrongPassword")
        val httpRequest = org.mockito.Mockito.mock(HttpServletRequest::class.java)

        whenever(authenticationManager.authenticate(any<UsernamePasswordAuthenticationToken>()))
            .thenThrow(BadCredentialsException("Invalid username or password"))

        assertThrows(BadCredentialsException::class.java) {
            authService.login(request, httpRequest)
        }
    }

    @Test
    fun `login stores the authentication result in a new session on success`() {
        val request = LoginRequest(username = "user@example.com", password = "correctPassword")
        val httpRequest = org.mockito.Mockito.mock(HttpServletRequest::class.java)
        val httpSession = org.mockito.Mockito.mock(HttpSession::class.java)
        val authenticationResult = UsernamePasswordAuthenticationToken("user@example.com", "correctPassword")

        whenever(authenticationManager.authenticate(any<UsernamePasswordAuthenticationToken>()))
            .thenReturn(authenticationResult)
        whenever(httpRequest.getSession(true)).thenReturn(httpSession)

        authService.login(request, httpRequest)

        verify(httpSession).setAttribute(any(), any())
    }
}
