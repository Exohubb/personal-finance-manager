package com.syfe.personalfinancemanager.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class RegisterRequest(
    @field:NotBlank(message = "Username is required")
    @field:Email(message = "Username must be a valid email address")
    val username: String,

    @field:NotBlank(message = "Password is required")
    val password: String,

    @field:NotBlank(message = "Full name is required")
    val fullName: String,

    @field:NotBlank(message = "Phone number is required")
    val phoneNumber: String
)

data class RegisterResponse(
    val message: String,
    val userId: Long
)

data class LoginRequest(
    @field:NotBlank(message = "Username is required")
    val username: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)

data class MessageResponse(
    val message: String
)
