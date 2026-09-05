package com.syfe.personalfinancemanager.controller

import com.syfe.personalfinancemanager.dto.CreateTransactionRequest
import com.syfe.personalfinancemanager.dto.MessageResponse
import com.syfe.personalfinancemanager.dto.TransactionListResponse
import com.syfe.personalfinancemanager.dto.TransactionResponse
import com.syfe.personalfinancemanager.dto.UpdateTransactionRequest
import com.syfe.personalfinancemanager.security.SecurityUtils
import com.syfe.personalfinancemanager.service.TransactionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Full CRUD for income/expense transactions, scoped to the logged-in user. */
@RestController
@RequestMapping("/api/transactions")
class TransactionController(
    private val transactionService: TransactionService
) {

    /** `POST /api/transactions` - creates a transaction for the caller. */
    @PostMapping
    fun createTransaction(@Valid @RequestBody request: CreateTransactionRequest): ResponseEntity<TransactionResponse> {
        val userId = SecurityUtils.currentUserId()
        val response = transactionService.createTransaction(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * `GET /api/transactions` - lists the caller's transactions, newest
     * first. All query parameters are optional and may be combined:
     * [startDate]/[endDate] (`YYYY-MM-DD`), [category] (by name), and
     * [type] (`INCOME` or `EXPENSE`).
     */
    @GetMapping
    fun getTransactions(
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) type: String?
    ): ResponseEntity<TransactionListResponse> {
        val userId = SecurityUtils.currentUserId()
        val response = transactionService.getTransactions(userId, startDate, endDate, category, type)
        return ResponseEntity.ok(response)
    }

    /**
     * `PUT /api/transactions/{id}` - updates amount, category, and/or
     * description on one of the caller's own transactions. Any `date`
     * field in the request body is silently ignored - see
     * [com.syfe.personalfinancemanager.dto.UpdateTransactionRequest].
     */
    @PutMapping("/{id}")
    fun updateTransaction(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateTransactionRequest
    ): ResponseEntity<TransactionResponse> {
        val userId = SecurityUtils.currentUserId()
        val response = transactionService.updateTransaction(userId, id, request)
        return ResponseEntity.ok(response)
    }

    /** `DELETE /api/transactions/{id}` - permanently deletes one of the caller's own transactions. */
    @DeleteMapping("/{id}")
    fun deleteTransaction(@PathVariable id: Long): ResponseEntity<MessageResponse> {
        val userId = SecurityUtils.currentUserId()
        transactionService.deleteTransaction(userId, id)
        return ResponseEntity.ok(MessageResponse("Transaction deleted successfully"))
    }
}
