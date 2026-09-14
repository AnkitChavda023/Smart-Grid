package com.smartgrid.commons.exception;

public class ResourceNotFoundException extends SmartGridException {

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(resourceType + " not found: " + identifier);
    }
}
