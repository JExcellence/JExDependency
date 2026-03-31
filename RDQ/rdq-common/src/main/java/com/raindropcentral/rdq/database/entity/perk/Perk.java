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

package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rplatform.config.icon.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entity representing a perk definition in the RaindropQuests system.
 *
 * <p>This entity stores the configuration and metadata for a perk that players can unlock and activate.
 * It includes the perk's type, category, requirements, unlock rewards, and effect configuration.
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Setter
@Getter
@Entity
@Table(name = "rdq_perk")
public class Perk extends BaseEntity {

    /**
     * Unique identifier for this perk (e.g., "speed_boost", "flight").
     */
    @Column(name = "identifier", unique = true, nullable = false, length = 100)
    private String identifier;

    /**
     * The activation behavior type of this perk.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "perk_type", nullable = false, length = 50)
    private PerkType perkType;

    /**
     * The thematic category of this perk.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private PerkCategory category;

    /**
     * Whether this perk is currently enabled and available to players.
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /**
     * Display order for sorting perks in the UI.
     */
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    /**
     * The icon representing this perk in the UI.
     */
    @Convert(converter = IconSectionConverter.class)
    @Column(name = "icon", nullable = false, columnDefinition = "LONGTEXT")
    private IconSection icon;

    /**
     * Requirements that must be met to unlock this perk.
     */
    @OneToMany(mappedBy = "perk", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<PerkRequirement> requirements = new HashSet<>();

    /**
     * Rewards granted when this perk is unlocked.
     */
    @OneToMany(mappedBy = "perk", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<PerkUnlockReward> unlockRewards = new HashSet<>();

    /**
     * Perk-specific configuration stored as JSON.
     * Contains effect parameters, cooldowns, trigger chances, etc.
     */
    @Column(name = "config_json", columnDefinition = "LONGTEXT")
    private String configJson;

    /**
     * Version for optimistic locking.
     */
    @Version
    @Column(name = "version")
    private int version;

    /**
     * Timestamp when this perk was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this perk was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Protected no-argument constructor for JPA.
     */
    protected Perk() {
    }

    /**
     * Constructs a new {@code Perk} with the specified identifier, type, and category.
     *
     * @param identifier the unique identifier for this perk
     * @param perkType   the activation behavior type
     * @param category   the thematic category
     * @param icon       the icon for UI display
     */
    public Perk(
            @NotNull final String identifier,
            @NotNull final PerkType perkType,
            @NotNull final PerkCategory category,
            @NotNull final IconSection icon
    ) {
        this.identifier = identifier;
        this.perkType = perkType;
        this.category = category;
        this.icon = icon;
    }

    /**
     * Adds a requirement to this perk.
     *
     * @param requirement the requirement to add
     */
    public void addRequirement(@NotNull final PerkRequirement requirement) {
        this.requirements.add(requirement);
        requirement.setPerk(this);
    }

    /**
     * Removes a requirement from this perk.
     *
     * @param requirement the requirement to remove
     */
    public void removeRequirement(@NotNull final PerkRequirement requirement) {
        this.requirements.remove(requirement);
        requirement.setPerk(null);
    }

    /**
     * Adds an unlock reward to this perk.
     *
     * @param reward the reward to add
     */
    public void addUnlockReward(@NotNull final PerkUnlockReward reward) {
        this.unlockRewards.add(reward);
        reward.setPerk(this);
    }

    /**
     * Removes an unlock reward from this perk.
     *
     * @param reward the reward to remove
     */
    public void removeUnlockReward(@NotNull final PerkUnlockReward reward) {
        this.unlockRewards.remove(reward);
        reward.setPerk(null);
    }

    /**
     * Executes equals.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Perk that)) return false;

        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }

        return identifier != null && identifier.equals(that.identifier);
    }

    /**
     * Returns whether hCode.
     */
    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }

        return identifier != null ? identifier.hashCode() : System.identityHashCode(this);
    }

    /**
     * Executes toString.
     */
    @Override
    public String toString() {
        return "Perk{" +
                "id=" + getId() +
                ", identifier='" + identifier + '\'' +
                ", perkType=" + perkType +
                ", category=" + category +
                ", enabled=" + enabled +
                '}';
    }
}
