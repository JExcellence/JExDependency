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

package com.raindropcentral.rplatform.service;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Reflective bridge for the optional {@code RCoreBossBarService}.
 *
 * <p>Feature modules use this bridge so they can register boss-bar providers with {@code RCore}
 * when it is installed, while still compiling and booting without a direct compile-time
 * dependency on the {@code RCore} plugin module.</p>
 *
 * @author Codex
 * @since 1.0.0
 * @version 1.0.0
 */
public final class RCoreBossBarBridge {

    private static final String SERVICE_CLASS_NAME = "com.raindropcentral.core.service.RCoreBossBarService";
    private static final String PROVIDER_DEFINITION_CLASS_NAME = SERVICE_CLASS_NAME + "$ProviderDefinition";
    private static final String PROVIDER_OPTION_CLASS_NAME = SERVICE_CLASS_NAME + "$ProviderOption";
    private static final String PROVIDER_OPTION_CHOICE_CLASS_NAME = SERVICE_CLASS_NAME + "$ProviderOptionChoice";
    private static final String PREFERENCE_SEED_CLASS_NAME = SERVICE_CLASS_NAME + "$PreferenceSeed";
    private static final String LEGACY_RESOLVER_CLASS_NAME = SERVICE_CLASS_NAME + "$LegacyPreferenceResolver";
    private static final String CHANGE_HANDLER_CLASS_NAME = SERVICE_CLASS_NAME + "$PreferenceChangeHandler";

    private final Object service;
    private final Class<?> legacyResolverClass;
    private final Class<?> changeHandlerClass;
    private final Constructor<?> providerDefinitionConstructor;
    private final Constructor<?> providerOptionConstructor;
    private final Constructor<?> providerOptionChoiceConstructor;
    private final Constructor<?> preferenceSeedConstructor;
    private final Method registerProviderMethod;
    private final Method unregisterProviderMethod;
    private final Method resolvePreferencesMethod;
    private final Method toggleEnabledMethod;
    private final Method setOptionMethod;
    private final Method openSettingsViewMethod;
    private final Method providerKeyMethod;
    private final Method snapshotProviderKeyMethod;
    private final Method snapshotEnabledMethod;
    private final Method snapshotOptionsMethod;

    private RCoreBossBarBridge(
        final @NotNull Object service,
        final @NotNull Class<?> legacyResolverClass,
        final @NotNull Class<?> changeHandlerClass,
        final @NotNull Constructor<?> providerDefinitionConstructor,
        final @NotNull Constructor<?> providerOptionConstructor,
        final @NotNull Constructor<?> providerOptionChoiceConstructor,
        final @NotNull Constructor<?> preferenceSeedConstructor,
        final @NotNull Method registerProviderMethod,
        final @NotNull Method unregisterProviderMethod,
        final @NotNull Method resolvePreferencesMethod,
        final @NotNull Method toggleEnabledMethod,
        final @NotNull Method setOptionMethod,
        final @NotNull Method openSettingsViewMethod,
        final @NotNull Method providerKeyMethod,
        final @NotNull Method snapshotProviderKeyMethod,
        final @NotNull Method snapshotEnabledMethod,
        final @NotNull Method snapshotOptionsMethod
    ) {
        this.service = service;
        this.legacyResolverClass = legacyResolverClass;
        this.changeHandlerClass = changeHandlerClass;
        this.providerDefinitionConstructor = providerDefinitionConstructor;
        this.providerOptionConstructor = providerOptionConstructor;
        this.providerOptionChoiceConstructor = providerOptionChoiceConstructor;
        this.preferenceSeedConstructor = preferenceSeedConstructor;
        this.registerProviderMethod = registerProviderMethod;
        this.unregisterProviderMethod = unregisterProviderMethod;
        this.resolvePreferencesMethod = resolvePreferencesMethod;
        this.toggleEnabledMethod = toggleEnabledMethod;
        this.setOptionMethod = setOptionMethod;
        this.openSettingsViewMethod = openSettingsViewMethod;
        this.providerKeyMethod = providerKeyMethod;
        this.snapshotProviderKeyMethod = snapshotProviderKeyMethod;
        this.snapshotEnabledMethod = snapshotEnabledMethod;
        this.snapshotOptionsMethod = snapshotOptionsMethod;
    }

