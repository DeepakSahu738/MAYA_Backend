package com.MAYA.MAYA.Controller.exception;

public class CreatorNotFoundException extends RuntimeException {
    public CreatorNotFoundException(Long creatorId) {
        super("Creator not found with ID: " + creatorId);
    }
}
