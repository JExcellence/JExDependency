package com.raindropcentral.rdq.bounty.type;

/**
 * Represents the current status of a bounty in the system.
 */
public enum BountyStatus {
    /**
     * The bounty is active and can be claimed.
     */
    ACTIVE,
    
    /**
     * The bounty has been claimed by a hunter.
     */
    CLAIMED,
    
    /**
     * The bounty has expired and is no longer claimable.
     */
    EXPIRED,
    
    /**
     * The bounty has been cancelled by an administrator.
     */
    CANCELLED
}
