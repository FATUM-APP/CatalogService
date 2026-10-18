package fatum.catalog.service

import fatum.catalog.dto.CreateCategoryRequest
import fatum.catalog.dto.mapper.CategoryMapper
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.Category
import fatum.catalog.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(
    val categoryRepository: CategoryRepository
) {
    @Transactional
    fun createCategory(request: CreateCategoryRequest): Category {
        if (categoryRepository.existsById(request.name))
            throw FatumServiceException(FatumServiceException.CATEGORY_EXISTS)

        // parentName es nullable — solo busca si viene
        val parent = request.parentName?.let {
            categoryRepository.findById(it).orElseThrow {  // findById retorna Optional
                FatumServiceException(FatumServiceException.PARENT_CATEGORY_NOT_FOUND)
            }
        }

        val category = CategoryMapper.toEntity(request, parent)
        return categoryRepository.save(category)  // faltaba guardar y retornar
    }
}