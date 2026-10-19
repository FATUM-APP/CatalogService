package fatum.catalog.exception;

public class FatumServiceException extends RuntimeException {

    private final CatalogErrorCode errorCode;

    public FatumServiceException(CatalogErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public CatalogErrorCode getErrorCode() {
        return errorCode;
    }
}