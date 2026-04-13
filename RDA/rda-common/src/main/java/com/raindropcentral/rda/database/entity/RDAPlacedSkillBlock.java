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

import com.raindropcentral.rda.SkillType;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Persisted placed-block marker used to suppress natural-only block-break skill XP.
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
@Entity
@Table(name = "rda_placed_skill_blocks")
public class RDAPlacedSkillBlock extends BaseEntity {

    @Column(name = "location_key", nullable = false, unique = true, length = 255)
    private String locationKey;

    @Column(name = "skill_id", nullable = false, length = 32)
    private String skillId;

    @Column(name = "world_name", nullable = false, length = 128)
    private String worldName;

    @Column(name = "block_x", nullable = false)
    private int x;

    @Column(name = "block_y", nullable = false)
    private int y;

    @Column(name = "block_z", nullable = false)
    private int z;

    @Column(name = "material_name", nullable = false, length = 64)
    private String materialName;

    /**
     * Creates a placed-block marker for the supplied skill and block.
     *
     * @param skillType owning skill type
     * @param block placed tracked block
     */
    public RDAPlacedSkillBlock(final @NotNull SkillType skillType, final @NotNull Block block) {
        Objects.requireNonNull(block, "block");
        this.skillId = Objects.requireNonNull(skillType, "skillType").getId();
        this.locationKey = toLocationKey(skillType, block);
        this.worldName = Objects.requireNonNull(block.getWorld(), "world").getName();
        this.x = block.getX();
        this.y = block.getY();
        this.z = block.getZ();
        this.materialName = block.getType().name();
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected RDAPlacedSkillBlock() {
    }

    /**
     * Returns the stable location key for this marker.
     *
     * @return location key
     */
    public @NotNull String getLocationKey() {
        return this.locationKey;
    }

    /**
     * Converts a block location into the stable suppression key format.
     *
     * @param skillType owning skill type
     * @param block block to encode
     * @return stable world and coordinate key
     */
    public static @NotNull String toLocationKey(final @NotNull SkillType skillType, final @NotNull Block block) {
        Objects.requireNonNull(block, "block");
        return Objects.requireNonNull(skillType, "skillType").getId()
            + ":" + Objects.requireNonNull(block.getWorld(), "world").getName()
            + ":" + block.getX()
            + ":" + block.getY()
            + ":" + block.getZ();
    }
}
