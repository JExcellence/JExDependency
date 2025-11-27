package com.raindropcentral.rdq.bounty.exception;

import java.util.UUID;

/**
 * Exception thrown when attempting to create a bounty that already exists.
 */
public final class BountyAlreadyExistsException extends BountyException {
    
    public BountyAlreadyExistsException(UUID targetUuid) {
        super("An active bounty already exists for target: " + targetUuid);
    }
    
    public BountyAlreadyExistsException(String message) {
        super(message);
    }
}
