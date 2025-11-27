package com.raindropcentral.rdq.shared.error;

public class SelfTargetingException extends RDQException {

    public SelfTargetingException() {
        super(new RDQError.SelfTargeting());
    }
}
