package com.syfe.personalfinancemanager.dto

import java.math.BigDecimal

data class MonthlyReportResponse(
    val month: Int,
    val year: Int,
    val totalIncome: Map<String, BigDecimal>,
    val totalExpenses: Map<String, BigDecimal>,
    val netSavings: BigDecimal
)

data class YearlyReportResponse(
    val year: Int,
    val totalIncome: Map<String, BigDecimal>,
    val totalExpenses: Map<String, BigDecimal>,
    val netSavings: BigDecimal
)
