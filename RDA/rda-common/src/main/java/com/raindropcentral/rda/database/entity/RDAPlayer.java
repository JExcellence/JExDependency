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

package com.raindropcentral.rda.database.entity;

import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Persistent player profile for Raindrop Abilities.
 *
 * <p>The profile stores the stable player identifier plus legacy mining and woodcutting columns
 * used to seed the new child skill-state table during migration.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.1.0
 */
@Entity
@Table(name = "rda_players")
public class RDAPlayer extends BaseEntity {

    @Column(name = "player_uuid", nullable = false, unique = true)
    @Convert(converter = UUIDConverter.class)
    private UUID playerUuid;

    @Column(name = "mining_xp", nullable = false)
    private long miningXp;

    @Column(name = "mining_level", nullable = false)
    private int miningLevel;

    @Column(name = "mining_prestige", nullable = false)
    private int miningPrestige;

    @Column(name = "woodcutting_xp", nullable = false)
    private long woodcuttingXp;

    @Column(name = "woodcutting_level", nullable = false)
    private int woodcuttingLevel;

    @Column(name = "woodcutting_prestige", nullable = false)
    private int woodcuttingPrestige;

    /**
     * Creates a player profile for the supplied UUID.
     *
     * @param playerUuid unique player identifier
     * @throws NullPointerException if {@code playerUuid} is {@code null}
     */
    public RDAPlayer(final @NotNull UUID playerUuid) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected RDAPlayer() {
    }

    /**
     * Returns the stable cache identifier for this player entity.
     *
     * @return player UUID
     */
    public @NotNull UUID getIdentifier() {
        return this.playerUuid;
    }

    /**
     * Returns the stored player UUID.
     *
     * @return unique player identifier
     */
    public @NotNull UUID getPlayerUuid() {
        return this.playerUuid;
    }

    /**
     * Returns the persisted mining XP carried inside the player's current mining level.
     *
     * @return current mining XP progress
     */
    public long getMiningXp() {
        return this.miningXp;
    }

    /**
     * Updates the persisted mining XP carried inside the player's current mining level.
     *
     * @param miningXp mining XP to store
     */
    public void setMiningXp(final long miningXp) {
        this.miningXp = Math.max(0L, miningXp);
    }

    /**
     * Returns the player's internal mining level.
     *
     * @return mining level
     */
    public int getMiningLevel() {
        return this.miningLevel;
    }

    /**
     * Updates the player's internal mining level.
     *
     * @param miningLevel level to store
     */
    public void setMiningLevel(final int miningLevel) {
        this.miningLevel = Math.max(0, miningLevel);
    }

    /**
     * Returns the player's completed mining prestige count.
     *
     * @return mining prestige count
     */
    public int getMiningPrestige() {
        return this.miningPrestige;
    }

    /**
     * Updates the player's completed mining prestige count.
     *
     * @param miningPrestige prestige count to store
     */
    public void setMiningPrestige(final int miningPrestige) {
        this.miningPrestige = Math.max(0, miningPrestige);
    }

    /**
     * Returns the persisted woodcutting XP carried inside the player's current woodcutting level.
     *
     * @return current woodcutting XP progress
     */
    public long getWoodcuttingXp() {
        return this.woodcuttingXp;
    }

    /**
     * Updates the persisted woodcutting XP carried inside the player's current woodcutting level.
     *
     * @param woodcuttingXp woodcutting XP to store
     */
    public void setWoodcuttingXp(final long woodcuttingXp) {
        this.woodcuttingXp = Math.max(0L, woodcuttingXp);
    }

    /**
     * Returns the player's internal woodcutting level.
     *
     * @return woodcutting level
     */
    public int getWoodcuttingLevel() {
        return this.woodcuttingLevel;
    }

    /**
     * Updates the player's internal woodcutting level.
     *
     * @param woodcuttingLevel level to store
     */
    public void setWoodcuttingLevel(final int woodcuttingLevel) {
        this.woodcuttingLevel = Math.max(0, woodcuttingLevel);
    }

    /**
     * Returns the player's completed woodcutting prestige count.
     *
     * @return woodcutting prestige count
     */
    public int getWoodcuttingPrestige() {
        return this.woodcuttingPrestige;
    }

    /**
     * Updates the player's completed woodcutting prestige count.
     *
     * @param woodcuttingPrestige prestige count to store
     */
    public void setWoodcuttingPrestige(final int woodcuttingPrestige) {
        this.woodcuttingPrestige = Math.max(0, woodcuttingPrestige);
    }
}
