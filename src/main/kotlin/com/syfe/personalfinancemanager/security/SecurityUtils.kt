package com.syfe.personalfinancemanager.security

import org.springframework.security.core.context.SecurityContextHolder

object SecurityUtils {

    fun currentUserId(): Long {
        val principal = SecurityContextHolder.getContext().authentication!!.principal as AppUserPrincipal
        return principal.id
    }
}
