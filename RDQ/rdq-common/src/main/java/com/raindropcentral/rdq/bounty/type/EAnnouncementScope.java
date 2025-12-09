package com.raindropcentral.rdq.bounty.type;

public enum EAnnouncementScope {

    SERVER,
    NEARBY,
    TARGET
    ;

    public static  EAnnouncementScope of(String value) {
        try  {
            return EAnnouncementScope.valueOf(value);
        } catch (IllegalArgumentException e) {
            return EAnnouncementScope.SERVER;
        }
    }
}
