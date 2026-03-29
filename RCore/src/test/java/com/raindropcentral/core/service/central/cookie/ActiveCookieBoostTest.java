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

package com.raindropcentral.core.service.central.cookie;

import com.raindropcentral.rplatform.cookie.CookieBoostType;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveCookieBoostTest {

    @Test
    void serializesAndDeserializesPersistedBoostPayload() {
        final UUID playerId = UUID.randomUUID();
        final ActiveCookieBoost boost = new ActiveCookieBoost(
                playerId,
                CookieBoostType.JOB_VAULT,
                "EcoJobs",
                "builder",
                "job-vault-rate-50-cookie",
                0.5D,
                1_900_000_000_000L
        );

        final Optional<ActiveCookieBoost> decoded = ActiveCookieBoost.deserialize(
                playerId,
                boost.statisticIdentifier(),
                boost.serialize()
        );

        assertTrue(decoded.isPresent());
        assertEquals(boost.playerId(), decoded.get().playerId());
        assertEquals(boost.boostType(), decoded.get().boostType());
        assertEquals(boost.integrationId(), decoded.get().integrationId());
        assertEquals(boost.targetId(), decoded.get().targetId());
        assertEquals(boost.itemCode(), decoded.get().itemCode());
        assertEquals(boost.rateBonus(), decoded.get().rateBonus());
        assertEquals(boost.expiresAtEpochMs(), decoded.get().expiresAtEpochMs());
        assertEquals(1.5D, decoded.get().multiplier());
    }

    @Test
    void ignoresNonBoostStatisticIdentifiers() {
        final Optional<ActiveCookieBoost> decoded = ActiveCookieBoost.deserialize(
                UUID.randomUUID(),
                "some.other.statistic",
                "{\"rateBonus\":1.0}"
        );

        assertTrue(decoded.isEmpty());
    }
}
