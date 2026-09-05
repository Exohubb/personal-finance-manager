package com.syfe.personalfinancemanager.controller

import com.syfe.personalfinancemanager.dto.CategoryListResponse
import com.syfe.personalfinancemanager.dto.CategoryResponse
import com.syfe.personalfinancemanager.dto.CreateCategoryRequest
import com.syfe.personalfinancemanager.dto.MessageResponse
import com.syfe.personalfinancemanager.security.SecurityUtils
import com.syfe.personalfinancemanager.service.CategoryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Listing, creating, and deleting income/expense categories. */
@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val categoryService: CategoryService
) {

    /** `GET /api/categories` - lists default categories plus the caller's own custom ones. */
    @GetMapping
    fun getAllCategories(): ResponseEntity<CategoryListResponse> {
        val userId = SecurityUtils.currentUserId()
        return ResponseEntity.ok(categoryService.getAllCategoriesForUser(userId))
    }

    /** `POST /api/categories` - creates a custom category owned by the caller. */
    @PostMapping
    fun createCategory(@Valid @RequestBody request: CreateCategoryRequest): ResponseEntity<CategoryResponse> {
        val userId = SecurityUtils.currentUserId()
        val response = categoryService.createCustomCategory(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /** `DELETE /api/categories/{name}` - deletes one of the caller's own custom categories, by name. */
    @DeleteMapping("/{name}")
    fun deleteCategory(@PathVariable name: String): ResponseEntity<MessageResponse> {
        val userId = SecurityUtils.currentUserId()
        categoryService.deleteCustomCategory(userId, name)
        return ResponseEntity.ok(MessageResponse("Category deleted successfully"))
    }
}
