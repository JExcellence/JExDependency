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

package com.raindropcentral.core.service;

import com.raindropcentral.core.database.entity.player.RBossBarPreference;
import com.raindropcentral.core.database.repository.RBossBarPreferenceRepository;
import jakarta.persistence.EntityManagerFactory;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests {@link RCoreBossBarManager}.
 *
 * @author Codex
 * @since 2.1.0
 * @version 2.1.0
 */
class RCoreBossBarManagerTest {

    @Test
    void rejectsDuplicateProviderKeys() {
        final RCoreBossBarManager manager = new RCoreBossBarManager(new InMemoryBossBarPreferenceRepository(), (player, key) -> {
        });
        final RCoreBossBarService.ProviderDefinition providerDefinition = this.createEnabledOnlyProvider("rds.shop", false);

        manager.registerProvider(providerDefinition);

        assertThrows(IllegalArgumentException.class, () -> manager.registerProvider(providerDefinition));
    }

    @Test
    void resolvesDefaultsAndPersistsCreatedPreference() {
        final InMemoryBossBarPreferenceRepository repository = new InMemoryBossBarPreferenceRepository();
        final RCoreBossBarManager manager = new RCoreBossBarManager(repository, (player, key) -> {
        });
        manager.registerProvider(this.createDisplayModeProvider("rda.mana", false, null, null));

        final RCoreBossBarService.PreferenceSnapshot snapshot = manager.resolvePreferences(UUID.randomUUID(), "rda.mana");

        assertFalse(snapshot.enabled());
        assertEquals("ACTION_BAR", snapshot.options().get("display-mode"));
        assertEquals(1, repository.getCreateCount());
    }

    @Test
    void resolvesLegacySeedWhenProviderRequestsLazyMigration() {
        final InMemoryBossBarPreferenceRepository repository = new InMemoryBossBarPreferenceRepository();
        final RCoreBossBarManager manager = new RCoreBossBarManager(repository, (player, key) -> {
        });
        manager.registerProvider(this.createDisplayModeProvider(
            "rda.mana",
            false,
            (playerUuid, providerDefinition) -> new RCoreBossBarService.PreferenceSeed(
                true,
                Map.of("display-mode", "MENUS_ONLY")
            ),
            null
        ));

        final RCoreBossBarService.PreferenceSnapshot snapshot = manager.resolvePreferences(UUID.randomUUID(), "rda.mana");

        assertTrue(snapshot.enabled());
        assertEquals("MENUS_ONLY", snapshot.options().get("display-mode"));
    }

    @Test
    void setOptionPersistsUpdatedSnapshotAndNotifiesHandler() {
        final InMemoryBossBarPreferenceRepository repository = new InMemoryBossBarPreferenceRepository();
        final AtomicReference<RCoreBossBarService.PreferenceSnapshot> notifiedSnapshot = new AtomicReference<>();
        final RCoreBossBarManager manager = new RCoreBossBarManager(repository, (player, key) -> {
        });
        manager.registerProvider(this.createDisplayModeProvider(
            "rda.mana",
            true,
            null,
            (playerUuid, preferenceSnapshot) -> notifiedSnapshot.set(preferenceSnapshot)
        ));

        final UUID playerUuid = UUID.randomUUID();
        manager.resolvePreferences(playerUuid, "rda.mana");
        final RCoreBossBarService.PreferenceSnapshot snapshot = manager.setOption(
            playerUuid,
            "rda.mana",
            "display-mode",
            "menus_only"
        );

        assertEquals("MENUS_ONLY", snapshot.options().get("display-mode"));
        assertEquals("MENUS_ONLY", notifiedSnapshot.get().options().get("display-mode"));
        assertEquals(1, repository.getUpdateCount());
    }

