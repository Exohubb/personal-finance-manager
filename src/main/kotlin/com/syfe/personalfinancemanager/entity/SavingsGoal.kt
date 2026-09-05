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
