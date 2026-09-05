package com.syfe.personalfinancemanager.service

import com.syfe.personalfinancemanager.dto.CreateGoalRequest
import com.syfe.personalfinancemanager.dto.GoalListResponse
import com.syfe.personalfinancemanager.dto.GoalResponse
import com.syfe.personalfinancemanager.dto.UpdateGoalRequest
import com.syfe.personalfinancemanager.entity.SavingsGoal
import com.syfe.personalfinancemanager.entity.TransactionType
import com.syfe.personalfinancemanager.exception.BadRequestException
import com.syfe.personalfinancemanager.exception.ResourceNotFoundException
import com.syfe.personalfinancemanager.repository.SavingsGoalRepository
import com.syfe.personalfinancemanager.repository.TransactionRepository
import com.syfe.personalfinancemanager.repository.UserRepository
import com.syfe.personalfinancemanager.util.toMoney
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Business logic for creating, reading, updating, and deleting savings
 * goals, including the live progress calculation described on
 * [calculateProgress].
 */
@Service
class SavingsGoalService(
    private val savingsGoalRepository: SavingsGoalRepository,
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository
) {

    /**
     * Creates a savings goal. If [CreateGoalRequest.startDate] is omitted,
     * it defaults to today.
     *
     * @throws com.syfe.personalfinancemanager.exception.BadRequestException if the target date isn't in the future, or the start date is after the target date
     */
    fun createGoal(userId: Long, request: CreateGoalRequest): GoalResponse {
        val targetDate = parseDate(request.targetDate)

        if (!targetDate.isAfter(LocalDate.now())) {
            throw BadRequestException("Target date must be a future date")
        }

        val startDate = request.startDate?.let { parseDate(it) } ?: LocalDate.now()

        if (startDate.isAfter(targetDate)) {
            throw BadRequestException("Start date cannot be after the target date")
        }

        val user = userRepository.getReferenceById(userId)

        val goal = SavingsGoal(
            goalName = request.goalName,
            targetAmount = request.targetAmount!!,
            targetDate = targetDate,
            startDate = startDate,
            user = user
        )

        val saved = savingsGoalRepository.save(goal)
        return saved.toResponse()
    }

    /** Lists every goal owned by the given user, each with freshly calculated progress. */
    fun getAllGoals(userId: Long): GoalListResponse {
        val goals = savingsGoalRepository.findByUserId(userId)
        return GoalListResponse(goals.map { it.toResponse() })
    }

    /**
     * Fetches a single goal with freshly calculated progress.
     *
     * @throws com.syfe.personalfinancemanager.exception.ResourceNotFoundException if no goal with this id exists for this user
     */
    fun getGoal(userId: Long, id: Long): GoalResponse {
        val goal = savingsGoalRepository.findByIdAndUserId(id, userId)
            .orElseThrow { ResourceNotFoundException("Goal not found: $id") }
        return goal.toResponse()
    }

    /**
     * Updates a goal's target amount and/or target date. [goalName] and
     * [SavingsGoal.startDate] cannot be changed through this method.
     *
     * @throws com.syfe.personalfinancemanager.exception.ResourceNotFoundException if no goal with this id exists for this user
     * @throws com.syfe.personalfinancemanager.exception.BadRequestException if a new target date is supplied but isn't in the future
     */
    @Transactional
    fun updateGoal(userId: Long, id: Long, request: UpdateGoalRequest): GoalResponse {
        val goal = savingsGoalRepository.findByIdAndUserId(id, userId)
            .orElseThrow { ResourceNotFoundException("Goal not found: $id") }

        request.targetAmount?.let { goal.targetAmount = it }
        request.targetDate?.let { newDateString ->
            val newTargetDate = parseDate(newDateString)
            if (!newTargetDate.isAfter(LocalDate.now())) {
                throw BadRequestException("Target date must be a future date")
            }
            goal.targetDate = newTargetDate
        }

        return goal.toResponse()
    }

    /**
     * Deletes a goal.
     *
     * @throws com.syfe.personalfinancemanager.exception.ResourceNotFoundException if no goal with this id exists for this user
     */
    fun deleteGoal(userId: Long, id: Long) {
        val goal = savingsGoalRepository.findByIdAndUserId(id, userId)
            .orElseThrow { ResourceNotFoundException("Goal not found: $id") }
        savingsGoalRepository.delete(goal)
    }

    /**
     * Calculates a goal's progress as `(total income - total expenses)`
     * across every one of the owning user's transactions dated on or after
     * [SavingsGoal.startDate]. This is never persisted - it's recalculated
     * from scratch on every call, so the result always reflects the
     * current state of the user's transactions with no risk of a cached
     * value going stale.
     */
    private fun calculateProgress(goal: SavingsGoal): BigDecimal {
        val transactionsSinceStart = transactionRepository
            .findByUserIdAndDateGreaterThanEqual(goal.user.id!!, goal.startDate)

        val netProgress = transactionsSinceStart.fold(BigDecimal.ZERO) { runningTotal, transaction ->
            when (transaction.type) {
                TransactionType.INCOME -> runningTotal.add(transaction.amount)
                TransactionType.EXPENSE -> runningTotal.subtract(transaction.amount)
            }
        }

        return netProgress
    }

    private fun parseDate(value: String): LocalDate {
        return try {
            LocalDate.parse(value)
        } catch (ex: DateTimeParseException) {
            throw BadRequestException("Date must be in YYYY-MM-DD format")
        }
    }

    private fun SavingsGoal.toResponse(): GoalResponse {
        val progress = calculateProgress(this)

        val roundedProgress = progress.toMoney()
        val remaining = this.targetAmount.subtract(progress).toMoney()

        val percentage = if (this.targetAmount.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal.ZERO
        } else {
            progress
                .divide(this.targetAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
                .setScale(2, RoundingMode.HALF_UP)
        }

        return GoalResponse(
            id = this.id!!,
            goalName = this.goalName,
            targetAmount = this.targetAmount.setScale(2, RoundingMode.HALF_UP),
            targetDate = this.targetDate.toString(),
            startDate = this.startDate.toString(),
            currentProgress = roundedProgress,
            progressPercentage = percentage.toDouble(),
            remainingAmount = remaining
        )
    }
}
