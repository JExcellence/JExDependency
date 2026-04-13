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

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Public RCore service that centralizes boss-bar provider registration and player preferences.
 *
 * <p>Modules register one provider definition for each player-facing boss-bar-capable feature.
 * RCore then owns persisted enabled state, provider-specific option values, the shared
 * {@code /rc bossbar} command surface, and the generic in-game settings views.</p>
 *
 * @author Codex
 * @since 2.1.0
 * @version 2.1.0
 */
public interface RCoreBossBarService {

    /**
     * Returns every currently registered provider.
     *
     * @return immutable list of registered providers
     */
    @NotNull List<ProviderDefinition> getRegisteredProviders();

    /**
     * Resolves one provider definition by key.
     *
     * @param providerKey stable provider key
     * @return provider definition when registered
     */
    @NotNull Optional<ProviderDefinition> findProvider(@NotNull String providerKey);

    /**
     * Registers one boss-bar-capable provider with RCore.
     *
     * @param providerDefinition provider definition to register
     * @throws IllegalArgumentException if a provider with the same key is already registered
     */
    void registerProvider(@NotNull ProviderDefinition providerDefinition);

    /**
     * Unregisters a provider while leaving persisted player preferences intact.
     *
     * @param providerKey stable provider key
     * @return {@code true} when a provider was removed
     */
    boolean unregisterProvider(@NotNull String providerKey);

    /**
     * Resolves the current preference snapshot for one player and provider.
     *
     * <p>If no RCore row exists yet, the service lazily migrates the provider's legacy state or
     * creates a new row from provider defaults.</p>
     *
     * @param playerUuid target player UUID
     * @param providerKey stable provider key
     * @return resolved preference snapshot
     */
    @NotNull PreferenceSnapshot resolvePreferences(@NotNull UUID playerUuid, @NotNull String providerKey);

    /**
     * Sets the enabled state for one player and provider.
     *
     * @param playerUuid target player UUID
     * @param providerKey stable provider key
     * @param enabled replacement enabled state
     * @return updated preference snapshot
     */
    @NotNull PreferenceSnapshot setEnabled(@NotNull UUID playerUuid, @NotNull String providerKey, boolean enabled);

    /**
     * Toggles the enabled state for one player and provider.
     *
     * @param playerUuid target player UUID
     * @param providerKey stable provider key
     * @return updated preference snapshot
     */
    @NotNull PreferenceSnapshot toggleEnabled(@NotNull UUID playerUuid, @NotNull String providerKey);

    /**
     * Sets one provider-specific option value for one player.
     *
     * @param playerUuid target player UUID
     * @param providerKey stable provider key
     * @param optionKey option identifier
     * @param value replacement option value
     * @return updated preference snapshot
     */
    @NotNull PreferenceSnapshot setOption(
        @NotNull UUID playerUuid,
        @NotNull String providerKey,
        @NotNull String optionKey,
        @NotNull String value
    );

    /**
     * Opens the shared boss-bar settings root view for the supplied player.
     *
     * @param player player opening the settings view
     */
    void openSettingsView(@NotNull Player player);

    /**
     * Opens the provider-specific boss-bar settings detail view.
     *
     * @param player player opening the settings view
     * @param providerKey stable provider key
     */
    void openSettingsView(@NotNull Player player, @NotNull String providerKey);

    /**
     * Immutable provider definition registered by a companion plugin.
     *
     * @param key stable provider key
     * @param iconMaterial icon rendered in the shared settings views
     * @param nameTranslationKey translation key for the provider name
     * @param descriptionTranslationKey translation key for the provider description
     * @param defaultEnabled default enabled state when no stored preference exists
     * @param options provider-specific option definitions
     * @param legacyPreferenceResolver optional resolver used for lazy migration
     * @param preferenceChangeHandler optional callback invoked after RCore updates preferences
     */
    record ProviderDefinition(
        @NotNull String key,
        @NotNull Material iconMaterial,
        @NotNull String nameTranslationKey,
        @NotNull String descriptionTranslationKey,
        boolean defaultEnabled,
        @NotNull List<ProviderOption> options,
        @Nullable LegacyPreferenceResolver legacyPreferenceResolver,
        @Nullable PreferenceChangeHandler preferenceChangeHandler
    ) {

        /**
         * Creates a validated provider definition.
         */
        public ProviderDefinition {
            key = normalizeKey(key, "key");
            iconMaterial = Objects.requireNonNull(iconMaterial, "iconMaterial");
            nameTranslationKey = requireText(nameTranslationKey, "nameTranslationKey");
            descriptionTranslationKey = requireText(descriptionTranslationKey, "descriptionTranslationKey");
            options = List.copyOf(Objects.requireNonNull(options, "options"));

            final long uniqueOptionCount = options.stream().map(ProviderOption::key).distinct().count();
            if (uniqueOptionCount != options.size()) {
                throw new IllegalArgumentException("Provider options must use unique keys for provider " + key);
            }
        }

        /**
         * Resolves one option definition by key.
         *
         * @param optionKey option key to resolve
         * @return option definition when present
         */
        public @NotNull Optional<ProviderOption> findOption(final @NotNull String optionKey) {
            final String normalizedKey = normalizeKey(optionKey, "optionKey");
            return this.options.stream()
                .filter(option -> option.key().equals(normalizedKey))
                .findFirst();
        }
    }

