package com.raindropcentral.rdq.bounty.exception;

/**
 * Exception thrown when a requested bounty cannot be found.
 */
public final class BountyNotFoundException extends BountyException {
    
    public BountyNotFoundException(Long bountyId) {
        super("Bounty not found with ID: " + bountyId);
    }
    
    public BountyNotFoundException(String message) {
        super(message);
    }
}
