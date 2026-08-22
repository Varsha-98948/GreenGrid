package com.greengrid.exception;

import java.util.UUID;

public class DuplicateProblemException extends ConflictException {

    private final UUID existingProblemId;

    public DuplicateProblemException(String message, UUID existingProblemId) {
        super(message);
        this.existingProblemId = existingProblemId;
    }

    public UUID getExistingProblemId() {
        return existingProblemId;
    }
}
