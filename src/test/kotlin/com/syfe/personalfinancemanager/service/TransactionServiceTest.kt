package com.syfe.personalfinancemanager.service

import com.syfe.personalfinancemanager.dto.CreateTransactionRequest
import com.syfe.personalfinancemanager.dto.UpdateTransactionRequest
import com.syfe.personalfinancemanager.entity.Category
import com.syfe.personalfinancemanager.entity.Transaction
import com.syfe.personalfinancemanager.entity.TransactionType
import com.syfe.personalfinancemanager.entity.User
import com.syfe.personalfinancemanager.exception.BadRequestException
import com.syfe.personalfinancemanager.exception.ResourceNotFoundException
import com.syfe.personalfinancemanager.repository.CategoryRepository
import com.syfe.personalfinancemanager.repository.TransactionRepository
import com.syfe.personalfinancemanager.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class TransactionServiceTest {

    @Mock
    private lateinit var transactionRepository: TransactionRepository

    @Mock
    private lateinit var categoryRepository: CategoryRepository

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var transactionService: TransactionService

    private val testUser = User(id = 1L, username = "test@example.com")
    private val salaryCategory = Category(id = 1L, name = "Salary", type = TransactionType.INCOME, isCustom = false)
    private val foodCategory = Category(id = 2L, name = "Food", type = TransactionType.EXPENSE, isCustom = false)

    @BeforeEach
    fun setUp() {
        transactionService = TransactionService(transactionRepository, categoryRepository, userRepository)
    }

    @Test
    fun `createTransaction saves a valid income transaction and returns INCOME type`() {
        val request = CreateTransactionRequest(
            amount = BigDecimal("5000.00"),
            date = "2024-01-15",
            category = "Salary",
            description = "January Salary"
        )

        whenever(categoryRepository.findVisibleCategoryByName("Salary", 1L))
            .thenReturn(Optional.of(salaryCategory))
        whenever(userRepository.getReferenceById(1L)).thenReturn(testUser)
        whenever(transactionRepository.save(any<Transaction>())).thenAnswer {
            (it.arguments[0] as Transaction).apply { id = 1L }
        }

        val response = transactionService.createTransaction(1L, request)

        assertEquals(BigDecimal("5000.00"), response.amount)
        assertEquals("Salary", response.category)
        assertEquals(TransactionType.INCOME, response.type)
    }

    @Test
    fun `createTransaction rejects a future date`() {
        val futureDate = LocalDate.now().plusDays(1).toString()
        val request = CreateTransactionRequest(
            amount = BigDecimal("100.00"),
            date = futureDate,
            category = "Food",
            description = null
        )

        assertThrows(BadRequestException::class.java) {
            transactionService.createTransaction(1L, request)
        }

        verify(transactionRepository, org.mockito.Mockito.never()).save(any())
    }

    @Test
    fun `createTransaction rejects an unknown or inaccessible category`() {
        val request = CreateTransactionRequest(
            amount = BigDecimal("100.00"),
            date = "2024-01-15",
            category = "NotARealCategory",
            description = null
        )

        whenever(categoryRepository.findVisibleCategoryByName("NotARealCategory", 1L))
            .thenReturn(Optional.empty())

        assertThrows(BadRequestException::class.java) {
            transactionService.createTransaction(1L, request)
        }
    }

    @Test
    fun `createTransaction rejects a badly formatted date`() {
        val request = CreateTransactionRequest(
            amount = BigDecimal("100.00"),
            date = "15-01-2024",
            category = "Food",
            description = null
        )

        assertThrows(BadRequestException::class.java) {
            transactionService.createTransaction(1L, request)
        }
    }

    @Test
    fun `updateTransaction ignores any attempt to change the date`() {
        val existing = Transaction(
            id = 10L,
            amount = BigDecimal("100.00"),
            date = LocalDate.of(2024, 1, 17),
            description = "Groceries",
            user = testUser,
            category = foodCategory
        )
        whenever(transactionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(existing))

        val request = UpdateTransactionRequest(amount = BigDecimal("450.00"), description = "Trying to change date")

        val response = transactionService.updateTransaction(1L, 10L, request)

        assertEquals(LocalDate.of(2024, 1, 17), response.date)
        assertEquals(BigDecimal("450.00"), response.amount)
        assertEquals("Trying to change date", response.description)
    }

    @Test
    fun `updateTransaction throws ResourceNotFoundException when transaction does not belong to user`() {
        whenever(transactionRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty())

        assertThrows(ResourceNotFoundException::class.java) {
            transactionService.updateTransaction(1L, 999L, UpdateTransactionRequest(amount = BigDecimal("1.00")))
        }
    }

    @Test
    fun `deleteTransaction removes the transaction when it belongs to the user`() {
        val existing = Transaction(
            id = 5L,
            amount = BigDecimal("50.00"),
            date = LocalDate.of(2024, 1, 1),
            user = testUser,
            category = foodCategory
        )
        whenever(transactionRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(existing))

        transactionService.deleteTransaction(1L, 5L)

        verify(transactionRepository).delete(existing)
    }

    @Test
    fun `deleteTransaction throws ResourceNotFoundException for a transaction owned by another user`() {
        whenever(transactionRepository.findByIdAndUserId(5L, 2L)).thenReturn(Optional.empty())

        assertThrows(ResourceNotFoundException::class.java) {
            transactionService.deleteTransaction(2L, 5L)
        }
    }

    @Test
    fun `getTransactions filters by category name case-insensitively`() {
        val salaryTxn = Transaction(id = 1L, amount = BigDecimal("100"), date = LocalDate.of(2024, 1, 1), user = testUser, category = salaryCategory)
        val foodTxn = Transaction(id = 2L, amount = BigDecimal("50"), date = LocalDate.of(2024, 1, 2), user = testUser, category = foodCategory)
        whenever(transactionRepository.findByUserIdOrderByDateDesc(1L)).thenReturn(listOf(foodTxn, salaryTxn))

        val result = transactionService.getTransactions(1L, null, null, "salary", null)

        assertEquals(1, result.transactions.size)
        assertEquals("Salary", result.transactions[0].category)
    }
}
