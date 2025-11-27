package com.raindropcentral.rdq.bounty.exception;

import java.util.UUID;

/**
 * Exception thrown when a player attempts to place a bounty on themselves.
 */
public final class SelfTargetingException extends BountyException {
    
    public SelfTargetingException(UUID playerUuid) {
        super("Player cannot place a bounty on themselves: " + playerUuid);
    }
    
    public SelfTargetingException(String message) {
        super(message);
    }
}