    /**
     * Creates a bridge for the currently installed {@code RCoreBossBarService}.
     *
     * @return initialized bridge, or {@code null} when RCore is not installed or not registered
     */
    public static @Nullable RCoreBossBarBridge create() {
        try {
            final Class<?> serviceClass = Class.forName(SERVICE_CLASS_NAME);
            final Object service = Bukkit.getServicesManager().load(castServiceClass(serviceClass));
            if (service == null) {
                return null;
            }

            final Class<?> providerDefinitionClass = Class.forName(PROVIDER_DEFINITION_CLASS_NAME);
            final Class<?> providerOptionClass = Class.forName(PROVIDER_OPTION_CLASS_NAME);
            final Class<?> providerOptionChoiceClass = Class.forName(PROVIDER_OPTION_CHOICE_CLASS_NAME);
            final Class<?> preferenceSeedClass = Class.forName(PREFERENCE_SEED_CLASS_NAME);
            final Class<?> legacyResolverClass = Class.forName(LEGACY_RESOLVER_CLASS_NAME);
            final Class<?> changeHandlerClass = Class.forName(CHANGE_HANDLER_CLASS_NAME);

            return new RCoreBossBarBridge(
                service,
                legacyResolverClass,
                changeHandlerClass,
                providerDefinitionClass.getDeclaredConstructor(
                    String.class,
                    Material.class,
                    String.class,
                    String.class,
                    boolean.class,
                    List.class,
                    legacyResolverClass,
                    changeHandlerClass
                ),
                providerOptionClass.getDeclaredConstructor(
                    String.class,
                    String.class,
                    String.class,
                    String.class,
                    List.class
                ),
                providerOptionChoiceClass.getDeclaredConstructor(
                    String.class,
                    String.class,
                    String.class
                ),
                preferenceSeedClass.getDeclaredConstructor(boolean.class, Map.class),
                serviceClass.getMethod("registerProvider", providerDefinitionClass),
                serviceClass.getMethod("unregisterProvider", String.class),
                serviceClass.getMethod("resolvePreferences", UUID.class, String.class),
                serviceClass.getMethod("toggleEnabled", UUID.class, String.class),
                serviceClass.getMethod("setOption", UUID.class, String.class, String.class, String.class),
                serviceClass.getMethod("openSettingsView", Player.class, String.class),
                providerDefinitionClass.getMethod("key"),
                Class.forName(SERVICE_CLASS_NAME + "$PreferenceSnapshot").getMethod("providerKey"),
                Class.forName(SERVICE_CLASS_NAME + "$PreferenceSnapshot").getMethod("enabled"),
                Class.forName(SERVICE_CLASS_NAME + "$PreferenceSnapshot").getMethod("options")
            );
        } catch (final ClassNotFoundException exception) {
            return null;
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to initialize the optional RCore boss-bar bridge.", exception);
        }
    }

    /**
     * Registers one provider with RCore.
     *
     * @param registration provider registration metadata
     * @throws IllegalStateException when the reflective RCore call fails
     */
    public void registerProvider(final @NotNull ProviderRegistration registration) {
        Objects.requireNonNull(registration, "registration");
        try {
            this.registerProviderMethod.invoke(this.service, this.createProviderDefinition(registration));
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to register RCore boss-bar provider " + registration.key(), exception);
        }
    }

    /**
     * Unregisters one provider from RCore.
     *
     * @param providerKey provider key to remove
     * @return {@code true} when a provider was removed
     */
    public boolean unregisterProvider(final @NotNull String providerKey) {
        try {
            return (boolean) this.unregisterProviderMethod.invoke(this.service, providerKey);
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to unregister RCore boss-bar provider " + providerKey, exception);
        }
    }

    /**
     * Resolves one player's current provider preferences.
     *
     * @param playerUuid player UUID to inspect
     * @param providerKey provider key to resolve
     * @return immutable preference snapshot
     */
    public @NotNull PreferenceSnapshot resolvePreferences(
        final @NotNull UUID playerUuid,
        final @NotNull String providerKey
    ) {
        try {
            return this.toSnapshot(this.resolvePreferencesMethod.invoke(this.service, playerUuid, providerKey));
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException(
                "Failed to resolve RCore boss-bar preferences for " + providerKey + " and player " + playerUuid,
                exception
            );
        }
    }

    /**
     * Toggles one player's provider enabled state.
     *
     * @param playerUuid player UUID to update
     * @param providerKey provider key to update
     * @return updated preference snapshot
     */
    public @NotNull PreferenceSnapshot toggleEnabled(
        final @NotNull UUID playerUuid,
        final @NotNull String providerKey
    ) {
        try {
            return this.toSnapshot(this.toggleEnabledMethod.invoke(this.service, playerUuid, providerKey));
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException(
                "Failed to toggle the RCore boss-bar provider " + providerKey + " for player " + playerUuid,
                exception
            );
        }
    }

