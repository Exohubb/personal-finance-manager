package com.syfe.personalfinancemanager.repository

import com.syfe.personalfinancemanager.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {

    fun findByUsername(username: String): Optional<User>

    fun existsByUsername(username: String): Boolean
}
