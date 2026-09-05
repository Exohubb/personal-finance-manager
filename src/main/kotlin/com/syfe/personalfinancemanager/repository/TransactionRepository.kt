package com.syfe.personalfinancemanager.repository

import com.syfe.personalfinancemanager.entity.Transaction
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.Optional

/** Database access for [Transaction] rows. */
interface TransactionRepository : JpaRepository<Transaction, Long> {

    /** All of a user's transactions, newest date first. */
    fun findByUserIdOrderByDateDesc(userId: Long): List<Transaction>

    /** A user's transactions within an inclusive date range, newest first. Backs both the transaction date filter and the report endpoints. */
    fun findByUserIdAndDateBetweenOrderByDateDesc(
        userId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Transaction>

    /** A user's transactions dated on or after the given date - the basis of savings goal progress calculation. */
    fun findByUserIdAndDateGreaterThanEqual(userId: Long, date: LocalDate): List<Transaction>

    /**
     * Looks up one transaction, scoped to a specific owner. Returning
     * empty for a transaction that exists but belongs to someone else is
     * what allows the service layer to safely respond with 404 instead of
     * ever confirming another user's data exists.
     */
    fun findByIdAndUserId(id: Long, userId: Long): Optional<Transaction>

    /** Used to block deleting a custom category that's still referenced by a transaction. */
    fun existsByCategoryId(categoryId: Long): Boolean
}
