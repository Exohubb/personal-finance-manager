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
 * A single record of money moving in or out for one user, on one date,
 * against one [Category].
 *
 * [amount] is stored as [BigDecimal] rather than a floating point type,
 * since binary floating point numbers cannot represent every decimal value
 * exactly and would introduce rounding errors in currency calculations.
 *
 * Deleting a transaction is a hard delete (the row is removed outright, see
 * [com.syfe.personalfinancemanager.service.TransactionService.deleteTransaction]) -
 * there is no soft-delete flag, because every savings goal progress
 * calculation and every report reads directly from whichever transaction
 * rows currently exist.
 */
@Entity
@Table(name = "TRANSACTION")
class Transaction(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    var amount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var date: LocalDate = LocalDate.now(),

    var description: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = User(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    var category: Category = Category(),

) {
    /**
     * The transaction's type, derived from its [category] rather than
     * stored as its own column - a transaction is always income or an
     * expense according to whatever category it belongs to.
     */
    val type get() = category.type
}
