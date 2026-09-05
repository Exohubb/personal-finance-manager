package com.syfe.personalfinancemanager.dto

import com.syfe.personalfinancemanager.entity.TransactionType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateCategoryRequest(
    @field:NotBlank(message = "Category name is required")
    val name: String,

    @field:NotNull(message = "Category type is required")
    val type: TransactionType
)

data class CategoryResponse(
    val name: String,
    val type: TransactionType,
    val custom: Boolean
)

data class CategoryListResponse(
    val categories: List<CategoryResponse>
)
