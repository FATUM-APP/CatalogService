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
        const val CATEGORY_EXISTS = "Category already exists"
        const val CATEGORY_NOT_FOUND = "Category not found"
        const val REMOVING_CATEGORY_WITH_SERVICES = "Cannot remove a category with services"
        const val NEGATIVE_ESTIMATED_TIME = "Estimated time cannot be negative"
        const val QUESTION_WITHOUT_OPTIONS = "Question must have at least two options"
        const val INVALID_IMAGE = "Invalid image"
        const val IMAGE_TOO_LARGE = "Image too large"
        const val INVALID_IMAGE_FORMAT = "Invalid image format"
        const val TOO_MANY_IMAGES = "You have reached the maximum number of images allowed per service"
        const val NO_SERVICE_IMAGES = "Service has no images"
        const val UK_SERVICES_CATEGORY_PROVIDER = "Provider should have only one service per category"
        const val UK_SERVICES_TITLE_PROVIDER = "Provider cannot have two services with the same title"
    }
}