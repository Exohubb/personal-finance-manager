package com.syfe.personalfinancemanager.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

/**
 * Central Spring Security configuration: session-based authentication,
 * BCrypt password hashing, and JSON error responses in place of Spring
 * Security's default HTML-oriented behaviour.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val appUserDetailsService: AppUserDetailsService,
    private val restAuthenticationEntryPoint: RestAuthenticationEntryPoint,
    private val restAccessDeniedHandler: RestAccessDeniedHandler
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun daoAuthenticationProvider(passwordEncoder: PasswordEncoder): DaoAuthenticationProvider {
        val provider = DaoAuthenticationProvider(appUserDetailsService)
        provider.setPasswordEncoder(passwordEncoder)
        return provider
    }

    @Bean
    fun authenticationManager(daoAuthenticationProvider: DaoAuthenticationProvider): AuthenticationManager {
        return AuthenticationManager { authentication ->
            daoAuthenticationProvider.authenticate(authentication)
        }
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }

            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    .anyRequest().authenticated()
            }

            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            }

            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint(restAuthenticationEntryPoint)
                    .accessDeniedHandler(restAccessDeniedHandler)
            }

            .formLogin { it.disable() }
            .logout { it.disable() }

            .headers { headers -> headers.frameOptions { it.sameOrigin() } }

        return http.build()
    }
}