    /**
     * Updates one provider option for one player.
     *
     * @param playerUuid player UUID to update
     * @param providerKey provider key to update
     * @param optionKey provider option key
     * @param value replacement option value
     * @return updated preference snapshot
     */
    public @NotNull PreferenceSnapshot setOption(
        final @NotNull UUID playerUuid,
        final @NotNull String providerKey,
        final @NotNull String optionKey,
        final @NotNull String value
    ) {
        try {
            return this.toSnapshot(this.setOptionMethod.invoke(this.service, playerUuid, providerKey, optionKey, value));
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException(
                "Failed to update the RCore boss-bar option " + optionKey + " for provider " + providerKey,
                exception
            );
        }
    }

    /**
     * Opens the RCore provider settings view for one player.
     *
     * @param player player opening the settings view
     * @param providerKey provider key to open
     */
    public void openSettingsView(final @NotNull Player player, final @NotNull String providerKey) {
        try {
            this.openSettingsViewMethod.invoke(this.service, player, providerKey);
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to open RCore boss-bar settings for provider " + providerKey, exception);
        }
    }

    private @NotNull Object createProviderDefinition(final @NotNull ProviderRegistration registration)
        throws ReflectiveOperationException {
        final List<Object> optionDefinitions = registration.options().stream()
            .map(this::createProviderOptionUnchecked)
            .toList();

        final Object legacyResolverProxy = registration.legacyPreferenceResolver() == null
            ? null
            : Proxy.newProxyInstance(
                this.legacyResolverClass.getClassLoader(),
                new Class<?>[]{this.legacyResolverClass},
                this.createLegacyResolverHandler(registration.legacyPreferenceResolver())
            );
        final Object changeHandlerProxy = registration.preferenceChangeHandler() == null
            ? null
            : Proxy.newProxyInstance(
                this.changeHandlerClass.getClassLoader(),
                new Class<?>[]{this.changeHandlerClass},
                this.createChangeHandler(registration.preferenceChangeHandler())
            );
        return this.providerDefinitionConstructor.newInstance(
            registration.key(),
            registration.iconMaterial(),
            registration.nameTranslationKey(),
            registration.descriptionTranslationKey(),
            registration.defaultEnabled(),
            optionDefinitions,
            legacyResolverProxy,
            changeHandlerProxy
        );
    }

    private @NotNull Object createProviderOptionUnchecked(final @NotNull ProviderOption option) {
        try {
            return this.createProviderOption(option);
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to create reflected RCore boss-bar option " + option.key(), exception);
        }
    }

    private @NotNull Object createProviderOption(final @NotNull ProviderOption option) throws ReflectiveOperationException {
        final List<Object> choiceDefinitions = option.choices().stream()
            .map(this::createOptionChoiceUnchecked)
            .toList();
        return this.providerOptionConstructor.newInstance(
            option.key(),
            option.nameTranslationKey(),
            option.descriptionTranslationKey(),
            option.defaultValue(),
            choiceDefinitions
        );
    }

    private @NotNull Object createOptionChoiceUnchecked(final @NotNull ProviderOptionChoice choice) {
        try {
            return this.providerOptionChoiceConstructor.newInstance(
                choice.value(),
                choice.labelTranslationKey(),
                choice.descriptionTranslationKey()
            );
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to create reflected RCore boss-bar option choice " + choice.value(), exception);
        }
    }

    private @NotNull InvocationHandler createLegacyResolverHandler(
        final @NotNull LegacyPreferenceResolver legacyPreferenceResolver
    ) {
        return (proxy, method, args) -> {
            if (!"resolve".equals(method.getName())) {
                return method.invoke(this, args);
            }
            final UUID playerUuid = (UUID) args[0];
            final String providerKey = (String) this.providerKeyMethod.invoke(args[1]);
            final PreferenceSeed preferenceSeed = legacyPreferenceResolver.resolve(playerUuid, providerKey);
            return preferenceSeed == null ? null : this.preferenceSeedConstructor.newInstance(
                preferenceSeed.enabled(),
                preferenceSeed.options()
            );
        };
    }

    private @NotNull InvocationHandler createChangeHandler(final @NotNull PreferenceChangeHandler changeHandler) {
        return (proxy, method, args) -> {
            if (!"onPreferenceChanged".equals(method.getName())) {
                return method.invoke(this, args);
            }
            changeHandler.onPreferenceChanged((UUID) args[0], this.toSnapshot(args[1]));
            return null;
        };
    }

