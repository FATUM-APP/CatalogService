package fatum.catalog.exception;

public class FatumServiceException extends RuntimeException {

    public static final String BLANK = "A required value should not be blank";
    public static final String SELF_PARENT_CATEGORY = "A category cannot be its own parent";
    public static final String CATEGORY_HAS_SUBCATEGORIES = "A root category cannot have services";
    public static final String CATEGORY_HAS_SERVICES = "A category with services cannot have subcategories";
    public static final String INITIAL_AFTER_FINAL = "The initial time or date cannot be after the final time";
    public static final String NEGATIVE_PRICE = "price cannot be negative";
    public static final String CATEGORY_WITHOUT_SCHEDULING = "Category must have a scheduling model";
    public static final String QUESTION_WITHOUT_OPTIONS = "A choice question must have at least 2 options";
    public static final String MAX_PHOTOS_EXCEEDED = "Maximum number of photos per service exceeded";
    public static final String CATEGORY_EXISTS = "Category already exists";
    public static final String PARENT_CATEGORY_NOT_FOUND = "Parent category not found";
    public static final String REMOVING_CATEGORY_WITH_SERVICES = "Cannot remove a category with services";

    public FatumServiceException(String message) {
        super(message);
    }
}
