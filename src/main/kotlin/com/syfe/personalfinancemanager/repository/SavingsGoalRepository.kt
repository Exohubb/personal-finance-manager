package com.syfe.personalfinancemanager.repository

import com.syfe.personalfinancemanager.entity.SavingsGoal
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

/** Database access for [SavingsGoal] rows. */
interface SavingsGoalRepository : JpaRepository<SavingsGoal, Long> {

    /** All of a user's savings goals. */
    fun findByUserId(userId: Long): List<SavingsGoal>

    /**
     * Looks up one goal, scoped to a specific owner - the same
     * ownership-check pattern as [TransactionRepository.findByIdAndUserId],
     * ensuring a user can never read, update, or delete another user's goal.
     */
    fun findByIdAndUserId(id: Long, userId: Long): Optional<SavingsGoal>
}
