package fatum.catalog.exception

import org.springframework.http.HttpStatus

enum class CatalogErrorCode(val defaultMessage: String, val httpStatus: HttpStatus) {

    // Generales
    BLANK("A required value should not be blank", HttpStatus.BAD_REQUEST),
    INITIAL_AFTER_FINAL("The initial time or date cannot be after the final time", HttpStatus.BAD_REQUEST),

    // Categorías
    CATEGORY_EXISTS("Category already exists", HttpStatus.CONFLICT),
    CATEGORY_NOT_FOUND("Category not found", HttpStatus.NOT_FOUND),
    SELF_PARENT_CATEGORY("A category cannot be its own parent", HttpStatus.BAD_REQUEST),
    SUBCATEGORY_WITHOUT_ROOT("Subcategory doesn't have root category", HttpStatus.BAD_REQUEST),
    CATEGORY_WITHOUT_SCHEDULING("Category must have a scheduling model", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_SUBCATEGORIES("A root category cannot have services", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_SERVICES("A category with services cannot have subcategories", HttpStatus.BAD_REQUEST),
    REMOVING_CATEGORY_WITH_SERVICES("Cannot remove a category with services", HttpStatus.BAD_REQUEST),
    ENABLING_SUB_WITH_ROOT_DISABLED("Cannot enable a subcategory with root category disabled", HttpStatus.CONFLICT),

    // Servicios
    SERVICE_WITHOUT_CATEGORY("Service must have a category", HttpStatus.BAD_REQUEST),
   SERVICE_NOT_FOUND("Item not found", HttpStatus.NOT_FOUND),
    UK_SERVICES_CATEGORY_PROVIDER("Provider should have only one service per category", HttpStatus.CONFLICT),
    UK_SERVICES_TITLE_PROVIDER("Provider cannot have two services with the same title", HttpStatus.CONFLICT),
    NEGATIVE_PRICE("Price cannot be negative", HttpStatus.BAD_REQUEST),
    NEGATIVE_ESTIMATED_TIME("Estimated time cannot be negative", HttpStatus.BAD_REQUEST),

    // Preguntas
    QUESTION_WITHOUT_OPTIONS("Question must have at least two options", HttpStatus.BAD_REQUEST),

    // Imágenes
    INVALID_IMAGE("Invalid image", HttpStatus.BAD_REQUEST),
    IMAGE_TOO_LARGE("Image too large", HttpStatus.PAYLOAD_TOO_LARGE),
    INVALID_IMAGE_FORMAT("Invalid image format", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    TOO_MANY_IMAGES("You have reached the maximum number of images allowed per service", HttpStatus.BAD_REQUEST),
    NO_SERVICE_IMAGES("Service has no images", HttpStatus.BAD_REQUEST),
    NO_IMAGE_ID("A saved image must have an ID", HttpStatus.INTERNAL_SERVER_ERROR),
    NO_IMAGE_URL("No URL was returned for image", HttpStatus.INTERNAL_SERVER_ERROR);
}
