package com.syfe.personalfinancemanager.security

import com.syfe.personalfinancemanager.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/**
 * Bridges our [com.syfe.personalfinancemanager.repository.UserRepository]
 * to Spring Security. Called automatically by Spring Security's
 * authentication machinery during login - never invoked directly by our
 * own code.
 */
@Service
class AppUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    /**
     * @throws UsernameNotFoundException if no account exists with this username, which Spring Security treats as a failed login
     */
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            .orElseThrow { UsernameNotFoundException("No user found with username: $username") }

        return AppUserPrincipal(user)
    }
}