    private @NotNull PreferenceSnapshot toSnapshot(final @NotNull Object snapshot) throws ReflectiveOperationException {
        return new PreferenceSnapshot(
            (String) this.snapshotProviderKeyMethod.invoke(snapshot),
            (boolean) this.snapshotEnabledMethod.invoke(snapshot),
            Map.copyOf(castOptionMap(this.snapshotOptionsMethod.invoke(snapshot)))
        );
    }

    @SuppressWarnings("unchecked")
    private static @NotNull Map<String, String> castOptionMap(final @NotNull Object rawOptions) {
        return (Map<String, String>) rawOptions;
    }

    @SuppressWarnings("unchecked")
    private static @NotNull Class<Object> castServiceClass(final @NotNull Class<?> rawServiceClass) {
        return (Class<Object>) rawServiceClass;
    }

    /**
     * Declarative provider registration payload used by feature modules.
     *
     * @param key stable provider key
     * @param iconMaterial icon rendered in the shared RCore view
     * @param nameTranslationKey translation key for the provider name
     * @param descriptionTranslationKey translation key for the provider description
     * @param defaultEnabled default enabled state
     * @param options provider-specific option definitions
     * @param legacyPreferenceResolver optional lazy migration resolver
     * @param preferenceChangeHandler optional live refresh callback
     */
    public record ProviderRegistration(
        @NotNull String key,
        @NotNull Material iconMaterial,
        @NotNull String nameTranslationKey,
        @NotNull String descriptionTranslationKey,
        boolean defaultEnabled,
        @NotNull List<ProviderOption> options,
        @Nullable LegacyPreferenceResolver legacyPreferenceResolver,
        @Nullable PreferenceChangeHandler preferenceChangeHandler
    ) {
    }

    /**
     * Definition of one provider-specific option.
     *
     * @param key stable option key
     * @param nameTranslationKey translation key for the option label
     * @param descriptionTranslationKey translation key for the option description
     * @param defaultValue default option value
     * @param choices allowed option choices
     */
    public record ProviderOption(
        @NotNull String key,
        @NotNull String nameTranslationKey,
        @NotNull String descriptionTranslationKey,
        @NotNull String defaultValue,
        @NotNull List<ProviderOptionChoice> choices
    ) {
    }

    /**
     * One allowed provider-option value.
     *
     * @param value canonical stored value
     * @param labelTranslationKey translation key for the choice label
     * @param descriptionTranslationKey optional translation key for the choice description
     */
    public record ProviderOptionChoice(
        @NotNull String value,
        @NotNull String labelTranslationKey,
        @Nullable String descriptionTranslationKey
    ) {
    }

    /**
     * Immutable preference snapshot exposed to feature modules.
     *
     * @param providerKey owning provider key
     * @param enabled enabled state
     * @param options option values keyed by option identifier
     */
    public record PreferenceSnapshot(
        @NotNull String providerKey,
        boolean enabled,
        @NotNull Map<String, String> options
    ) {
    }

    /**
     * Lazy migration seed supplied by a feature module when no RCore row exists yet.
     *
     * @param enabled enabled state to seed
     * @param options option values to seed
     */
    public record PreferenceSeed(
        boolean enabled,
        @NotNull Map<String, String> options
    ) {

        /**
         * Creates an enabled-only seed with no option values.
         *
         * @param enabled enabled state to seed
         * @return immutable seed
         */
        public static @NotNull PreferenceSeed enabledOnly(final boolean enabled) {
            return new PreferenceSeed(enabled, Map.of());
        }
    }

    /**
     * Resolves a lazy migration seed for one player and provider.
     */
    @FunctionalInterface
    public interface LegacyPreferenceResolver {

        /**
         * Resolves the legacy preference seed for the supplied player and provider.
         *
         * @param playerUuid player UUID being migrated
         * @param providerKey provider key requesting migration
         * @return legacy seed, or {@code null} when no legacy state exists
         */
        @Nullable PreferenceSeed resolve(@NotNull UUID playerUuid, @NotNull String providerKey);
    }

    /**
     * Callback invoked after RCore persists one provider preference change.
     */
    @FunctionalInterface
    public interface PreferenceChangeHandler {

        /**
         * Handles one updated preference snapshot.
         *
         * @param playerUuid player UUID whose preference changed
         * @param preferenceSnapshot updated preference snapshot
         */
        void onPreferenceChanged(@NotNull UUID playerUuid, @NotNull PreferenceSnapshot preferenceSnapshot);
    }
}
