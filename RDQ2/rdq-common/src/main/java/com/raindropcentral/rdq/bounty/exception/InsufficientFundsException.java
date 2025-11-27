package com.raindropcentral.rdq.bounty.exception;

import java.util.UUID;

/**
 * Exception thrown when a player does not have sufficient funds to create a bounty.
 */
public final class InsufficientFundsException extends BountyException {
    
    public InsufficientFundsException(UUID playerUuid, String currency, double required, double available) {
        super(String.format("Player %s has insufficient %s: required %.2f, available %.2f", 
            playerUuid, currency, required, available));
    }
    
    public InsufficientFundsException(String message) {
        super(message);
    }
}
