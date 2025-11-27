package com.raindropcentral.rdq.bounty.exception;

/**
 * Exception thrown when attempting to claim a bounty that has already been claimed.
 */
public final class BountyAlreadyClaimedException extends BountyException {
    
    public BountyAlreadyClaimedException(Long bountyId) {
        super("Bounty has already been claimed: " + bountyId);
    }
    
    public BountyAlreadyClaimedException(String message) {
        super(message);
    }
}
