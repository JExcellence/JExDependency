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

import java.lang.reflect.Proxy;

import com.raindropcentral.rdr.configs.StoreRequirementSection;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.junit.jupiter.api.Test;
import org.bukkit.entity.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests storage store pricing support behavior.
 */
class StorageStorePricingSupportTest {

    @Test
    void formatsAmountsUsingTwoDecimalPlaces() {
        assertEquals("1000.00", StorageStorePricingSupport.formatAmount(1000.0D));
        assertEquals("50.25", StorageStorePricingSupport.formatAmount(50.25D));
    }

    @Test
    void identifiesVaultCurrencyDisplayName() {
        assertEquals("Vault", StorageStorePricingSupport.getCurrencyDisplayName("vault"));
    }

    @Test
    void formatsRequirementSummariesFromResolvedEntries() {
        final String summary = StorageStorePricingSupport.formatRequirementSummary(
            java.util.List.of(
                new StorageStorePricingSupport.ResolvedStoreRequirement(
                    1,
                    "one",
                    StoreRequirementSection.currency("one", "vault", 1000.0D, "GOLD_INGOT"),
                    null,
                    "Vault: 1000.00",
                    true,
                    -1.0D
                ),
                new StorageStorePricingSupport.ResolvedStoreRequirement(
                    1,
                    "two",
                    StoreRequirementSection.currency("two", "coins", 50.0D, "EMERALD"),
                    null,
                    "Coins: 50.00",
                    true,
                    -1.0D
                )
            )
        );

        assertEquals("Vault: 1000.00, Coins: 50.00", summary);
    }

    @Test
    void resolvesReadyAvailabilityWhenEveryRequirementIsMet() {
        final Player player = fakePlayer();
        final StorageStorePricingSupport.RequirementAvailability availability =
            StorageStorePricingSupport.resolveAvailability(
                player,
                java.util.List.of(
                    new StorageStorePricingSupport.ResolvedStoreRequirement(
                        1,
                        "one",
                        StoreRequirementSection.currency("one", "vault", 1000.0D, "GOLD_INGOT"),
                        new FakeRequirement(true),
                        "ready",
                        true,
                        -1.0D
                    )
                )
            );

        assertEquals(StorageStorePricingSupport.RequirementAvailability.READY, availability);
    }

    @Test
    void resolvesPendingAvailabilityWhenARequirementIsUnmet() {
        final Player player = fakePlayer();
        final StorageStorePricingSupport.RequirementAvailability availability =
            StorageStorePricingSupport.resolveAvailability(
                player,
                java.util.List.of(
                    new StorageStorePricingSupport.ResolvedStoreRequirement(
                        1,
                        "one",
                        StoreRequirementSection.currency("one", "vault", 1000.0D, "GOLD_INGOT"),
                        new FakeRequirement(false),
                        "pending",
                        true,
                        -1.0D
                    )
                )
            );

        assertEquals(StorageStorePricingSupport.RequirementAvailability.PENDING, availability);
    }

    @Test
    void resolvesUnavailableAvailabilityWhenARequirementIsNotOperational() {
        final Player player = fakePlayer();
        final StorageStorePricingSupport.RequirementAvailability availability =
            StorageStorePricingSupport.resolveAvailability(
                player,
                java.util.List.of(
                    new StorageStorePricingSupport.ResolvedStoreRequirement(
                        1,
                        "one",
                        StoreRequirementSection.currency("one", "vault", 1000.0D, "GOLD_INGOT"),
                        null,
                        "offline",
                        false,
                        -1.0D
                    )
                )
            );

        assertEquals(StorageStorePricingSupport.RequirementAvailability.UNAVAILABLE, availability);
    }

    private static Player fakePlayer() {
        return (Player) Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class[]{Player.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "toString" -> "FakePlayer";
                case "hashCode" -> 1;
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            }
        );
    }

    private static final class FakeRequirement extends AbstractRequirement {

        private final boolean met;

        private FakeRequirement(final boolean met) {
            super("TEST");
            this.met = met;
        }

        @Override
        public boolean isMet(final Player player) {
            return this.met;
        }

        @Override
        public double calculateProgress(final Player player) {
            return this.met ? 1.0D : 0.0D;
        }

        @Override
        public void consume(final Player player) {
        }

        @Override
        public String getDescriptionKey() {
            return "test.requirement";
        }
    }
}
