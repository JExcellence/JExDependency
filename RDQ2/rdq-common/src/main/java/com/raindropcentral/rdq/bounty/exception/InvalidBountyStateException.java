package com.raindropcentral.rdq.bounty.exception;

/**
 * Exception thrown when a bounty operation is attempted on a bounty in an invalid state.
 */
public final class InvalidBountyStateException extends BountyException {
    
    public InvalidBountyStateException(String message) {
        super(message);
    }
    
    public InvalidBountyStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
