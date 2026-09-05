package com.syfe.personalfinancemanager.repository

import com.syfe.personalfinancemanager.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

/** Database access for [User] accounts. */
interface UserRepository : JpaRepository<User, Long> {

    /** Looks up a user by their login identifier (email address). */
    fun findByUsername(username: String): Optional<User>

    /** Used at registration time to reject an email that's already taken. */
    fun existsByUsername(username: String): Boolean
}
