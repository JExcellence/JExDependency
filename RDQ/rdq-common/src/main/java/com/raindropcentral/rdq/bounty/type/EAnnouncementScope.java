package com.raindropcentral.rdq.bounty.type;

/**
 * Represents the EAnnouncementScope API type.
 */
public enum EAnnouncementScope {

    SERVER,
    NEARBY,
    TARGET
    ;

    /**
     * Executes of.
     */
    public static  EAnnouncementScope of(String value) {
        try  {
            return EAnnouncementScope.valueOf(value);
        } catch (IllegalArgumentException e) {
            return EAnnouncementScope.SERVER;
        }
    }
}
