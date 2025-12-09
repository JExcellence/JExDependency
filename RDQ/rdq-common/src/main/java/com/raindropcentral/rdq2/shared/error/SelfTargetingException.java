package com.raindropcentral.rdq2.shared.error;

public class SelfTargetingException extends RDQException {

    public SelfTargetingException() {
        super(new RDQError.SelfTargeting());
    }
}
