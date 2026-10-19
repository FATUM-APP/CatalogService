package fatum.catalog.repository

import fatum.catalog.model.Category
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CategoryRepository : JpaRepository<Category, String> {

    fun existsByNameIgnoreCase(name: String): Boolean

    fun findByNameIgnoreCase(name: String): Optional<Category>
}