package fatum.catalog.exception

class FatumServiceException(val errorCode: CatalogErrorCode) : RuntimeException(errorCode.defaultMessage)
