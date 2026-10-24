package fatum.catalog.repository

import fatum.catalog.model.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, String> {

    fun existsByNameIgnoreCase(name: String): Boolean

    fun findByNameIgnoreCaseAndIsAble(name: String, isAble: Boolean): Category?
    fun findByNameIgnoreCase(name: String): Category?

    fun findByParentIsNullAndIsAble(isAble: Boolean): List<Category>
    fun findByParentIsNull(): List<Category>
}
