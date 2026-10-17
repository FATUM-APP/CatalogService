package fatum.catalog.exception

class FatumServiceException (message: String) : RuntimeException(message) {

    companion object {
        const val BLANK = "A required value should not be blank"
        const val SELF_PARENT_CATEGORY = "A category cannot be its own parent"
        const val CATEGORY_HAS_SUBCATEGORIES = "A root category cannot have services"
        const val CATEGORY_HAS_SERVICES = "A category with services cannot have subcategories"
        const val INITIAL_AFTER_FINAL = "The initial time or date cannot be after the final time"
        const val NEGATIVE_PRICE = "price cannot be negative"
        const val CATEGORY_WITHOUT_SCHEDULING = "Category must have a scheduling model"
    }
}