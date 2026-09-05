package com.syfe.personalfinancemanager.security

import com.syfe.personalfinancemanager.entity.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppUserPrincipalTest {

    @Test
    fun `exposes the wrapped user's id, username and password correctly`() {
        val user = User(id = 7L, username = "person@example.com", password = "hashed-value", fullName = "Someone", phoneNumber = "+1")
        val principal = AppUserPrincipal(user)

        assertEquals(7L, principal.id)
        assertEquals("person@example.com", principal.username)
        assertEquals("hashed-value", principal.password)
    }

    @Test
    fun `account status flags are always true since this app has no lock or expiry feature`() {
        val user = User(id = 1L, username = "a@b.com", password = "x", fullName = "A", phoneNumber = "1")
        val principal = AppUserPrincipal(user)

        assertTrue(principal.isAccountNonExpired)
        assertTrue(principal.isAccountNonLocked)
        assertTrue(principal.isCredentialsNonExpired)
        assertTrue(principal.isEnabled)
    }

    @Test
    fun `has no granted authorities since this app has no role-based permissions`() {
        val user = User(id = 1L, username = "a@b.com", password = "x", fullName = "A", phoneNumber = "1")
        val principal = AppUserPrincipal(user)

        assertTrue(principal.authorities.isEmpty())
    }
}
