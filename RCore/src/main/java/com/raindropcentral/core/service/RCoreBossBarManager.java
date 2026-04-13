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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Default in-memory registry and persistence-backed implementation of {@link RCoreBossBarService}.
 *
 * <p>The manager keeps a small snapshot cache keyed by {@code player + provider} so live plugin HUD
 * services can resolve preferences without repeatedly hitting the repository layer during regular
 * refresh ticks.</p>
 *
 * @author Codex
 * @since 2.1.0
 * @version 2.1.0
 */
public class RCoreBossBarManager implements RCoreBossBarService {

    private final RBossBarPreferenceRepository repository;
    private final BiConsumer<Player, String> viewOpener;
    private final Map<String, ProviderDefinition> providerRegistry = new ConcurrentHashMap<>();
    private final Map<String, PreferenceSnapshot> preferenceCache = new ConcurrentHashMap<>();

    /**
     * Creates a new boss-bar manager.
     *
     * @param repository backing repository for preference persistence
     * @param viewOpener callback used to open the shared settings views
     */
    public RCoreBossBarManager(
        final @NotNull RBossBarPreferenceRepository repository,
        final @NotNull BiConsumer<Player, String> viewOpener
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.viewOpener = Objects.requireNonNull(viewOpener, "viewOpener");
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull java.util.List<ProviderDefinition> getRegisteredProviders() {
        return this.providerRegistry.values().stream()
            .sorted(Comparator.comparing(ProviderDefinition::key))
            .toList();
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull Optional<ProviderDefinition> findProvider(final @NotNull String providerKey) {
        final String normalizedKey = normalizeProviderKey(providerKey);
        return Optional.ofNullable(this.providerRegistry.get(normalizedKey));
    }

    /** {@inheritDoc} */
    @Override
    public void registerProvider(final @NotNull ProviderDefinition providerDefinition) {
        Objects.requireNonNull(providerDefinition, "providerDefinition");
        final ProviderDefinition existingDefinition = this.providerRegistry.putIfAbsent(
            providerDefinition.key(),
            providerDefinition
        );
        if (existingDefinition != null) {
            throw new IllegalArgumentException("Boss-bar provider is already registered: " + providerDefinition.key());
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean unregisterProvider(final @NotNull String providerKey) {
        final String normalizedKey = normalizeProviderKey(providerKey);
        return this.providerRegistry.remove(normalizedKey) != null;
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull PreferenceSnapshot resolvePreferences(
        final @NotNull UUID playerUuid,
        final @NotNull String providerKey
    ) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        final ProviderDefinition providerDefinition = this.requireProvider(providerKey);
        return this.preferenceCache.computeIfAbsent(
            cacheKey(playerUuid, providerDefinition.key()),
            ignored -> this.loadOrCreatePreference(playerUuid, providerDefinition)
        );
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull PreferenceSnapshot setEnabled(
        final @NotNull UUID playerUuid,
        final @NotNull String providerKey,
        final boolean enabled
    ) {
        final ProviderDefinition providerDefinition = this.requireProvider(providerKey);
        final RBossBarPreference preference = this.requirePreferenceEntity(playerUuid, providerDefinition);
        preference.setEnabled(enabled);
        this.repository.update(preference);
        return this.storeSnapshot(playerUuid, providerDefinition, preference);
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull PreferenceSnapshot toggleEnabled(
        final @NotNull UUID playerUuid,
        final @NotNull String providerKey
    ) {
        final PreferenceSnapshot currentSnapshot = this.resolvePreferences(playerUuid, providerKey);
        return this.setEnabled(playerUuid, providerKey, !currentSnapshot.enabled());
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull PreferenceSnapshot setOption(
        final @NotNull UUID playerUuid,
        final @NotNull String providerKey,
        final @NotNull String optionKey,
        final @NotNull String value
    ) {
        final ProviderDefinition providerDefinition = this.requireProvider(providerKey);
        final ProviderOption providerOption = providerDefinition.findOption(optionKey)
            .orElseThrow(() -> new IllegalArgumentException(
                "Unknown option '" + optionKey + "' for provider " + providerDefinition.key()
            ));
        final RBossBarPreference preference = this.requirePreferenceEntity(playerUuid, providerDefinition);
        preference.putOptionValue(providerOption.key(), providerOption.normalizeValue(value));
        this.repository.update(preference);
        return this.storeSnapshot(playerUuid, providerDefinition, preference);
    }

    /** {@inheritDoc} */
    @Override
    public void openSettingsView(final @NotNull Player player) {
        this.viewOpener.accept(Objects.requireNonNull(player, "player"), "");
    }

    /** {@inheritDoc} */
    @Override
    public void openSettingsView(final @NotNull Player player, final @NotNull String providerKey) {
        this.requireProvider(providerKey);
        this.viewOpener.accept(Objects.requireNonNull(player, "player"), normalizeProviderKey(providerKey));
    }

    private @NotNull ProviderDefinition requireProvider(final @NotNull String providerKey) {
        return this.findProvider(providerKey)
            .orElseThrow(() -> new IllegalArgumentException("Unknown boss-bar provider: " + providerKey));
    }

    private @NotNull RBossBarPreference requirePreferenceEntity(
        final @NotNull UUID playerUuid,
        final @NotNull ProviderDefinition providerDefinition
    ) {
        this.resolvePreferences(playerUuid, providerDefinition.key());
        final RBossBarPreference preference = this.repository.findByPlayerAndProvider(playerUuid, providerDefinition.key());
        if (preference == null) {
            throw new IllegalStateException(
                "Boss-bar preference row disappeared for "
                    + playerUuid
                    + " and provider "
                    + providerDefinition.key()
            );
        }
        return preference;
    }

    private @NotNull PreferenceSnapshot loadOrCreatePreference(
        final @NotNull UUID playerUuid,
        final @NotNull ProviderDefinition providerDefinition
    ) {
        final RBossBarPreference persistedPreference = this.repository.findByPlayerAndProvider(playerUuid, providerDefinition.key());
        if (persistedPreference != null) {
            return this.toSnapshot(playerUuid, providerDefinition, persistedPreference);
        }

        final PreferenceSeed legacySeed = providerDefinition.legacyPreferenceResolver() == null
            ? null
            : providerDefinition.legacyPreferenceResolver().resolve(playerUuid, providerDefinition);
        final RBossBarPreference createdPreference = new RBossBarPreference(
            playerUuid,
            providerDefinition.key(),
            legacySeed == null ? providerDefinition.defaultEnabled() : legacySeed.enabled()
        );

        final Map<String, String> seededOptions = legacySeed == null ? Map.of() : legacySeed.options();
        for (final ProviderOption option : providerDefinition.options()) {
            final String seededValue = seededOptions.get(option.key());
            final String resolvedValue = seededValue == null
                ? option.defaultValue()
                : resolveStoredValue(option, seededValue);
            createdPreference.putOptionValue(option.key(), resolvedValue);
        }

        this.repository.create(createdPreference);
        return this.toSnapshot(playerUuid, providerDefinition, createdPreference);
    }

    private @NotNull PreferenceSnapshot storeSnapshot(
        final @NotNull UUID playerUuid,
        final @NotNull ProviderDefinition providerDefinition,
        final @NotNull RBossBarPreference preference
    ) {
        final PreferenceSnapshot preferenceSnapshot = this.toSnapshot(playerUuid, providerDefinition, preference);
        this.preferenceCache.put(cacheKey(playerUuid, providerDefinition.key()), preferenceSnapshot);
        if (providerDefinition.preferenceChangeHandler() != null) {
            providerDefinition.preferenceChangeHandler().onPreferenceChanged(playerUuid, preferenceSnapshot);
        }
        return preferenceSnapshot;
    }

    private @NotNull PreferenceSnapshot toSnapshot(
        final @NotNull UUID playerUuid,
        final @NotNull ProviderDefinition providerDefinition,
        final @NotNull RBossBarPreference preference
    ) {
        final Map<String, String> optionValues = new LinkedHashMap<>();
        final Map<String, String> persistedValues = preference.getOptionValues();
        for (final ProviderOption option : providerDefinition.options()) {
            final String storedValue = persistedValues.get(option.key());
            optionValues.put(
                option.key(),
                storedValue == null ? option.defaultValue() : resolveStoredValue(option, storedValue)
            );
        }
        return new PreferenceSnapshot(playerUuid, providerDefinition.key(), preference.isEnabled(), optionValues);
    }

    private static @NotNull String resolveStoredValue(
        final @NotNull ProviderOption providerOption,
        final @NotNull String storedValue
    ) {
        try {
            return providerOption.normalizeValue(storedValue);
        } catch (final IllegalArgumentException ignored) {
            return providerOption.defaultValue();
        }
    }

    private static @NotNull String cacheKey(final @NotNull UUID playerUuid, final @NotNull String providerKey) {
        return RBossBarPreference.composeKey(playerUuid, providerKey);
    }

    private static @NotNull String normalizeProviderKey(final @NotNull String providerKey) {
        return providerKey.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
