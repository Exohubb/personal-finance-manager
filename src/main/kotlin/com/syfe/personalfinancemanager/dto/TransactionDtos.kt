package com.syfe.personalfinancemanager.dto

import com.syfe.personalfinancemanager.entity.TransactionType
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

data class CreateTransactionRequest(
    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.00", inclusive = false, message = "Amount must be a positive value")
    val amount: BigDecimal?,

    @field:NotBlank(message = "Date is required")
    val date: String,

    @field:NotBlank(message = "Category is required")
    val category: String,

    val description: String? = null
)

data class UpdateTransactionRequest(
    @field:DecimalMin(value = "0.00", inclusive = false, message = "Amount must be a positive value")
    val amount: BigDecimal? = null,

    val category: String? = null,

    val description: String? = null
)

data class TransactionResponse(
    val id: Long,
    val amount: BigDecimal,
    val date: LocalDate,
    val category: String,
    val description: String?,
    val type: TransactionType
)

data class TransactionListResponse(
    val transactions: List<TransactionResponse>
)
