package com.syfe.personalfinancemanager.service

import com.syfe.personalfinancemanager.dto.MonthlyReportResponse
import com.syfe.personalfinancemanager.dto.YearlyReportResponse
import com.syfe.personalfinancemanager.entity.Transaction
import com.syfe.personalfinancemanager.entity.TransactionType
import com.syfe.personalfinancemanager.exception.BadRequestException
import com.syfe.personalfinancemanager.repository.TransactionRepository
import com.syfe.personalfinancemanager.util.toMoney
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/** Builds monthly and yearly income/expense summaries from a user's transactions. */
@Service
class ReportService(
    private val transactionRepository: TransactionRepository
) {

    /**
     * Builds an income/expense breakdown for a single calendar month,
     * grouped by category name, with a net savings total.
     *
     * @throws com.syfe.personalfinancemanager.exception.BadRequestException if [month] is outside the 1-12 range
     */
    fun getMonthlyReport(userId: Long, year: Int, month: Int): MonthlyReportResponse {
        if (month < 1 || month > 12) {
            throw BadRequestException("Month must be between 1 and 12")
        }

        val start = LocalDate.of(year, month, 1)
        val end = start.withDayOfMonth(start.lengthOfMonth())

        val transactions = transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, start, end)
        return buildMonthlyReport(month, year, transactions)
    }

    /**
     * Builds an income/expense breakdown for a full calendar year,
     * grouped by category name, with a net savings total. Uses the same
     * aggregation logic as [getMonthlyReport], just across the whole year.
     */
    fun getYearlyReport(userId: Long, year: Int): YearlyReportResponse {
        val start = LocalDate.of(year, 1, 1)
        val end = LocalDate.of(year, 12, 31)

        val transactions = transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, start, end)
        return buildYearlyReport(year, transactions)
    }

    private fun groupByCategoryTotals(transactions: List<Transaction>, type: TransactionType): Map<String, BigDecimal> {
        return transactions
            .filter { it.type == type }
            .groupBy { it.category.name }
            .mapValues { (_, txns) ->
                txns.fold(BigDecimal.ZERO) { total, txn -> total.add(txn.amount) }
                    .setScale(2, RoundingMode.HALF_UP)
            }
    }

    private fun buildMonthlyReport(month: Int, year: Int, transactions: List<Transaction>): MonthlyReportResponse {
        val income = groupByCategoryTotals(transactions, TransactionType.INCOME)
        val expenses = groupByCategoryTotals(transactions, TransactionType.EXPENSE)
        val netSavings = calculateNetSavings(income, expenses)

        return MonthlyReportResponse(
            month = month,
            year = year,
            totalIncome = income,
            totalExpenses = expenses,
            netSavings = netSavings
        )
    }

    private fun buildYearlyReport(year: Int, transactions: List<Transaction>): YearlyReportResponse {
        val income = groupByCategoryTotals(transactions, TransactionType.INCOME)
        val expenses = groupByCategoryTotals(transactions, TransactionType.EXPENSE)
        val netSavings = calculateNetSavings(income, expenses)

        return YearlyReportResponse(
            year = year,
            totalIncome = income,
            totalExpenses = expenses,
            netSavings = netSavings
        )
    }

    private fun calculateNetSavings(income: Map<String, BigDecimal>, expenses: Map<String, BigDecimal>): BigDecimal {
        val totalIncome = income.values.fold(BigDecimal.ZERO) { total, amount -> total.add(amount) }
        val totalExpenses = expenses.values.fold(BigDecimal.ZERO) { total, amount -> total.add(amount) }
        return totalIncome.subtract(totalExpenses).toMoney()
    }
}
