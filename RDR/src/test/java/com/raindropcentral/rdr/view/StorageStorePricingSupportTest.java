/*
 * StorageStorePricingSupportTest.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rdr.view;

import java.lang.reflect.Proxy;

import com.raindropcentral.rdr.configs.StoreRequirementSection;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.junit.jupiter.api.Test;
import org.bukkit.entity.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
                    true
                ),
                new StorageStorePricingSupport.ResolvedStoreRequirement(
                    1,
                    "two",
                    StoreRequirementSection.currency("two", "coins", 50.0D, "EMERALD"),
                    null,
                    "Coins: 50.00",
                    true
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
                        true
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
                        true
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
                        false
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
