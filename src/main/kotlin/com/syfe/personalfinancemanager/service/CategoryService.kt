package com.syfe.personalfinancemanager.service

import com.syfe.personalfinancemanager.dto.CategoryListResponse
import com.syfe.personalfinancemanager.dto.CategoryResponse
import com.syfe.personalfinancemanager.dto.CreateCategoryRequest
import com.syfe.personalfinancemanager.entity.Category
import com.syfe.personalfinancemanager.exception.AppAccessDeniedException
import com.syfe.personalfinancemanager.exception.BadRequestException
import com.syfe.personalfinancemanager.exception.ConflictException
import com.syfe.personalfinancemanager.exception.ResourceNotFoundException
import com.syfe.personalfinancemanager.repository.CategoryRepository
import com.syfe.personalfinancemanager.repository.TransactionRepository
import com.syfe.personalfinancemanager.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository
) {

    fun getAllCategoriesForUser(userId: Long): CategoryListResponse {
        val categories = categoryRepository.findByOwnerIsNullOrOwnerId(userId)
        return CategoryListResponse(categories.map { it.toResponse() })
    }

    fun createCustomCategory(userId: Long, request: CreateCategoryRequest): CategoryResponse {
        val owner = userRepository.getReferenceById(userId)

        if (categoryRepository.existsByNameAndOwner(request.name, owner)) {
            throw ConflictException("A category with this name already exists")
        }

        val category = Category(
            name = request.name,
            type = request.type,
            isCustom = true,
            owner = owner
        )

        val saved = categoryRepository.save(category)
        return saved.toResponse()
    }

    fun deleteCustomCategory(userId: Long, name: String) {
        val ownedCustomCategory = categoryRepository.findByNameAndOwnerIdAndIsCustomTrue(name, userId)

        if (ownedCustomCategory.isPresent) {
            val category = ownedCustomCategory.get()
            if (transactionRepository.existsByCategoryId(category.id!!)) {
                throw BadRequestException("Cannot delete a category that is used by existing transactions")
            }
            categoryRepository.delete(category)
            return
        }

        val anyMatch = categoryRepository.findAll().firstOrNull { it.name == name }

        when {
            anyMatch == null -> throw ResourceNotFoundException("Category not found: $name")
            !anyMatch.isCustom -> throw BadRequestException("Default categories cannot be deleted")
            else -> throw AppAccessDeniedException("You do not have permission to delete this category")
        }
    }

    private fun Category.toResponse() = CategoryResponse(
        name = this.name,
        type = this.type,
        custom = this.isCustom
    )
}
