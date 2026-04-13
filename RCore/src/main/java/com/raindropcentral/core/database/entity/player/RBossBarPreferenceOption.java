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

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;

/**
 * Child option row for one {@link RBossBarPreference} entity.
 *
 * <p>Each option row stores one normalized key/value pair so providers can expose provider-specific
 * settings while RCore keeps the persistence model generic.</p>
 *
 * @author Codex
 * @since 2.1.0
 * @version 2.1.0
 */
@Entity
@Table(
    name = "r_boss_bar_preference_option",
    uniqueConstraints = @UniqueConstraint(name = "uk_r_boss_bar_pref_option", columnNames = {
        "preference_id",
        "option_key"
    })
)
public class RBossBarPreferenceOption extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "preference_id", nullable = false)
    private RBossBarPreference preference;

    @Column(name = "option_key", nullable = false, length = 64)
    private String optionKey;

    @Column(name = "option_value", nullable = false, length = 128)
    private String optionValue;

    protected RBossBarPreferenceOption() {}

    /**
     * Creates a new child option row.
     *
     * @param preference owning preference row
     * @param optionKey option identifier
     * @param optionValue stored value
     */
    public RBossBarPreferenceOption(
        final @NotNull RBossBarPreference preference,
        final @NotNull String optionKey,
        final @NotNull String optionValue
    ) {
        this.preference = Objects.requireNonNull(preference, "preference");
        this.optionKey = Objects.requireNonNull(optionKey, "optionKey");
        this.optionValue = Objects.requireNonNull(optionValue, "optionValue");
    }

    /**
     * Returns the normalized option key.
     *
     * @return option key
     */
    public @NotNull String getOptionKey() {
        return this.optionKey;
    }

    /**
     * Returns the stored option value.
     *
     * @return option value
     */
    public @NotNull String getOptionValue() {
        return this.optionValue;
    }

    /**
     * Updates the stored option value.
     *
     * @param optionValue replacement option value
     */
    public void setOptionValue(final @NotNull String optionValue) {
        this.optionValue = Objects.requireNonNull(optionValue, "optionValue");
    }
}
