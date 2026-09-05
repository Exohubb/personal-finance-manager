package com.syfe.personalfinancemanager.repository

import com.syfe.personalfinancemanager.entity.SavingsGoal
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface SavingsGoalRepository : JpaRepository<SavingsGoal, Long> {

    fun findByUserId(userId: Long): List<SavingsGoal>

    fun findByIdAndUserId(id: Long, userId: Long): Optional<SavingsGoal>
}
