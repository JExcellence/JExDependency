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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Persistent invite row for one player invited into an RDA party.
 *
 * @author Codex
 * @since 1.3.0
 * @version 1.3.0
 */
@Entity
@Table(name = "rda_party_invites")
public class RDAPartyInvite extends BaseEntity {

    @Column(name = "invite_uuid", nullable = false, unique = true)
    @Convert(converter = UUIDConverter.class)
    private UUID inviteUuid;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "party_id_fk", nullable = false)
    private RDAParty party;

    @Column(name = "invited_player_uuid", nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID invitedPlayerUuid;

    @Column(name = "inviter_uuid", nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID inviterUuid;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "expires_at", nullable = false)
    private long expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "invite_status", nullable = false, length = 16)
    private RDAPartyInviteStatus status;

    /**
     * Creates a pending party invite.
     *
     * @param party owning party
     * @param invitedPlayerUuid invited player UUID
     * @param inviterUuid inviter UUID
     * @param expiresAt invite expiry timestamp
     */
    public RDAPartyInvite(
        final @NotNull RDAParty party,
        final @NotNull UUID invitedPlayerUuid,
        final @NotNull UUID inviterUuid,
        final long expiresAt
    ) {
        this.inviteUuid = UUID.randomUUID();
        this.party = Objects.requireNonNull(party, "party");
        this.invitedPlayerUuid = Objects.requireNonNull(invitedPlayerUuid, "invitedPlayerUuid");
        this.inviterUuid = Objects.requireNonNull(inviterUuid, "inviterUuid");
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = Math.max(0L, expiresAt);
        this.status = RDAPartyInviteStatus.PENDING;
    }

    /**
     * Constructor reserved for JPA hydration.
     */
    protected RDAPartyInvite() {
    }

    /**
     * Returns the stable invite UUID.
     *
     * @return stable invite UUID
     */
    public @NotNull UUID getInviteUuid() {
        return Objects.requireNonNull(this.inviteUuid, "inviteUuid");
    }

    /**
     * Returns the owning party.
     *
     * @return owning party
     */
    public @NotNull RDAParty getParty() {
        return Objects.requireNonNull(this.party, "party");
    }

    /**
     * Returns the invited player UUID.
     *
     * @return invited player UUID
     */
    public @NotNull UUID getInvitedPlayerUuid() {
        return Objects.requireNonNull(this.invitedPlayerUuid, "invitedPlayerUuid");
    }

    /**
     * Returns the inviter UUID.
     *
     * @return inviter UUID
     */
    public @NotNull UUID getInviterUuid() {
        return Objects.requireNonNull(this.inviterUuid, "inviterUuid");
    }

    /**
     * Returns the creation timestamp in epoch milliseconds.
     *
     * @return creation timestamp
     */
    public long getCreatedAtMillis() {
        return Math.max(0L, this.createdAt);
    }

    /**
     * Returns the expiry timestamp in epoch milliseconds.
     *
     * @return expiry timestamp
     */
    public long getExpiresAt() {
        return Math.max(0L, this.expiresAt);
    }

    /**
     * Returns the current invite status.
     *
     * @return current invite status
     */
    public @NotNull RDAPartyInviteStatus getStatus() {
        return this.status == null ? RDAPartyInviteStatus.PENDING : this.status;
    }

    /**
     * Reports whether the invite is still pending.
     *
     * @return {@code true} when the invite is pending
     */
    public boolean isPending() {
        return this.getStatus() == RDAPartyInviteStatus.PENDING;
    }

    /**
     * Reports whether the pending invite has expired.
     *
     * @param now current epoch milliseconds
     * @return {@code true} when the invite is expired
     */
    public boolean isExpired(final long now) {
        return this.isPending() && now >= this.expiresAt;
    }

    /**
     * Marks the invite as accepted.
     */
    public void accept() {
        this.status = RDAPartyInviteStatus.ACCEPTED;
    }

    /**
     * Marks the invite as declined.
     */
    public void decline() {
        this.status = RDAPartyInviteStatus.DECLINED;
    }

    /**
     * Marks the invite as expired.
     */
    public void expire() {
        this.status = RDAPartyInviteStatus.EXPIRED;
    }

    /**
     * Refreshes the pending invite window and inviter metadata.
     *
     * @param inviterUuid new inviter UUID
     * @param expiresAt replacement expiry timestamp
     */
    public void refresh(final @NotNull UUID inviterUuid, final long expiresAt) {
        this.inviterUuid = Objects.requireNonNull(inviterUuid, "inviterUuid");
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = Math.max(0L, expiresAt);
        this.status = RDAPartyInviteStatus.PENDING;
    }
}