    @Test
    void setOptionRejectsInvalidValues() {
        final InMemoryBossBarPreferenceRepository repository = new InMemoryBossBarPreferenceRepository();
        final RCoreBossBarManager manager = new RCoreBossBarManager(repository, (player, key) -> {
        });
        manager.registerProvider(this.createDisplayModeProvider("rda.mana", true, null, null));

        final UUID playerUuid = UUID.randomUUID();
        manager.resolvePreferences(playerUuid, "rda.mana");

        assertThrows(
            IllegalArgumentException.class,
            () -> manager.setOption(playerUuid, "rda.mana", "display-mode", "invalid-choice")
        );
    }

    private RCoreBossBarService.ProviderDefinition createEnabledOnlyProvider(
        final String providerKey,
        final boolean defaultEnabled
    ) {
        return new RCoreBossBarService.ProviderDefinition(
            providerKey,
            Material.CHEST,
            providerKey + ".name",
            providerKey + ".description",
            defaultEnabled,
            List.of(),
            null,
            null
        );
    }

    private RCoreBossBarService.ProviderDefinition createDisplayModeProvider(
        final String providerKey,
        final boolean defaultEnabled,
        final RCoreBossBarService.LegacyPreferenceResolver legacyPreferenceResolver,
        final RCoreBossBarService.PreferenceChangeHandler preferenceChangeHandler
    ) {
        return new RCoreBossBarService.ProviderDefinition(
            providerKey,
            Material.HEART_OF_THE_SEA,
            providerKey + ".name",
            providerKey + ".description",
            defaultEnabled,
            List.of(new RCoreBossBarService.ProviderOption(
                "display-mode",
                providerKey + ".display-mode.name",
                providerKey + ".display-mode.description",
                "ACTION_BAR",
                List.of(
                    new RCoreBossBarService.ProviderOptionChoice("ACTION_BAR", "mode.action_bar", null),
                    new RCoreBossBarService.ProviderOptionChoice("MENUS_ONLY", "mode.menus_only", null)
                )
            )),
            legacyPreferenceResolver,
            preferenceChangeHandler
        );
    }

    private static final class InMemoryBossBarPreferenceRepository extends RBossBarPreferenceRepository {

        private final Map<String, RBossBarPreference> storedPreferences = new HashMap<>();
        private int createCount;
        private int updateCount;

        private InMemoryBossBarPreferenceRepository() {
            super(
                new DirectExecutorService(),
                mock(EntityManagerFactory.class),
                RBossBarPreference.class,
                RBossBarPreference::getCompositeKey
            );
        }

        @Override
        public RBossBarPreference create(final RBossBarPreference entity) {
            this.createCount++;
            this.storedPreferences.put(entity.getCompositeKey(), entity);
            return entity;
        }

        @Override
        public RBossBarPreference update(final RBossBarPreference entity) {
            this.updateCount++;
            this.storedPreferences.put(entity.getCompositeKey(), entity);
            return entity;
        }

        @Override
        public RBossBarPreference findByPlayerAndProvider(final UUID playerUuid, final String providerKey) {
            return this.storedPreferences.get(RBossBarPreference.composeKey(playerUuid, providerKey));
        }

        @Override
        public List<RBossBarPreference> findAllByPlayer(final UUID playerUuid) {
            final ArrayList<RBossBarPreference> matches = new ArrayList<>();
            for (final RBossBarPreference storedPreference : this.storedPreferences.values()) {
                if (storedPreference.getPlayerUuid().equals(playerUuid)) {
                    matches.add(storedPreference);
                }
            }
            return List.copyOf(matches);
        }

        private int getCreateCount() {
            return this.createCount;
        }

        private int getUpdateCount() {
            return this.updateCount;
        }
    }

    private static final class DirectExecutorService extends AbstractExecutorService {

        private boolean shutdown;

        @Override
        public void shutdown() {
            this.shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            this.shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return this.shutdown;
        }

        @Override
        public boolean isTerminated() {
            return this.shutdown;
        }

        @Override
        public boolean awaitTermination(final long timeout, final TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(final Runnable command) {
            command.run();
        }
    }
}
