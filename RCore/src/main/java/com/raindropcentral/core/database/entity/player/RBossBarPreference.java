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

package com.raindropcentral.core.database.entity.player;

import de.jexcellence.hibernate.converter.UuidBytesConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Persists one player's preference row for one registered boss-bar provider.
 *
 * <p>The entity stores the canonical enabled state in RCore and owns a child collection of
 * provider-specific option values. A unique constraint prevents duplicate rows for the same
 * {@code player + provider} pair so lazy migration can safely create the preference the first time
 * a module requests it.</p>
 *
 * @author Codex
 * @since 2.1.0
 * @version 2.1.0
 */
@Entity
@Table(
    name = "r_boss_bar_preference",
    uniqueConstraints = @UniqueConstraint(name = "uk_r_boss_bar_player_provider", columnNames = {
        "player_uuid",
        "provider_key"
    })
)
public class RBossBarPreference extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "player_uuid", nullable = false)
    @Convert(converter = UuidBytesConverter.class)
    private UUID playerUuid;

    @Column(name = "provider_key", nullable = false, length = 64)
    private String providerKey;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @OneToMany(mappedBy = "preference", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("optionKey ASC")
    private Set<RBossBarPreferenceOption> options = new LinkedHashSet<>();

    protected RBossBarPreference() {}

    /**
     * Creates a new boss-bar preference row.
     *
     * @param playerUuid owning player UUID
     * @param providerKey stable provider key
     * @param enabled initial enabled state
     */
    public RBossBarPreference(
        final @NotNull UUID playerUuid,
        final @NotNull String providerKey,
        final boolean enabled
    ) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.providerKey = normalizeKey(providerKey, "providerKey");
        this.enabled = enabled;
    }

    /**
     * Returns the owning player UUID.
     *
     * @return owning player UUID
     */
    public @NotNull UUID getPlayerUuid() {
        return this.playerUuid;
    }

    /**
     * Returns the stable provider key.
     *
     * @return provider key
     */
    public @NotNull String getProviderKey() {
        return this.providerKey;
    }

    /**
     * Returns whether the provider is enabled for the player.
     *
     * @return {@code true} when enabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Updates the enabled state.
     *
     * @param enabled replacement enabled state
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns an immutable snapshot of provider-specific option values.
     *
     * @return option map keyed by option identifier
     */
    public @NotNull Map<String, String> getOptionValues() {
        final Map<String, String> values = new LinkedHashMap<>();
        for (final RBossBarPreferenceOption option : this.options) {
            values.put(option.getOptionKey(), option.getOptionValue());
        }
        return Map.copyOf(values);
    }

    /**
     * Reads one option value by key.
     *
     * @param optionKey option key to resolve
     * @return stored option value, or {@code null} when absent
     */
    public @Nullable String getOptionValue(final @NotNull String optionKey) {
        final String normalizedKey = normalizeKey(optionKey, "optionKey");
        for (final RBossBarPreferenceOption option : this.options) {
            if (option.getOptionKey().equals(normalizedKey)) {
                return option.getOptionValue();
            }
        }
        return null;
    }

    /**
     * Creates or replaces an option value on the preference row.
     *
     * @param optionKey option identifier
     * @param optionValue stored option value
     */
    public void putOptionValue(final @NotNull String optionKey, final @NotNull String optionValue) {
        final String normalizedKey = normalizeKey(optionKey, "optionKey");
        final String normalizedValue = Objects.requireNonNull(optionValue, "optionValue");
        for (final RBossBarPreferenceOption option : this.options) {
            if (option.getOptionKey().equals(normalizedKey)) {
                option.setOptionValue(normalizedValue);
                return;
            }
        }
        this.options.add(new RBossBarPreferenceOption(this, normalizedKey, normalizedValue));
    }

    /**
     * Returns the repository cache key composed from the player UUID and provider key.
     *
     * @return stable composite cache key
     */
    public @NotNull String getCompositeKey() {
        return composeKey(this.playerUuid, this.providerKey);
    }

    /**
     * Composes a stable repository cache key for the supplied identifiers.
     *
     * @param playerUuid owning player UUID
     * @param providerKey provider key
     * @return stable composite cache key
     */
    public static @NotNull String composeKey(
        final @NotNull UUID playerUuid,
        final @NotNull String providerKey
    ) {
        return Objects.requireNonNull(playerUuid, "playerUuid")
            + ":"
            + normalizeKey(providerKey, "providerKey");
    }

    private static @NotNull String normalizeKey(final @NotNull String rawKey, final @NotNull String fieldName) {
        final String normalized = Objects.requireNonNull(rawKey, fieldName).trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return normalized;
    }
}
