package com.syfe.personalfinancemanager.entity

/**
 * The two kinds of money movement tracked by this application.
 *
 * Used both as a [Category]'s type and as a [Transaction]'s derived type -
 * a transaction's type always matches the category it's filed under.
 */
enum class TransactionType {
    INCOME,
    EXPENSE
}
