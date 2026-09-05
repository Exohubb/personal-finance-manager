package com.syfe.personalfinancemanager.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class CreateGoalRequest(
    @field:NotBlank(message = "Goal name is required")
    val goalName: String,

    @field:NotNull(message = "Target amount is required")
    @field:DecimalMin(value = "0.00", inclusive = false, message = "Target amount must be a positive value")
    val targetAmount: BigDecimal?,

    @field:NotBlank(message = "Target date is required")
    val targetDate: String,

    val startDate: String? = null
)

data class UpdateGoalRequest(
    @field:DecimalMin(value = "0.00", inclusive = false, message = "Target amount must be a positive value")
    val targetAmount: BigDecimal? = null,

    val targetDate: String? = null
)

data class GoalResponse(
    val id: Long,
    val goalName: String,
    val targetAmount: BigDecimal,
    val targetDate: String,
    val startDate: String,
    val currentProgress: BigDecimal,
    val progressPercentage: Double,
    val remainingAmount: BigDecimal
)

data class GoalListResponse(
    val goals: List<GoalResponse>
)
