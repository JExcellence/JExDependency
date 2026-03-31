/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

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
