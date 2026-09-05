package com.syfe.personalfinancemanager.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * A label used to classify a [Transaction] as income or an expense.
 *
 * A category is either a **default** category (shared by every user,
 * seeded once at startup by
 * [com.syfe.personalfinancemanager.config.DefaultCategorySeeder], and never
 * editable or deletable) or a **custom** category owned by exactly one
 * user. Which kind a given row is depends entirely on [owner]:
 *
 * - `owner == null` -> a default category, visible to everyone.
 * - `owner != null` -> a custom category, visible only to that user.
 *
 * Custom category names only need to be unique per owner, not globally -
 * two different users may each have their own category named "Freelance".
 */
@Entity
@Table(name = "CATEGORY")
class Category(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var name: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: TransactionType = TransactionType.EXPENSE,

    @Column(nullable = false)
    var isCustom: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = true)
    var owner: User? = null
)
