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

package com.raindropcentral.rdr.view;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Encapsulates policy decisions for storage lease.
 */
final class StorageLeasePolicy {

    static final long LEASE_RENEW_INTERVAL_TICKS = 20L * 30L;

    private static final Duration LEASE_DURATION = Duration.ofMinutes(2);

    private StorageLeasePolicy() {}

    static LocalDateTime nextExpiry() {
        return LocalDateTime.now().plus(LEASE_DURATION);
    }
}