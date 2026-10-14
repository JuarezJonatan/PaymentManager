package com.jejuarez.paymentmanager.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, String resourceId) {
        super("%s with id '%s' was not found".formatted(resourceName, resourceId));
    }
}
