package com.syfe.personalfinancemanager.security

import org.springframework.security.core.context.SecurityContextHolder

/** Helper for reading who's making the current request. */
object SecurityUtils {

    /**
     * Returns the database id of the currently logged-in user, read
     * straight from the Spring Security session context - never from
     * anything a client could send in a request body. This is what makes
     * data isolation between users enforceable: every service method takes
     * a `userId` sourced only from here.
     */
    fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication!!.principal as AppUserPrincipal
        return principal.id
    }
}
