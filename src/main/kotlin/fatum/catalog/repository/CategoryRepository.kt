package fatum.catalog.repository

import fatum.catalog.model.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository: JpaRepository<Category, String> {

}