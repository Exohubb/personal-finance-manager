package com.syfe.personalfinancemanager.security

import com.syfe.personalfinancemanager.entity.User
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class SecurityUtilsTest {

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `currentUserId reads the id off the AppUserPrincipal stored in the security context`() {
        val user = User(id = 99L, username = "someone@example.com", password = "x", fullName = "Someone", phoneNumber = "1")
        val principal = AppUserPrincipal(user)

        val authentication = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        SecurityContextHolder.getContext().authentication = authentication

        assertEquals(99L, SecurityUtils.currentUserId())
    }
}
