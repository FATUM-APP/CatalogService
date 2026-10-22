package fatum.catalog.model

import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.constant.SchedulingModel
import fatum.catalog.model.constant.JobType
import fatum.catalog.model.constant.PriceType


import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertContains


class CategoryTest {
    private fun createRoot(name: String = "Belleza", schedulingModel: SchedulingModel = SchedulingModel.SINGLE_SESSION)
    = Category(name, schedulingModel)

    private fun createSub(name: String = "Barberia y Peluqueria", parent: Category = createRoot())
    = Category(name, parent)

    private fun createCatalogItem(
        providerId: String = "provider-123",
        title: String = "Mi Servicio",
        priceType: PriceType = PriceType.FIXED,
        jobType: JobType = JobType.PRESENCIAL,
        category: Category = createRoot(),
        estimatedTime: Int? = null
    ) = CatalogItem(
    providerId = providerId,
    title = title,
    priceType = priceType,
    jobType = jobType,
    category = category,
    defaultEstimatedTime = estimatedTime)


    @Nested
    inner class Creation {
        @Test
        fun `should create category`() {
            var category = createRoot("belleZa     ")
            assertEquals(category.name, "Belleza")
            assertEquals(SchedulingModel.SINGLE_SESSION, category.schedulingModel)
            assertNull(category.parent)

            category = createRoot(name = "BarberiA y Peluqueria     ", schedulingModel = SchedulingModel.PROJECT)
            assertEquals(category.name, "Barberia Y Peluqueria")
            assertEquals(category.slug, "barberia-y-peluqueria")
        }

        @Test
        fun `should throw on blank name`() {
            val ex = assertThrows<FatumServiceException> {
                Category("   ", SchedulingModel.SINGLE_SESSION)
            }
            assertEquals(CatalogErrorCode.BLANK, ex.errorCode)
        }
    }

    @Nested
    inner class SubCategories {

        @Test
        fun `should create subcategory`() {
            val parent = createRoot()
            val subcategory = createSub(parent = parent)
            assertEquals(parent, subcategory.parent)
            assertContains(parent.subcategories, subcategory)
        }

        @Test
        fun `should inherits parent's scheduling model`() {
            val parent = createRoot()
            val subcategory = createSub(parent = parent)
            assertEquals(parent.schedulingModel, subcategory.schedulingModel)
        }

        @Test
        fun `should remove subcategory`(){
            val parent = createRoot()
            val subcategory = createSub(parent = parent)
            parent.removeSubCategory(subcategory)
            assertTrue(parent.subcategories.isEmpty())
        }

        @Test
        fun `category shouldn't be its own parent`(){
            val ex = assertThrows<FatumServiceException> {
                Category("Belleza", createRoot(schedulingModel = SchedulingModel.PROJECT))
            }

            assertEquals(CatalogErrorCode.SELF_PARENT_CATEGORY, ex.errorCode)
        }

        @Test
        fun `category shouldn't repeat subcategory`(){
            val parent = createRoot()
            createSub(parent = parent)
            createSub(parent= parent)
            assertEquals(1, parent.subcategories.size)
        }

        @Test
        fun `category disable should disable all its subcategories`(){
            val parent = createRoot()
            val child1 = createSub(parent = parent)
            val child2 = createSub(name = "Pelo",parent = parent)
            createSub(name = "Corte",parent = child2)

            //disable subcategory
            child2.disable()
            assertFalse(child2.isAble)
            assertTrue(child2.subcategories.all{ it.isAble == false })
            assertTrue(parent.subcategories.any{ it.isAble})

            // Disable root category
            parent.disable()
            val subcategories = parent.subcategories.
            flatMapTo(parent.subcategories as MutableSet) { it.subcategories }
            assertEquals(subcategories.size,3)
            assertTrue(parent.subcategories.flatMap { it.subcategories }.all{ it.isAble == false })
        }

        @Test
        fun `category enable should enable all its subcategories`(){
            val parent = createRoot()
            val child1 = createSub(parent = parent)
            val child2 = createSub(name = "Pelo",parent = parent)
            createSub(name = "Corte",parent = child2)

            // Disable root category
            child2.disable()
            parent.enable()
            val subcategories = parent.subcategories.
            flatMapTo(parent.subcategories as MutableSet) { it.subcategories }
            assertEquals(subcategories.size,3)
            assertTrue(parent.subcategories.flatMap { it.subcategories }.all{ it.isAble})
        }

        @Test
        fun `subcategory with services shouldn't be removed`(){
            val parent = createRoot()
            val sub = createSub(parent = parent)
            createCatalogItem(
                providerId = "provider",
                title = "Test",
                priceType = PriceType.HOURLY,
                jobType = JobType.REMOTE,
                category = sub,
            )
            val ex = assertThrows<FatumServiceException> {
                parent.removeSubCategory(sub)
            }
            assertEquals(CatalogErrorCode.REMOVING_CATEGORY_WITH_SERVICES, ex.errorCode)
        }

        @Test
        fun `category with services shouldn't be parent`(){
            val parent = createRoot()
            createCatalogItem(
                providerId = "provider",
                title = "Test",
                priceType = PriceType.HOURLY,
                jobType = JobType.REMOTE,
                category = parent,
            )
            val ex = assertThrows<FatumServiceException> {
                createSub(parent = parent)
            }
            assertEquals(CatalogErrorCode.CATEGORY_HAS_SERVICES, ex.errorCode)
        }
    }

    @Nested
    inner class Services {
        @Test
        fun `root category should know its children services`() {
            var parent = createRoot()
            val child1 = createSub(parent = parent)
            val child2 = createSub(name = "Pelo",parent = child1)
            val child3 = createSub(name = "Corte",parent = child2)
            createCatalogItem(category = child3)
            assertTrue(parent.hasServices())
            assertFalse(parent.services.isEmpty())

            parent = createRoot()
            createCatalogItem(category = parent)
            assertTrue(parent.subcategories.isEmpty())
            assertTrue(parent.hasServices())
        }

        @Test
        fun `category shouldn't have subcategories and services`(){
            val parent = createRoot()
            createSub(parent = parent)

            assertEquals(CatalogErrorCode.CATEGORY_HAS_SUBCATEGORIES
                ,assertThrows<FatumServiceException> {
                createCatalogItem(category = parent)
            }.errorCode)
        }
    }
}