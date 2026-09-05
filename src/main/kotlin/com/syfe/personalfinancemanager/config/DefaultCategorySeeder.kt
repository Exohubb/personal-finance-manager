package com.syfe.personalfinancemanager.config

import com.syfe.personalfinancemanager.entity.Category
import com.syfe.personalfinancemanager.entity.TransactionType
import com.syfe.personalfinancemanager.repository.CategoryRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

/**
 * Seeds the seven default categories (`Salary` income; `Food`, `Rent`,
 * `Transportation`, `Entertainment`, `Healthcare`, `Utilities` expense)
 * once, automatically, on application startup - satisfying the requirement
 * that these exist for every user without any manual setup step.
 */
@Component
class DefaultCategorySeeder(
    private val categoryRepository: CategoryRepository
) : CommandLineRunner {

    /**
     * Runs once, automatically, after the Spring application context has
     * fully started - the standard [CommandLineRunner] contract. Skips
     * seeding entirely if any category already exists, so restarting the
     * app never creates duplicate default categories.
     */
    override fun run(vararg args: String) {
        if (categoryRepository.count() > 0) {
            return
        }

        val defaults = listOf(
            Category(name = "Salary", type = TransactionType.INCOME, isCustom = false, owner = null),
            Category(name = "Food", type = TransactionType.EXPENSE, isCustom = false, owner = null),
            Category(name = "Rent", type = TransactionType.EXPENSE, isCustom = false, owner = null),
            Category(name = "Transportation", type = TransactionType.EXPENSE, isCustom = false, owner = null),
            Category(name = "Entertainment", type = TransactionType.EXPENSE, isCustom = false, owner = null),
            Category(name = "Healthcare", type = TransactionType.EXPENSE, isCustom = false, owner = null),
            Category(name = "Utilities", type = TransactionType.EXPENSE, isCustom = false, owner = null)
        )

        categoryRepository.saveAll(defaults)
    }
}
