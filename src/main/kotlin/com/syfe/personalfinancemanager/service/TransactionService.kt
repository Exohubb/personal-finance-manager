package com.syfe.personalfinancemanager.service

import com.syfe.personalfinancemanager.dto.CreateTransactionRequest
import com.syfe.personalfinancemanager.dto.TransactionListResponse
import com.syfe.personalfinancemanager.dto.TransactionResponse
import com.syfe.personalfinancemanager.dto.UpdateTransactionRequest
import com.syfe.personalfinancemanager.entity.Transaction
import com.syfe.personalfinancemanager.entity.TransactionType
import com.syfe.personalfinancemanager.exception.BadRequestException
import com.syfe.personalfinancemanager.exception.ResourceNotFoundException
import com.syfe.personalfinancemanager.repository.CategoryRepository
import com.syfe.personalfinancemanager.repository.TransactionRepository
import com.syfe.personalfinancemanager.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Service
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository
) {

    fun createTransaction(userId: Long, request: CreateTransactionRequest): TransactionResponse {
        val date = parseDate(request.date)

        if (date.isAfter(LocalDate.now())) {
            throw BadRequestException("Transaction date cannot be in the future")
        }

        val category = categoryRepository
            .findVisibleCategoryByName(request.category, userId)
            .orElseThrow { BadRequestException("Category not found or not accessible: ${request.category}") }

        val user = userRepository.getReferenceById(userId)

        val transaction = Transaction(
            amount = request.amount!!,
            date = date,
            description = request.description,
            user = user,
            category = category
        )

        val saved = transactionRepository.save(transaction)
        return saved.toResponse()
    }

    fun getTransactions(
        userId: Long,
        startDate: String?,
        endDate: String?,
        category: String?,
        type: String?
    ): TransactionListResponse {
        var transactions = transactionRepository.findByUserIdOrderByDateDesc(userId)

        if (startDate != null || endDate != null) {
            val from = startDate?.let { parseDate(it) } ?: LocalDate.MIN
            val to = endDate?.let { parseDate(it) } ?: LocalDate.MAX
            transactions = transactions.filter { it.date >= from && it.date <= to }
        }

        if (category != null) {
            transactions = transactions.filter { it.category.name.equals(category, ignoreCase = true) }
        }

        if (type != null) {
            val requestedType = try {
                TransactionType.valueOf(type.uppercase())
            } catch (ex: IllegalArgumentException) {
                throw BadRequestException("Invalid transaction type: $type")
            }
            transactions = transactions.filter { it.type == requestedType }
        }

        return TransactionListResponse(transactions.map { it.toResponse() })
    }

    @Transactional
    fun updateTransaction(userId: Long, id: Long, request: UpdateTransactionRequest): TransactionResponse {
        val transaction = transactionRepository.findByIdAndUserId(id, userId)
            .orElseThrow { ResourceNotFoundException("Transaction not found: $id") }

        request.amount?.let { transaction.amount = it }
        request.description?.let { transaction.description = it }
        request.category?.let { newCategoryName ->
            val newCategory = categoryRepository
                .findVisibleCategoryByName(newCategoryName, userId)
                .orElseThrow { BadRequestException("Category not found or not accessible: $newCategoryName") }
            transaction.category = newCategory
        }

        return transaction.toResponse()
    }

    fun deleteTransaction(userId: Long, id: Long) {
        val transaction = transactionRepository.findByIdAndUserId(id, userId)
            .orElseThrow { ResourceNotFoundException("Transaction not found: $id") }

        transactionRepository.delete(transaction)
    }

    private fun parseDate(value: String): LocalDate {
        return try {
            LocalDate.parse(value)
        } catch (ex: DateTimeParseException) {
            throw BadRequestException("Date must be in YYYY-MM-DD format")
        }
    }

    private fun Transaction.toResponse() = TransactionResponse(
        id = this.id!!,
        amount = this.amount,
        date = this.date,
        category = this.category.name,
        description = this.description,
        type = this.type
    )
}
