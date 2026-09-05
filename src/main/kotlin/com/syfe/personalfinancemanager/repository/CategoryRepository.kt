package com.syfe.personalfinancemanager.repository

import com.syfe.personalfinancemanager.entity.Category
import com.syfe.personalfinancemanager.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

/** Database access for [Category] rows, both default and custom. */
interface CategoryRepository : JpaRepository<Category, Long> {

    /**
     * Every category a given user is allowed to use: the shared default
     * categories (`owner IS NULL`) plus that user's own custom ones.
     */
    fun findByOwnerIsNullOrOwnerId(ownerId: Long): List<Category>

    /**
     * Resolves a category name (as sent in a transaction request) to the
     * real [Category] row, scoped to what the given user can actually see.
     *
     * Written as an explicit JPQL query rather than a derived method name
     * because the equivalent name
     * (`findByNameAndOwnerIsNullOrNameAndOwnerId`) is ambiguous to Spring
     * Data's method-name parser - it expects the `name` condition to be
     * supplied twice, once per side of the `Or`.
     */
    @Query("SELECT c FROM Category c WHERE c.name = :name AND (c.owner IS NULL OR c.owner.id = :ownerId)")
    fun findVisibleCategoryByName(
        @Param("name") name: String,
        @Param("ownerId") ownerId: Long
    ): Optional<Category>

    /** Used before creating a custom category, to enforce per-user name uniqueness. */
    fun existsByNameAndOwner(name: String, owner: User): Boolean

    /**
     * Looks up a category by name, but only matches if it's both owned by
     * this user and marked custom - default categories never match here,
     * which is what makes them impossible to delete through this lookup.
     */
    fun findByNameAndOwnerIdAndIsCustomTrue(name: String, ownerId: Long): Optional<Category>
}
