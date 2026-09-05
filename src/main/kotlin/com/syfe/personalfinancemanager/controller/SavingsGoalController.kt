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

@RestController
@RequestMapping("/api/goals")
class SavingsGoalController(
    private val savingsGoalService: SavingsGoalService
) {

    @PostMapping
    fun createGoal(@Valid @RequestBody request: CreateGoalRequest): ResponseEntity<GoalResponse> {
        val userId = SecurityUtils.currentUserId()
        val response = savingsGoalService.createGoal(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    fun getAllGoals(): ResponseEntity<GoalListResponse> {
        val userId = SecurityUtils.currentUserId()
        return ResponseEntity.ok(savingsGoalService.getAllGoals(userId))
    }

    @GetMapping("/{id}")
    fun getGoal(@PathVariable id: Long): ResponseEntity<GoalResponse> {
        val userId = SecurityUtils.currentUserId()
        return ResponseEntity.ok(savingsGoalService.getGoal(userId, id))
    }

    @PutMapping("/{id}")
    fun updateGoal(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateGoalRequest
    ): ResponseEntity<GoalResponse> {
        val userId = SecurityUtils.currentUserId()
        return ResponseEntity.ok(savingsGoalService.updateGoal(userId, id, request))
    }

    @DeleteMapping("/{id}")
    fun deleteGoal(@PathVariable id: Long): ResponseEntity<MessageResponse> {
        val userId = SecurityUtils.currentUserId()
        savingsGoalService.deleteGoal(userId, id)
        return ResponseEntity.ok(MessageResponse("Goal deleted successfully"))
    }
}
