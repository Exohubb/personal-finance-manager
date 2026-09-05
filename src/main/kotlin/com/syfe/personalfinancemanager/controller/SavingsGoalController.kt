package com.syfe.personalfinancemanager.controller

import com.syfe.personalfinancemanager.dto.CreateGoalRequest
import com.syfe.personalfinancemanager.dto.GoalListResponse
import com.syfe.personalfinancemanager.dto.GoalResponse
import com.syfe.personalfinancemanager.dto.MessageResponse
import com.syfe.personalfinancemanager.dto.UpdateGoalRequest
import com.syfe.personalfinancemanager.security.SecurityUtils
import com.syfe.personalfinancemanager.service.SavingsGoalService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Full CRUD for savings goals. Every read endpoint here returns
 * freshly calculated progress - see
 * [com.syfe.personalfinancemanager.service.SavingsGoalService.calculateProgress].
 */
@RestController
@RequestMapping("/api/goals")
class SavingsGoalController(
    private val savingsGoalService: SavingsGoalService
) {

    /** `POST /api/goals` - creates a savings goal for the caller. */
    @PostMapping
    fun createGoal(@Valid @RequestBody request: CreateGoalRequest): ResponseEntity<GoalResponse> {
        val userId = SecurityUtils.currentUserId()
        val response = savingsGoalService.createGoal(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /** `GET /api/goals` - lists all of the caller's goals with live progress. */
    @GetMapping
    fun getAllGoals(): ResponseEntity<GoalListResponse> {
        val userId = SecurityUtils.currentUserId()
        return ResponseEntity.ok(savingsGoalService.getAllGoals(userId))
    }

    /** `GET /api/goals/{id}` - fetches one of the caller's goals with live progress. */
    @GetMapping("/{id}")
    fun getGoal(@PathVariable id: Long): ResponseEntity<GoalResponse> {
        val userId = SecurityUtils.currentUserId()
        return ResponseEntity.ok(savingsGoalService.getGoal(userId, id))
    }

    /** `PUT /api/goals/{id}` - updates target amount and/or target date on one of the caller's own goals. */
    @PutMapping("/{id}")
    fun updateGoal(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateGoalRequest
    ): ResponseEntity<GoalResponse> {
        val userId = SecurityUtils.currentUserId()
        return ResponseEntity.ok(savingsGoalService.updateGoal(userId, id, request))
    }

    /** `DELETE /api/goals/{id}` - deletes one of the caller's own goals. */
    @DeleteMapping("/{id}")
    fun deleteGoal(@PathVariable id: Long): ResponseEntity<MessageResponse> {
        val userId = SecurityUtils.currentUserId()
        savingsGoalService.deleteGoal(userId, id)
        return ResponseEntity.ok(MessageResponse("Goal deleted successfully"))
    }
}
