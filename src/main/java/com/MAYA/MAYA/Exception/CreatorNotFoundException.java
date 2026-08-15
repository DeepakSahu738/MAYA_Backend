package com.MAYA.MAYA.Exception;

/**
 * Thrown when a creator ID does not exist in the database.
 */
public class CreatorNotFoundException extends RuntimeException {

    private final Long creatorId;

    public CreatorNotFoundException(Long creatorId) {
        super("Creator not found with ID: " + creatorId);
        this.creatorId = creatorId;
    }

    public Long getCreatorId() {
        return creatorId;
    }
}
