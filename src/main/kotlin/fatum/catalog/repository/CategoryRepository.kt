package fatum.catalog.repository

import fatum.catalog.model.Category
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CategoryRepository : JpaRepository<Category, String> {

    fun existsByNameIgnoreCase(name: String): Boolean

    fun findByNameIgnoreCase(name: String): Optional<Category>

    fun findByNameIgnoreCaseAndIsAbleTrue(name: String): Optional<Category>

    fun findByParentIsNull(): List<Category>

    fun findByParentIsNullAndIsAbleTrue(): List<Category>
}
