package com.syfe.personalfinancemanager.service

import com.syfe.personalfinancemanager.entity.Category
import com.syfe.personalfinancemanager.entity.Transaction
import com.syfe.personalfinancemanager.entity.TransactionType
import com.syfe.personalfinancemanager.entity.User
import com.syfe.personalfinancemanager.exception.BadRequestException
import com.syfe.personalfinancemanager.repository.TransactionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ReportServiceTest {

    @Mock
    private lateinit var transactionRepository: TransactionRepository

    private lateinit var reportService: ReportService

    private val testUser = User(id = 1L, username = "test@example.com")
    private val salaryCategory = Category(id = 1L, name = "Salary", type = TransactionType.INCOME, isCustom = false)
    private val freelanceCategory = Category(id = 3L, name = "Freelance", type = TransactionType.INCOME, isCustom = true)
    private val foodCategory = Category(id = 2L, name = "Food", type = TransactionType.EXPENSE, isCustom = false)

    @BeforeEach
    fun setUp() {
        reportService = ReportService(transactionRepository)
    }

    @Test
    fun `getMonthlyReport groups income and expenses by category and calculates net savings`() {
        val salaryTxn = Transaction(id = 1L, amount = BigDecimal("3000.00"), date = LocalDate.of(2024, 1, 5), user = testUser, category = salaryCategory)
        val freelanceTxn = Transaction(id = 2L, amount = BigDecimal("500.00"), date = LocalDate.of(2024, 1, 10), user = testUser, category = freelanceCategory)
        val foodTxn = Transaction(id = 3L, amount = BigDecimal("400.00"), date = LocalDate.of(2024, 1, 12), user = testUser, category = foodCategory)

        whenever(
            transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(1L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))
        ).thenReturn(listOf(salaryTxn, freelanceTxn, foodTxn))

        val report = reportService.getMonthlyReport(1L, 2024, 1)

        assertEquals(BigDecimal("3000.00"), report.totalIncome["Salary"])
        assertEquals(BigDecimal("500.00"), report.totalIncome["Freelance"])
        assertEquals(BigDecimal("400.00"), report.totalExpenses["Food"])
        assertEquals(BigDecimal("3100.00"), report.netSavings)
    }

    @Test
    fun `getMonthlyReport returns a plain zero net savings when there is no data`() {
        whenever(
            transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(any(), any(), any())
        ).thenReturn(emptyList())

        val report = reportService.getMonthlyReport(1L, 2024, 12)

        assertEquals(BigDecimal.ZERO, report.netSavings)
        assertEquals(emptyMap<String, BigDecimal>(), report.totalIncome)
    }

    @Test
    fun `getMonthlyReport rejects a month outside the valid 1 to 12 range`() {
        assertThrows(BadRequestException::class.java) {
            reportService.getMonthlyReport(1L, 2024, 13)
        }
        assertThrows(BadRequestException::class.java) {
            reportService.getMonthlyReport(1L, 2024, 0)
        }
    }

    @Test
    fun `getYearlyReport aggregates transactions across the whole year`() {
        val salaryTxn = Transaction(id = 1L, amount = BigDecimal("36000.00"), date = LocalDate.of(2024, 6, 1), user = testUser, category = salaryCategory)
        val foodTxn = Transaction(id = 2L, amount = BigDecimal("4800.00"), date = LocalDate.of(2024, 3, 1), user = testUser, category = foodCategory)

        whenever(
            transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(1L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))
        ).thenReturn(listOf(salaryTxn, foodTxn))

        val report = reportService.getYearlyReport(1L, 2024)

        assertEquals(BigDecimal("31200.00"), report.netSavings)
    }
}
