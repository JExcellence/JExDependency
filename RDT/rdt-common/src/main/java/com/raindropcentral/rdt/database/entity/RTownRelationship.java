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

package com.raindropcentral.rdt.database.entity;

import com.raindropcentral.rdt.utils.TownRelationshipState;
import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Persistent unordered diplomacy relationship for one pair of towns.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(
    name = "rdt_town_relationships",
    uniqueConstraints = @UniqueConstraint(columnNames = {"primary_town_uuid", "secondary_town_uuid"})
)
public class RTownRelationship extends BaseEntity {

    @Column(name = "primary_town_uuid", nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID primaryTownUuid;

    @Column(name = "secondary_town_uuid", nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID secondaryTownUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "confirmed_state", nullable = false, length = 32)
    private TownRelationshipState confirmedState;

    @Enumerated(EnumType.STRING)
    @Column(name = "pending_state", length = 32)
    private TownRelationshipState pendingState;

    @Column(name = "pending_requester_town_uuid")
    @Convert(converter = UUIDConverter.class)
    private UUID pendingRequesterTownUuid;

    @Column(name = "cooldown_until_millis", nullable = false)
    private long cooldownUntilMillis;

    /**
     * Creates a town-pair relationship using stable UUID ordering.
     *
     * @param leftTownUuid first town UUID
     * @param rightTownUuid second town UUID
     */
    public RTownRelationship(final @NotNull UUID leftTownUuid, final @NotNull UUID rightTownUuid) {
        final TownPair townPair = normalizePair(leftTownUuid, rightTownUuid);
        this.primaryTownUuid = townPair.primaryTownUuid();
        this.secondaryTownUuid = townPair.secondaryTownUuid();
        this.confirmedState = TownRelationshipState.NEUTRAL;
        this.cooldownUntilMillis = 0L;
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected RTownRelationship() {
    }

    /**
     * Returns the lexicographically first town UUID in the stored pair.
     *
     * @return first ordered town UUID
     */
    public @NotNull UUID getPrimaryTownUuid() {
        return Objects.requireNonNull(this.primaryTownUuid, "primaryTownUuid");
    }

    /**
     * Returns the lexicographically second town UUID in the stored pair.
     *
     * @return second ordered town UUID
     */
    public @NotNull UUID getSecondaryTownUuid() {
        return Objects.requireNonNull(this.secondaryTownUuid, "secondaryTownUuid");
    }

    /**
     * Returns the stable cache key for this unordered town pair.
     *
     * @return stable pair cache key
     */
    public @NotNull String getPairKey() {
        return buildPairKey(this.getPrimaryTownUuid(), this.getSecondaryTownUuid());
    }

    /**
     * Returns the current confirmed diplomacy state.
     *
     * @return confirmed diplomacy state
     */
    public @NotNull TownRelationshipState getConfirmedState() {
        return this.confirmedState == null ? TownRelationshipState.NEUTRAL : this.confirmedState;
    }

    /**
     * Replaces the current confirmed diplomacy state.
     *
     * @param confirmedState replacement confirmed state
     */
    public void setConfirmedState(final @NotNull TownRelationshipState confirmedState) {
        this.confirmedState = Objects.requireNonNull(confirmedState, "confirmedState");
    }

    /**
     * Returns the pending requested diplomacy state.
     *
     * @return pending requested state, or {@code null} when no request is pending
     */
    public @Nullable TownRelationshipState getPendingState() {
        return this.pendingState;
    }

    /**
     * Replaces the pending requested diplomacy state.
     *
     * @param pendingState replacement pending state, or {@code null} to clear it
     */
    public void setPendingState(final @Nullable TownRelationshipState pendingState) {
        this.pendingState = pendingState;
    }

    /**
     * Returns the town UUID that created the current pending request.
     *
     * @return pending requester town UUID, or {@code null} when no request is pending
     */
    public @Nullable UUID getPendingRequesterTownUuid() {
        return this.pendingRequesterTownUuid;
    }

    /**
     * Replaces the town UUID that created the current pending request.
     *
     * @param pendingRequesterTownUuid replacement requester town UUID, or {@code null} to clear it
     */
    public void setPendingRequesterTownUuid(final @Nullable UUID pendingRequesterTownUuid) {
        this.pendingRequesterTownUuid = pendingRequesterTownUuid;
    }

    /**
     * Returns the cooldown expiry timestamp in epoch milliseconds.
     *
     * @return cooldown expiry timestamp
     */
    public long getCooldownUntilMillis() {
        return Math.max(0L, this.cooldownUntilMillis);
    }

    /**
     * Replaces the cooldown expiry timestamp in epoch milliseconds.
     *
     * @param cooldownUntilMillis replacement cooldown expiry timestamp
     */
    public void setCooldownUntilMillis(final long cooldownUntilMillis) {
        this.cooldownUntilMillis = Math.max(0L, cooldownUntilMillis);
    }

    /**
     * Returns whether this stored pair contains the supplied town UUID.
     *
     * @param townUuid town UUID to inspect
     * @return {@code true} when the town belongs to this pair
     */
    public boolean containsTown(final @Nullable UUID townUuid) {
        return townUuid != null
            && (Objects.equals(this.primaryTownUuid, townUuid) || Objects.equals(this.secondaryTownUuid, townUuid));
    }

    /**
     * Returns whether this stored relationship matches one unordered town pair.
     *
     * @param leftTownUuid first town UUID
     * @param rightTownUuid second town UUID
     * @return {@code true} when the stored pair matches the supplied pair
     */
    public boolean matchesTownPair(
        final @NotNull UUID leftTownUuid,
        final @NotNull UUID rightTownUuid
    ) {
        return Objects.equals(this.getPairKey(), buildPairKey(leftTownUuid, rightTownUuid));
    }

    /**
     * Returns the other town UUID in this pair.
     *
     * @param townUuid known town UUID from this pair
     * @return opposite town UUID, or {@code null} when the supplied UUID is not part of this pair
     */
    public @Nullable UUID getOtherTownUuid(final @Nullable UUID townUuid) {
        if (!this.containsTown(townUuid)) {
            return null;
        }
        return Objects.equals(this.primaryTownUuid, townUuid) ? this.secondaryTownUuid : this.primaryTownUuid;
    }

    /**
     * Clears the stored pending request fields.
     */
    public void clearPendingState() {
        this.pendingState = null;
        this.pendingRequesterTownUuid = null;
    }

    /**
     * Builds the stable cache key for an unordered town pair.
     *
     * @param leftTownUuid first town UUID
     * @param rightTownUuid second town UUID
     * @return stable unordered pair cache key
     */
    public static @NotNull String buildPairKey(
        final @NotNull UUID leftTownUuid,
        final @NotNull UUID rightTownUuid
    ) {
        final TownPair townPair = normalizePair(leftTownUuid, rightTownUuid);
        return townPair.primaryTownUuid() + ":" + townPair.secondaryTownUuid();
    }

    private static @NotNull TownPair normalizePair(
        final @NotNull UUID leftTownUuid,
        final @NotNull UUID rightTownUuid
    ) {
        final UUID validatedLeftTownUuid = Objects.requireNonNull(leftTownUuid, "leftTownUuid");
        final UUID validatedRightTownUuid = Objects.requireNonNull(rightTownUuid, "rightTownUuid");
        if (Objects.equals(validatedLeftTownUuid, validatedRightTownUuid)) {
            throw new IllegalArgumentException("Town relationships require two different towns.");
        }
        return validatedLeftTownUuid.compareTo(validatedRightTownUuid) <= 0
            ? new TownPair(validatedLeftTownUuid, validatedRightTownUuid)
            : new TownPair(validatedRightTownUuid, validatedLeftTownUuid);
    }

    private record TownPair(UUID primaryTownUuid, UUID secondaryTownUuid) {
    }
}