    /**
     * Definition of one provider-specific option exposed through RCore.
     *
     * @param key stable option key
     * @param nameTranslationKey translation key for the option label
     * @param descriptionTranslationKey translation key for the option description
     * @param defaultValue default stored value
     * @param choices allowed values exposed to commands and the generic detail view
     */
    record ProviderOption(
        @NotNull String key,
        @NotNull String nameTranslationKey,
        @NotNull String descriptionTranslationKey,
        @NotNull String defaultValue,
        @NotNull List<ProviderOptionChoice> choices
    ) {

        /**
         * Creates a validated provider option definition.
         */
        public ProviderOption {
            key = normalizeKey(key, "key");
            nameTranslationKey = requireText(nameTranslationKey, "nameTranslationKey");
            descriptionTranslationKey = requireText(descriptionTranslationKey, "descriptionTranslationKey");
            defaultValue = requireText(defaultValue, "defaultValue");
            choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
            if (choices.isEmpty()) {
                throw new IllegalArgumentException("Provider option " + key + " must define at least one choice");
            }

            final long uniqueChoiceCount = choices.stream().map(ProviderOptionChoice::value).distinct().count();
            if (uniqueChoiceCount != choices.size()) {
                throw new IllegalArgumentException("Provider option " + key + " must define unique choice values");
            }

            normalizeChoiceValue(choices, key, defaultValue);
        }

        /**
         * Resolves the canonical stored value for the supplied input.
         *
         * @param rawValue raw command or migration value
         * @return canonical stored value
         * @throws IllegalArgumentException if the value is not one of the allowed choices
         */
        public @NotNull String normalizeValue(final @NotNull String rawValue) {
            return normalizeChoiceValue(this.choices, this.key, rawValue);
        }
    }

    /**
     * One allowed option value.
     *
     * @param value canonical stored value
     * @param labelTranslationKey translation key for the displayed choice label
     * @param descriptionTranslationKey optional translation key for choice-specific detail text
     */
    record ProviderOptionChoice(
        @NotNull String value,
        @NotNull String labelTranslationKey,
        @Nullable String descriptionTranslationKey
    ) {

        /**
         * Creates a validated option choice.
         */
        public ProviderOptionChoice {
            value = requireText(value, "value");
            labelTranslationKey = requireText(labelTranslationKey, "labelTranslationKey");
        }
    }

    /**
     * Immutable player preference snapshot returned by the service.
     *
     * @param playerUuid owning player UUID
     * @param providerKey stable provider key
     * @param enabled enabled state
     * @param options immutable provider-specific option map
     */
    record PreferenceSnapshot(
        @NotNull UUID playerUuid,
        @NotNull String providerKey,
        boolean enabled,
        @NotNull Map<String, String> options
    ) {

        /**
         * Creates a validated snapshot.
         */
        public PreferenceSnapshot {
            playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
            providerKey = normalizeKey(providerKey, "providerKey");
            options = Map.copyOf(Objects.requireNonNull(options, "options"));
        }
    }

    /**
     * Seed data used during lazy migration from plugin-owned preferences.
     *
     * @param enabled migrated enabled state
     * @param options migrated provider-specific option values
     */
    record PreferenceSeed(
        boolean enabled,
        @NotNull Map<String, String> options
    ) {

        /**
         * Creates a validated seed snapshot.
         */
        public PreferenceSeed {
            options = Map.copyOf(Objects.requireNonNull(options, "options"));
        }

        /**
         * Creates an enabled-only seed with no provider-specific options.
         *
         * @param enabled migrated enabled state
         * @return immutable seed
         */
        public static @NotNull PreferenceSeed enabledOnly(final boolean enabled) {
            return new PreferenceSeed(enabled, Map.of());
        }
    }

    /**
     * Resolves legacy provider data the first time RCore needs a stored preference row.
     */
    @FunctionalInterface
    interface LegacyPreferenceResolver {

        /**
         * Resolves the legacy preference seed for the supplied player.
         *
         * @param playerUuid player UUID being migrated
         * @param providerDefinition provider requesting migration
         * @return legacy seed, or {@code null} when no legacy state exists
         */
        @Nullable PreferenceSeed resolve(@NotNull UUID playerUuid, @NotNull ProviderDefinition providerDefinition);
    }

    /**
     * Callback invoked after RCore updates one player's provider preferences.
     */
    @FunctionalInterface
    interface PreferenceChangeHandler {

        /**
         * Invoked after RCore persists a new snapshot.
         *
         * @param playerUuid player UUID whose preference changed
         * @param preferenceSnapshot updated preference snapshot
         */
        void onPreferenceChanged(@NotNull UUID playerUuid, @NotNull PreferenceSnapshot preferenceSnapshot);
    }

    private static @NotNull String requireText(final @Nullable String value, final @NotNull String fieldName) {
        final String normalized = Objects.requireNonNull(value, fieldName).trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return normalized;
    }

    private static @NotNull String normalizeKey(final @Nullable String value, final @NotNull String fieldName) {
        return requireText(value, fieldName).toLowerCase(Locale.ROOT);
    }

    private static @NotNull String comparableValue(final @Nullable String value) {
        return requireText(value, "value")
            .replace('-', '_')
            .replace(' ', '_')
            .toUpperCase(Locale.ROOT);
    }

    private static @NotNull String normalizeChoiceValue(
        final @NotNull List<ProviderOptionChoice> choices,
        final @NotNull String optionKey,
        final @NotNull String rawValue
    ) {
        final String comparableInput = comparableValue(rawValue);
        return choices.stream()
            .map(ProviderOptionChoice::value)
            .filter(choiceValue -> comparableValue(choiceValue).equals(comparableInput))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Invalid value '" + rawValue + "' for option " + optionKey
            ));
    }
}
