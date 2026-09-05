package com.syfe.personalfinancemanager.security

import com.syfe.personalfinancemanager.entity.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * Wraps our own [User] entity so Spring Security's authentication machinery
 * can work with it. We wrap [User] instead of using Spring Security's
 * built-in `UserDetails` implementation so [id] stays available - every
 * service method needs the real database id to scope its queries by owner.
 */
class AppUserPrincipal(val user: User) : UserDetails {

    /** The wrapped user's real database id, used everywhere for data isolation. */
    val id: Long get() = user.id!!

    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()

    override fun getPassword(): String = user.password

    override fun getUsername(): String = user.username

    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true
}
