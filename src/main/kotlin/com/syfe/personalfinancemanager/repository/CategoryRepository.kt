package com.syfe.personalfinancemanager.repository

import com.syfe.personalfinancemanager.entity.Category
import com.syfe.personalfinancemanager.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface CategoryRepository : JpaRepository<Category, Long> {

    fun findByOwnerIsNullOrOwnerId(ownerId: Long): List<Category>

    @Query("SELECT c FROM Category c WHERE c.name = :name AND (c.owner IS NULL OR c.owner.id = :ownerId)")
    fun findVisibleCategoryByName(
        @Param("name") name: String,
        @Param("ownerId") ownerId: Long
    ): Optional<Category>

    fun existsByNameAndOwner(name: String, owner: User): Boolean

    fun findByNameAndOwnerIdAndIsCustomTrue(name: String, ownerId: Long): Optional<Category>
}
