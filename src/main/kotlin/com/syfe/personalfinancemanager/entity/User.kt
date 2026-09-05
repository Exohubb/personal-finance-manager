package com.syfe.personalfinancemanager.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * A registered account in the system.
 *
 * [username] doubles as the login identifier and must be a valid email
 * address (enforced at the DTO validation layer, not here). [password] is
 * always stored as a BCrypt hash, never as plain text - see
 * [com.syfe.personalfinancemanager.service.AuthService.register].
 *
 * The table is named `APP_USER` rather than `USER` because `USER` is a
 * reserved keyword in some SQL dialects (including H2), which can cause
 * unexpected query errors if used directly as a table name.
 */
@Entity
@Table(name = "APP_USER")
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var username: String = "",

    @Column(nullable = false)
    var password: String = "",

    @Column(nullable = false)
    var fullName: String = "",

    @Column(nullable = false)
    var phoneNumber: String = ""
)
