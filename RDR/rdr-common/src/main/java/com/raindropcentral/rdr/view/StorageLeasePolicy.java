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