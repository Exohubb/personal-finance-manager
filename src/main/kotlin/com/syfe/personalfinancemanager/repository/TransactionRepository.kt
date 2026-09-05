package com.syfe.personalfinancemanager.repository

import com.syfe.personalfinancemanager.entity.Transaction
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.Optional

interface TransactionRepository : JpaRepository<Transaction, Long> {

    fun findByUserIdOrderByDateDesc(userId: Long): List<Transaction>

    fun findByUserIdAndDateBetweenOrderByDateDesc(
        userId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Transaction>

    fun findByUserIdAndDateGreaterThanEqual(userId: Long, date: LocalDate): List<Transaction>

    fun findByIdAndUserId(id: Long, userId: Long): Optional<Transaction>

    fun existsByCategoryId(categoryId: Long): Boolean
}
