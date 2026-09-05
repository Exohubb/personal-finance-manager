package com.syfe.personalfinancemanager.dto

import java.math.BigDecimal

/** Response shape for `GET /api/reports/monthly/{year}/{month}`. */
data class MonthlyReportResponse(
    val month: Int,
    val year: Int,
    val totalIncome: Map<String, BigDecimal>,
    val totalExpenses: Map<String, BigDecimal>,
    val netSavings: BigDecimal
)

/** Response shape for `GET /api/reports/yearly/{year}`. */
data class YearlyReportResponse(
    val year: Int,
    val totalIncome: Map<String, BigDecimal>,
    val totalExpenses: Map<String, BigDecimal>,
    val netSavings: BigDecimal
)
