package com.syfe.personalfinancemanager.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

/**
 * A savings target a user is tracking progress toward.
 *
 * Notably, this entity does **not** store progress, percentage complete, or
 * remaining amount anywhere. Those values depend on the user's transaction
 * history, which can change at any time, so storing them would go stale
 * immediately. Instead,
 * [com.syfe.personalfinancemanager.service.SavingsGoalService] recalculates
 * progress fresh every time a goal is read, as
 * `(total income - total expenses)` for every transaction dated on or
 * after [startDate].
 */
@Entity
@Table(name = "SAVINGS_GOAL")
class SavingsGoal(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var goalName: String = "",

    @Column(nullable = false)
    var targetAmount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var targetDate: LocalDate = LocalDate.now(),

    @Column(nullable = false)
    var startDate: LocalDate = LocalDate.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = User()
)
