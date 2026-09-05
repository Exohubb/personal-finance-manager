package com.syfe.personalfinancemanager.service

import com.syfe.personalfinancemanager.dto.CreateCategoryRequest
import com.syfe.personalfinancemanager.entity.Category
import com.syfe.personalfinancemanager.entity.TransactionType
import com.syfe.personalfinancemanager.entity.User
import com.syfe.personalfinancemanager.exception.AppAccessDeniedException
import com.syfe.personalfinancemanager.exception.BadRequestException
import com.syfe.personalfinancemanager.exception.ConflictException
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
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class CategoryServiceTest {

    @Mock
    private lateinit var categoryRepository: CategoryRepository

    @Mock
    private lateinit var transactionRepository: TransactionRepository

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var categoryService: CategoryService

    private val owner = User(id = 1L, username = "owner@example.com")

    @BeforeEach
    fun setUp() {
        categoryService = CategoryService(categoryRepository, transactionRepository, userRepository)
    }

    @Test
    fun `createCustomCategory rejects a duplicate name for the same user`() {
        val request = CreateCategoryRequest(name = "Freelance", type = TransactionType.INCOME)
        whenever(userRepository.getReferenceById(1L)).thenReturn(owner)
        whenever(categoryRepository.existsByNameAndOwner("Freelance", owner)).thenReturn(true)

        assertThrows(ConflictException::class.java) {
            categoryService.createCustomCategory(1L, request)
        }
    }

    @Test
    fun `createCustomCategory saves a new category marked as custom`() {
        val request = CreateCategoryRequest(name = "Freelance", type = TransactionType.INCOME)
        whenever(userRepository.getReferenceById(1L)).thenReturn(owner)
        whenever(categoryRepository.existsByNameAndOwner("Freelance", owner)).thenReturn(false)
        whenever(categoryRepository.save(any<Category>())).thenAnswer { it.arguments[0] as Category }

        val response = categoryService.createCustomCategory(1L, request)

        assertEquals("Freelance", response.name)
        assertEquals(true, response.custom)
    }

    @Test
    fun `deleteCustomCategory blocks deletion when the category is used by a transaction`() {
        val category = Category(id = 5L, name = "Freelance", type = TransactionType.INCOME, isCustom = true, owner = owner)
        whenever(categoryRepository.findByNameAndOwnerIdAndIsCustomTrue("Freelance", 1L))
            .thenReturn(Optional.of(category))
        whenever(transactionRepository.existsByCategoryId(5L)).thenReturn(true)

        assertThrows(BadRequestException::class.java) {
            categoryService.deleteCustomCategory(1L, "Freelance")
        }
    }

    @Test
    fun `deleteCustomCategory rejects deleting a default category`() {
        val defaultCategory = Category(id = 2L, name = "Food", type = TransactionType.EXPENSE, isCustom = false, owner = null)
        whenever(categoryRepository.findByNameAndOwnerIdAndIsCustomTrue("Food", 1L)).thenReturn(Optional.empty())
        whenever(categoryRepository.findAll()).thenReturn(listOf(defaultCategory))

        assertThrows(BadRequestException::class.java) {
            categoryService.deleteCustomCategory(1L, "Food")
        }
    }

    @Test
    fun `deleteCustomCategory throws AppAccessDeniedException for another user's custom category`() {
        val otherUser = User(id = 2L, username = "other@example.com")
        val othersCategory = Category(id = 6L, name = "GymMembership", type = TransactionType.EXPENSE, isCustom = true, owner = otherUser)

        whenever(categoryRepository.findByNameAndOwnerIdAndIsCustomTrue("GymMembership", 1L)).thenReturn(Optional.empty())
        whenever(categoryRepository.findAll()).thenReturn(listOf(othersCategory))

        assertThrows(AppAccessDeniedException::class.java) {
            categoryService.deleteCustomCategory(1L, "GymMembership")
        }
    }

    @Test
    fun `deleteCustomCategory throws ResourceNotFoundException for a category that does not exist at all`() {
        whenever(categoryRepository.findByNameAndOwnerIdAndIsCustomTrue("DoesNotExist", 1L)).thenReturn(Optional.empty())
        whenever(categoryRepository.findAll()).thenReturn(emptyList())

        assertThrows(ResourceNotFoundException::class.java) {
            categoryService.deleteCustomCategory(1L, "DoesNotExist")
        }
    }
}
