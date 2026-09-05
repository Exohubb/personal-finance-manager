package com.syfe.personalfinancemanager.service

import com.syfe.personalfinancemanager.dto.CreateGoalRequest
import com.syfe.personalfinancemanager.dto.UpdateGoalRequest
import com.syfe.personalfinancemanager.entity.Category
import com.syfe.personalfinancemanager.entity.SavingsGoal
import com.syfe.personalfinancemanager.entity.Transaction
import com.syfe.personalfinancemanager.entity.TransactionType
import com.syfe.personalfinancemanager.entity.User
import com.syfe.personalfinancemanager.exception.BadRequestException
import com.syfe.personalfinancemanager.exception.ResourceNotFoundException
import com.syfe.personalfinancemanager.repository.SavingsGoalRepository
import com.syfe.personalfinancemanager.repository.TransactionRepository
import com.syfe.personalfinancemanager.repository.UserRepository
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
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class SavingsGoalServiceTest {

    @Mock
    private lateinit var savingsGoalRepository: SavingsGoalRepository

    @Mock
    private lateinit var transactionRepository: TransactionRepository

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var savingsGoalService: SavingsGoalService

    private val testUser = User(id = 1L, username = "test@example.com")
    private val salaryCategory = Category(id = 1L, name = "Salary", type = TransactionType.INCOME, isCustom = false)
    private val foodCategory = Category(id = 2L, name = "Food", type = TransactionType.EXPENSE, isCustom = false)

    @BeforeEach
    fun setUp() {
        savingsGoalService = SavingsGoalService(savingsGoalRepository, transactionRepository, userRepository)
    }

    @Test
    fun `createGoal rejects a target date that is not in the future`() {
        val request = CreateGoalRequest(
            goalName = "Invalid Goal",
            targetAmount = BigDecimal("5000.00"),
            targetDate = LocalDate.now().toString(),
            startDate = null
        )

        assertThrows(BadRequestException::class.java) {
            savingsGoalService.createGoal(1L, request)
        }
    }

    @Test
    fun `createGoal rejects a start date that is after the target date`() {
        val request = CreateGoalRequest(
            goalName = "Invalid Dates Goal",
            targetAmount = BigDecimal("5000.00"),
            targetDate = "2026-01-01",
            startDate = "2027-01-01"
        )

        assertThrows(BadRequestException::class.java) {
            savingsGoalService.createGoal(1L, request)
        }
    }

    @Test
    fun `createGoal defaults startDate to today when not supplied`() {
        val request = CreateGoalRequest(
            goalName = "No Start Date Goal",
            targetAmount = BigDecimal("10000.00"),
            targetDate = "2027-01-01",
            startDate = null
        )
        whenever(userRepository.getReferenceById(1L)).thenReturn(testUser)
        whenever(savingsGoalRepository.save(any<SavingsGoal>())).thenAnswer {
            (it.arguments[0] as SavingsGoal).apply { id = 1L }
        }
        whenever(
            transactionRepository.findByUserIdAndDateGreaterThanEqual(org.mockito.kotlin.eq(1L), any<LocalDate>())
        ).thenReturn(emptyList())

        val response = savingsGoalService.createGoal(1L, request)

        assertEquals(LocalDate.now().toString(), response.startDate)
    }

    @Test
    fun `getGoal calculates progress as total income minus total expenses since start date`() {
        val goal = SavingsGoal(
            id = 1L,
            goalName = "Emergency Fund",
            targetAmount = BigDecimal("10000.00"),
            targetDate = LocalDate.of(2027, 1, 1),
            startDate = LocalDate.of(2024, 1, 1),
            user = testUser
        )
        whenever(savingsGoalRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(goal))

        val income = Transaction(id = 1L, amount = BigDecimal("7000.00"), date = LocalDate.of(2024, 1, 15), user = testUser, category = salaryCategory)
        val expense = Transaction(id = 2L, amount = BigDecimal("450.00"), date = LocalDate.of(2024, 1, 17), user = testUser, category = foodCategory)
        whenever(transactionRepository.findByUserIdAndDateGreaterThanEqual(1L, goal.startDate))
            .thenReturn(listOf(income, expense))

        val response = savingsGoalService.getGoal(1L, 1L)

        assertEquals(BigDecimal("6550.00"), response.currentProgress)
        assertEquals(65.5, response.progressPercentage)
        assertEquals(BigDecimal("3450.00"), response.remainingAmount)
    }

    @Test
    fun `getGoal returns zero progress when there are no transactions since start date`() {
        val goal = SavingsGoal(
            id = 2L,
            goalName = "Vacation Fund",
            targetAmount = BigDecimal("5000.00"),
            targetDate = LocalDate.of(2027, 12, 1),
            startDate = LocalDate.of(2024, 2, 1),
            user = testUser
        )
        whenever(savingsGoalRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(goal))
        whenever(transactionRepository.findByUserIdAndDateGreaterThanEqual(1L, goal.startDate))
            .thenReturn(emptyList())

        val response = savingsGoalService.getGoal(1L, 2L)

        assertEquals(BigDecimal.ZERO, response.currentProgress)
        assertEquals(0.0, response.progressPercentage)
    }

    @Test
    fun `getGoal throws ResourceNotFoundException for a goal belonging to another user`() {
        whenever(savingsGoalRepository.findByIdAndUserId(1L, 2L)).thenReturn(Optional.empty())

        assertThrows(ResourceNotFoundException::class.java) {
            savingsGoalService.getGoal(2L, 1L)
        }
    }

    @Test
    fun `updateGoal rejects updating target date to a non-future date`() {
        val goal = SavingsGoal(
            id = 1L,
            goalName = "Emergency Fund",
            targetAmount = BigDecimal("10000.00"),
            targetDate = LocalDate.of(2027, 1, 1),
            startDate = LocalDate.of(2024, 1, 1),
            user = testUser
        )
        whenever(savingsGoalRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(goal))

        val request = UpdateGoalRequest(targetDate = LocalDate.now().minusDays(1).toString())

        assertThrows(BadRequestException::class.java) {
            savingsGoalService.updateGoal(1L, 1L, request)
        }
    }
}
