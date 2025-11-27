package com.raindropcentral.rdq.bounty.exception;

/**
 * Base sealed exception for all bounty-related errors.
 * This sealed hierarchy ensures all bounty exceptions are known at compile time.
 */
public sealed class BountyException extends Exception 
    permits BountyNotFoundException, 
            BountyAlreadyExistsException, 
            BountyExpiredException, 
            BountyAlreadyClaimedException,
            InsufficientFundsException,
            InvalidBountyStateException,
            SelfTargetingException {
    
    public BountyException(String message) {
        super(message);
    }
    
    public BountyException(String message, Throwable cause) {
        super(message, cause);
    }
}
