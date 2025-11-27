package com.raindropcentral.rdq.bounty.exception;

/**
 * Exception thrown when attempting to claim or interact with an expired bounty.
 */
public final class BountyExpiredException extends BountyException {
    
    public BountyExpiredException(Long bountyId) {
        super("Bounty has expired: " + bountyId);
    }
    
    public BountyExpiredException(String message) {
        super(message);
    }
}
