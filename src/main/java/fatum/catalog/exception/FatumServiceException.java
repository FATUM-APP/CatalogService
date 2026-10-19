package fatum.catalog.exception;

public class FatumServiceException extends RuntimeException {

    public static final String BLANK = "A required value should not be blank";
    public static final String SELF_PARENT_CATEGORY = "A category cannot be its own parent";
    public static final String CATEGORY_HAS_SUBCATEGORIES = "A root category cannot have services";
    public static final String CATEGORY_HAS_SERVICES = "A category with services cannot have subcategories";
    public static final String INITIAL_AFTER_FINAL = "The initial time or date cannot be after the final time";
    public static final String NEGATIVE_PRICE = "price cannot be negative";
    public static final String CATEGORY_WITHOUT_SCHEDULING = "Category must have a scheduling model";
    public static final String CATEGORY_EXISTS = "Category already exists";
    public static final String CATEGORY_NOT_FOUND = "Category not found";
    public static final String REMOVING_CATEGORY_WITH_SERVICES = "Cannot remove a category with services";
    public static final String NEGATIVE_ESTIMATED_TIME = "Estimated time cannot be negative";
    public static final String QUESTION_WITHOUT_OPTIONS = "Question must have at least two options";
    public static final String INVALID_IMAGE = "Invalid image";
    public static final String IMAGE_TOO_LARGE = "Image too large";
    public static final String INVALID_IMAGE_FORMAT = "Invalid image format";
    public static final String TOO_MANY_IMAGES = "You have reached the maximum number of images allowed per service";
    public static final String NO_SERVICE_IMAGES = "Service has no images";
    public static final String UK_SERVICES_CATEGORY_PROVIDER= "Provider should have only one service per category";
    public static final String UK_SERVICES_TITLE_PROVIDER= "Provider cannot have two services with the same title";


    public FatumServiceException(String message) {
        super(message);
    }
}
