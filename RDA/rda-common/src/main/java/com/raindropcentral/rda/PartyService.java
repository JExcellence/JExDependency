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

package com.raindropcentral.rda;

import com.raindropcentral.rda.database.entity.RDAParty;
import com.raindropcentral.rda.database.entity.RDAPartyInvite;
import com.raindropcentral.rda.database.entity.RDAPartyMember;
import com.raindropcentral.rda.database.entity.RDAPlayer;
import com.raindropcentral.rda.database.repository.RRDAParty;
import com.raindropcentral.rda.database.repository.RRDAPartyInvite;
import com.raindropcentral.rda.database.repository.RRDAPartyMember;
import com.raindropcentral.rda.database.repository.RRDAPlayer;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Owns persistent RDA party membership, invitations, leadership transfer, and XP sharing.
 *
 * <p>The service intentionally keeps command messaging outside the core operations so party state
 * can be unit tested without depending on the translation runtime.</p>
 *
 * @author Codex
 * @since 1.3.0
 * @version 1.3.0
 */
public final class PartyService {

    private static final Comparator<RDAPartyMember> MEMBER_ORDER = Comparator
        .comparingLong(RDAPartyMember::getJoinedAt)
        .thenComparing(member -> member.getPlayerProfile().getPlayerUuid());

    private final JavaPlugin plugin;
    private final Logger logger;
    private final PartyConfig partyConfig;
    private final RRDAPlayer playerRepository;
    private final RRDAParty partyRepository;
    private final RRDAPartyMember partyMemberRepository;
    private final RRDAPartyInvite partyInviteRepository;

    /**
     * Creates a new persistent party service.
     *
     * @param plugin owning plugin
     * @param logger runtime logger
     * @param partyConfig loaded party configuration
     * @param playerRepository player repository
     * @param partyRepository party repository
     * @param partyMemberRepository party-member repository
     * @param partyInviteRepository party-invite repository
     */
    public PartyService(
        final @NotNull JavaPlugin plugin,
        final @NotNull Logger logger,
        final @NotNull PartyConfig partyConfig,
        final @NotNull RRDAPlayer playerRepository,
        final @NotNull RRDAParty partyRepository,
        final @NotNull RRDAPartyMember partyMemberRepository,
        final @NotNull RRDAPartyInvite partyInviteRepository
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.partyConfig = Objects.requireNonNull(partyConfig, "partyConfig");
        this.playerRepository = Objects.requireNonNull(playerRepository, "playerRepository");
        this.partyRepository = Objects.requireNonNull(partyRepository, "partyRepository");
        this.partyMemberRepository = Objects.requireNonNull(partyMemberRepository, "partyMemberRepository");
        this.partyInviteRepository = Objects.requireNonNull(partyInviteRepository, "partyInviteRepository");
    }

    /**
     * Expires stale invites after service startup.
     */
    public synchronized void initialize() {
        this.expireStaleInvites();
    }

    /**
     * Returns the loaded party configuration.
     *
     * @return loaded party configuration
     */
    public @NotNull PartyConfig getPartyConfig() {
        return this.partyConfig;
    }

    /**
     * Returns the party currently owned by the supplied player UUID.
     *
     * @param playerUuid player UUID
     * @return owning party, or {@code null} when the player is not in a party
     */
    public synchronized @Nullable RDAParty getPartyForPlayer(final @NotNull UUID playerUuid) {
        final RDAPartyMember membership = this.partyMemberRepository.findByPlayer(Objects.requireNonNull(playerUuid, "playerUuid"));
        return membership == null ? null : membership.getParty();
    }

    /**
     * Returns every member belonging to the supplied party in deterministic join order.
     *
     * @param party owning party
     * @return ordered party members
     */
    public synchronized @NotNull List<RDAPartyMember> getMembers(final @NotNull RDAParty party) {
        return this.partyMemberRepository.findAllByParty(Objects.requireNonNull(party, "party").getPartyUuid()).stream()
            .sorted(MEMBER_ORDER)
            .toList();
    }

    /**
     * Returns the current count of pending invites for the supplied party.
     *
     * @param party owning party
     * @return pending invite count
     */
    public synchronized int getPendingInviteCount(final @NotNull RDAParty party) {
        return this.getPendingInvitesForParty(Objects.requireNonNull(party, "party").getPartyUuid()).size();
    }

    /**
     * Returns every pending invite addressed to the supplied player.
     *
     * @param playerUuid invited player UUID
     * @return pending invites addressed to the player
     */
    public synchronized @NotNull List<RDAPartyInvite> getPendingInvites(final @NotNull UUID playerUuid) {
        this.expireStaleInvites();
        return this.partyInviteRepository.findPendingByInvitedPlayer(Objects.requireNonNull(playerUuid, "playerUuid"));
    }

    /**
     * Returns every pending invite belonging to the supplied party.
     *
     * @param partyUuid party UUID
     * @return pending invites belonging to the party
     */
    public synchronized @NotNull List<RDAPartyInvite> getPendingInvitesForParty(final @NotNull UUID partyUuid) {
        this.expireStaleInvites();
        return this.partyInviteRepository.findAllByParty(Objects.requireNonNull(partyUuid, "partyUuid")).stream()
            .filter(RDAPartyInvite::isPending)
            .toList();
    }

    /**
     * Sends or refreshes a party invite for the supplied target player.
     *
     * @param inviterUuid inviter UUID
     * @param targetPlayerUuid target player UUID
     * @return structured invite result
     */
    public synchronized @NotNull InviteResult invite(
        final @NotNull UUID inviterUuid,
        final @NotNull UUID targetPlayerUuid
    ) {
        Objects.requireNonNull(inviterUuid, "inviterUuid");
        Objects.requireNonNull(targetPlayerUuid, "targetPlayerUuid");
        this.expireStaleInvites();

        if (inviterUuid.equals(targetPlayerUuid)) {
            return new InviteResult(InviteStatus.SELF_TARGET, null, null, false);
        }

        if (this.getPartyForPlayer(targetPlayerUuid) != null) {
            return new InviteResult(InviteStatus.TARGET_ALREADY_IN_PARTY, null, null, false);
        }

        final RDAPartyMember inviterMembership = this.partyMemberRepository.findByPlayer(inviterUuid);
        final RDAParty party;
        if (inviterMembership == null) {
            if (!this.partyConfig.canAcceptAnotherMember(1)) {
                return new InviteResult(InviteStatus.PARTY_FULL, null, null, false);
            }

            final RDAPlayer leaderProfile = this.getOrCreatePlayerProfile(inviterUuid);
            party = this.partyRepository.create(new RDAParty(leaderProfile));
            this.partyMemberRepository.create(new RDAPartyMember(party, leaderProfile));
        } else {
            party = inviterMembership.getParty();
            if (!this.isLeader(party, inviterUuid)) {
                return new InviteResult(InviteStatus.NOT_LEADER, party, null, false);
            }
        }

        final RDAPartyInvite existingInvite = this.partyInviteRepository.findPendingByPartyAndTarget(
            party.getPartyUuid(),
            targetPlayerUuid
        );
        if (existingInvite != null) {
            existingInvite.refresh(inviterUuid, System.currentTimeMillis() + this.partyConfig.inviteTimeoutMillis());
            this.partyInviteRepository.update(existingInvite);
            return new InviteResult(InviteStatus.INVITED, party, existingInvite, inviterMembership == null);
        }

        if (!this.partyConfig.canAcceptAnotherMember(this.getMembers(party).size())) {
            return new InviteResult(InviteStatus.PARTY_FULL, party, null, inviterMembership == null);
        }

        final RDAPartyInvite invite = this.partyInviteRepository.create(new RDAPartyInvite(
            party,
            targetPlayerUuid,
            inviterUuid,
            System.currentTimeMillis() + this.partyConfig.inviteTimeoutMillis()
        ));
        return new InviteResult(InviteStatus.INVITED, party, invite, inviterMembership == null);
    }

    /**
     * Accepts a pending party invite identified by the current leader or original inviter UUID.
     *
     * @param invitedPlayerUuid invited player UUID
     * @param leaderOrInviterUuid leader or inviter UUID used by the player command
     * @return structured accept result
     */
    public synchronized @NotNull AcceptResult accept(
        final @NotNull UUID invitedPlayerUuid,
        final @NotNull UUID leaderOrInviterUuid
    ) {
        Objects.requireNonNull(invitedPlayerUuid, "invitedPlayerUuid");
        Objects.requireNonNull(leaderOrInviterUuid, "leaderOrInviterUuid");
        this.expireStaleInvites();

        if (this.getPartyForPlayer(invitedPlayerUuid) != null) {
            return new AcceptResult(AcceptStatus.ALREADY_IN_PARTY, null, null);
        }

        final RDAPartyInvite invite = this.resolvePendingInvite(invitedPlayerUuid, leaderOrInviterUuid);
        if (invite == null) {
            return new AcceptResult(AcceptStatus.NO_INVITE, null, null);
        }

        final RDAParty party = invite.getParty();
        if (!this.partyConfig.canAcceptAnotherMember(this.getMembers(party).size())) {
            return new AcceptResult(AcceptStatus.PARTY_FULL, party, invite);
        }

        this.partyMemberRepository.create(new RDAPartyMember(party, this.getOrCreatePlayerProfile(invitedPlayerUuid)));
        invite.accept();
        this.partyInviteRepository.update(invite);
        this.expireOtherPendingInvites(invitedPlayerUuid, invite.getInviteUuid());
        return new AcceptResult(AcceptStatus.ACCEPTED, party, invite);
    }

    /**
     * Declines a pending party invite identified by the current leader or original inviter UUID.
     *
     * @param invitedPlayerUuid invited player UUID
     * @param leaderOrInviterUuid leader or inviter UUID used by the player command
     * @return structured decline result
     */
    public synchronized @NotNull DeclineResult decline(
        final @NotNull UUID invitedPlayerUuid,
        final @NotNull UUID leaderOrInviterUuid
    ) {
        Objects.requireNonNull(invitedPlayerUuid, "invitedPlayerUuid");
        Objects.requireNonNull(leaderOrInviterUuid, "leaderOrInviterUuid");
        this.expireStaleInvites();

        final RDAPartyInvite invite = this.resolvePendingInvite(invitedPlayerUuid, leaderOrInviterUuid);
        if (invite == null) {
            return new DeclineResult(DeclineStatus.NO_INVITE, null);
        }

        invite.decline();
        this.partyInviteRepository.update(invite);
        return new DeclineResult(DeclineStatus.DECLINED, invite);
    }

    /**
     * Removes the supplied player from their current party.
     *
     * @param playerUuid leaving player UUID
     * @return structured leave result
     */
    public synchronized @NotNull LeaveResult leave(final @NotNull UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        final RDAPartyMember membership = this.partyMemberRepository.findByPlayer(playerUuid);
        if (membership == null) {
            return new LeaveResult(LeaveStatus.NOT_IN_PARTY, null, null);
        }

        final RDAParty party = membership.getParty();
        final List<RDAPartyMember> remainingMembers = this.getMembers(party).stream()
            .filter(member -> !member.getPlayerProfile().getPlayerUuid().equals(playerUuid))
            .toList();

        this.partyMemberRepository.delete(membership.getId());

        if (remainingMembers.isEmpty()) {
            this.deleteAllInvitesForParty(party.getPartyUuid());
            this.partyRepository.delete(party.getId());
            return new LeaveResult(LeaveStatus.DISBANDED, party, null);
        }

        if (this.isLeader(party, playerUuid)) {
            final RDAPartyMember nextLeader = this.selectNextLeader(remainingMembers);
            party.setLeaderProfile(nextLeader.getPlayerProfile());
            this.partyRepository.update(party);
            return new LeaveResult(LeaveStatus.LEFT, party, nextLeader.getPlayerProfile().getPlayerUuid());
        }

        return new LeaveResult(LeaveStatus.LEFT, party, null);
    }

    /**
     * Kicks one member from the caller's current party.
     *
     * @param leaderUuid leader UUID
     * @param targetPlayerUuid target player UUID
     * @return structured kick result
     */
    public synchronized @NotNull KickResult kick(
        final @NotNull UUID leaderUuid,
        final @NotNull UUID targetPlayerUuid
    ) {
        Objects.requireNonNull(leaderUuid, "leaderUuid");
        Objects.requireNonNull(targetPlayerUuid, "targetPlayerUuid");

        if (leaderUuid.equals(targetPlayerUuid)) {
            return new KickResult(KickStatus.CANNOT_KICK_SELF, null, null);
        }

        final RDAPartyMember leaderMembership = this.partyMemberRepository.findByPlayer(leaderUuid);
        if (leaderMembership == null) {
            return new KickResult(KickStatus.NOT_IN_PARTY, null, null);
        }

        final RDAParty party = leaderMembership.getParty();
        if (!this.isLeader(party, leaderUuid)) {
            return new KickResult(KickStatus.NOT_LEADER, party, null);
        }

        final RDAPartyMember targetMembership = this.partyMemberRepository.findByPlayer(targetPlayerUuid);
        if (targetMembership == null || !targetMembership.getParty().getPartyUuid().equals(party.getPartyUuid())) {
            return new KickResult(KickStatus.TARGET_NOT_IN_PARTY, party, null);
        }

        this.partyMemberRepository.delete(targetMembership.getId());
        return new KickResult(KickStatus.KICKED, party, targetPlayerUuid);
    }

    /**
     * Disbands the caller's current party when they are the leader.
     *
     * @param leaderUuid leader UUID
     * @return structured disband result
     */
    public synchronized @NotNull DisbandResult disband(final @NotNull UUID leaderUuid) {
        Objects.requireNonNull(leaderUuid, "leaderUuid");
        final RDAPartyMember membership = this.partyMemberRepository.findByPlayer(leaderUuid);
        if (membership == null) {
            return new DisbandResult(DisbandStatus.NOT_IN_PARTY, null);
        }

        final RDAParty party = membership.getParty();
        if (!this.isLeader(party, leaderUuid)) {
            return new DisbandResult(DisbandStatus.NOT_LEADER, party);
        }

        this.deleteAllMembersForParty(party.getPartyUuid());
        this.deleteAllInvitesForParty(party.getPartyUuid());
        this.partyRepository.delete(party.getId());
        return new DisbandResult(DisbandStatus.DISBANDED, party);
    }

    /**
     * Transfers leadership when the current leader disconnects but remains in the party.
     *
     * @param playerUuid disconnecting player UUID
     */
    public synchronized void handlePlayerQuit(final @NotNull UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid");
        final RDAPartyMember membership = this.partyMemberRepository.findByPlayer(playerUuid);
        if (membership == null) {
            return;
        }

        final RDAParty party = membership.getParty();
        if (!this.isLeader(party, playerUuid)) {
            return;
        }

        final List<RDAPartyMember> remainingMembers = this.getMembers(party).stream()
            .filter(member -> !member.getPlayerProfile().getPlayerUuid().equals(playerUuid))
            .toList();
        if (remainingMembers.isEmpty()) {
            return;
        }

        final RDAPartyMember nextLeader = this.selectNextLeader(remainingMembers);
        if (nextLeader.getPlayerProfile().getPlayerUuid().equals(playerUuid)) {
            return;
        }

        party.setLeaderProfile(nextLeader.getPlayerProfile());
        this.partyRepository.update(party);
    }

    /**
     * Expires every stale pending invite.
     */
    public synchronized void expireStaleInvites() {
        final long now = System.currentTimeMillis();
        for (final RDAPartyInvite invite : this.partyInviteRepository.findAll()) {
            if (!invite.isExpired(now)) {
                continue;
            }

            invite.expire();
            this.partyInviteRepository.update(invite);
            this.logger.fine("Expired RDA party invite " + invite.getInviteUuid());
        }
    }

    /**
     * Applies party XP sharing for one pre-prestige skill XP award.
     *
     * @param progressionService skill progression service owning the XP award
     * @param earner player who triggered the XP source
     * @param baseXp pre-prestige XP value for the event
     * @param sourceLabel display label for the XP source
     */
    public synchronized void distributeSkillXp(
        final @NotNull SkillProgressionService progressionService,
        final @NotNull Player earner,
        final long baseXp,
        final @NotNull String sourceLabel
    ) {
        Objects.requireNonNull(progressionService, "progressionService");
        Objects.requireNonNull(earner, "earner");
        Objects.requireNonNull(sourceLabel, "sourceLabel");

        if (baseXp <= 0L) {
            return;
        }

        final RDAParty party = this.getPartyForPlayer(earner.getUniqueId());
        if (party == null) {
            progressionService.awardDistributedBaseXp(earner, baseXp, sourceLabel, null);
            return;
        }

        final List<OnlinePartyMember> eligibleRecipients = new ArrayList<>();
        final double rangeSquared = this.partyConfig.xpShareSettings().rangeSquared();
        for (final RDAPartyMember member : this.getMembers(party)) {
            final UUID memberUuid = member.getPlayerProfile().getPlayerUuid();
            if (memberUuid.equals(earner.getUniqueId())) {
                continue;
            }

            final Player onlineMember = this.plugin.getServer().getPlayer(memberUuid);
            if (!this.isEligibleShareRecipient(earner, onlineMember, rangeSquared)) {
                continue;
            }

            eligibleRecipients.add(new OnlinePartyMember(member, onlineMember));
        }

        if (eligibleRecipients.isEmpty()) {
            progressionService.awardDistributedBaseXp(earner, baseXp, sourceLabel, null);
            return;
        }

        final long selfBaseXp = (long) Math.floor(baseXp * this.partyConfig.xpShareSettings().selfShare());
        final long othersPool = (long) Math.floor(baseXp * this.partyConfig.xpShareSettings().othersTotalShare());
        progressionService.awardDistributedBaseXp(earner, selfBaseXp, sourceLabel, null);

        if (othersPool <= 0L) {
            return;
        }

        final long sharePerMember = othersPool / eligibleRecipients.size();
        final long remainder = othersPool % eligibleRecipients.size();
        for (int index = 0; index < eligibleRecipients.size(); index++) {
            final long memberBaseXp = sharePerMember + (index < remainder ? 1L : 0L);
            if (memberBaseXp <= 0L) {
                continue;
            }

            progressionService.awardDistributedBaseXp(
                eligibleRecipients.get(index).player(),
                memberBaseXp,
                sourceLabel,
                earner
            );
        }
    }

    private boolean isLeader(final @NotNull RDAParty party, final @NotNull UUID playerUuid) {
        return Objects.requireNonNull(party, "party").getLeaderProfile().getPlayerUuid().equals(
            Objects.requireNonNull(playerUuid, "playerUuid")
        );
    }

    private boolean isEligibleShareRecipient(
        final @NotNull Player earner,
        final @Nullable Player recipient,
        final double rangeSquared
    ) {
        if (recipient == null) {
            return false;
        }
        if (recipient.getGameMode() == GameMode.CREATIVE || recipient.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        if (recipient.getWorld() != earner.getWorld()) {
            return false;
        }
        return recipient.getLocation().distanceSquared(earner.getLocation()) <= rangeSquared;
    }

    private @Nullable RDAPartyInvite resolvePendingInvite(
        final @NotNull UUID invitedPlayerUuid,
        final @NotNull UUID leaderOrInviterUuid
    ) {
        return this.getPendingInvites(invitedPlayerUuid).stream()
            .filter(invite -> invite.getInviterUuid().equals(leaderOrInviterUuid)
                || invite.getParty().getLeaderProfile().getPlayerUuid().equals(leaderOrInviterUuid))
            .sorted(Comparator.comparingLong(RDAPartyInvite::getCreatedAtMillis))
            .findFirst()
            .orElse(null);
    }

    private void expireOtherPendingInvites(final @NotNull UUID playerUuid, final @NotNull UUID acceptedInviteUuid) {
        for (final RDAPartyInvite invite : this.getPendingInvites(playerUuid)) {
            if (invite.getInviteUuid().equals(acceptedInviteUuid)) {
                continue;
            }

            invite.expire();
            this.partyInviteRepository.update(invite);
        }
    }

    private void deleteAllMembersForParty(final @NotNull UUID partyUuid) {
        for (final RDAPartyMember member : this.partyMemberRepository.findAllByParty(partyUuid)) {
            this.partyMemberRepository.delete(member.getId());
        }
    }

    private void deleteAllInvitesForParty(final @NotNull UUID partyUuid) {
        for (final RDAPartyInvite invite : this.partyInviteRepository.findAllByParty(partyUuid)) {
            this.partyInviteRepository.delete(invite.getId());
        }
    }

    private @NotNull RDAPartyMember selectNextLeader(final @NotNull List<RDAPartyMember> remainingMembers) {
        return remainingMembers.stream()
            .min(MEMBER_ORDER)
            .orElseThrow(() -> new IllegalStateException("Cannot select a new party leader without remaining members"));
    }

    private @NotNull RDAPlayer getOrCreatePlayerProfile(final @NotNull UUID playerUuid) {
        return this.playerRepository.findOrCreateByPlayer(Objects.requireNonNull(playerUuid, "playerUuid"));
    }

    private record OnlinePartyMember(
        @NotNull RDAPartyMember member,
        @NotNull Player player
    ) {
        private OnlinePartyMember {
            Objects.requireNonNull(member, "member");
            Objects.requireNonNull(player, "player");
        }
    }

    /**
     * Enumerates party invite operation outcomes.
     */
    public enum InviteStatus {
        INVITED,
        SELF_TARGET,
        TARGET_ALREADY_IN_PARTY,
        NOT_LEADER,
        PARTY_FULL
    }

    /**
     * Structured result returned by {@link #invite(UUID, UUID)}.
     *
     * @param status invite outcome
     * @param party resolved party involved in the invite, when available
     * @param invite resolved invite row, when available
     * @param createdParty whether the invite call created a brand new party for the inviter
     */
    public record InviteResult(
        @NotNull InviteStatus status,
        @Nullable RDAParty party,
        @Nullable RDAPartyInvite invite,
        boolean createdParty
    ) {
        /**
         * Validates the immutable invite result.
         */
        public InviteResult {
            Objects.requireNonNull(status, "status");
        }
    }

    /**
     * Enumerates party invite acceptance outcomes.
     */
    public enum AcceptStatus {
        ACCEPTED,
        NO_INVITE,
        ALREADY_IN_PARTY,
        PARTY_FULL
    }

    /**
     * Structured result returned by {@link #accept(UUID, UUID)}.
     *
     * @param status accept outcome
     * @param party resolved party involved in the acceptance, when available
     * @param invite resolved invite row, when available
     */
    public record AcceptResult(
        @NotNull AcceptStatus status,
        @Nullable RDAParty party,
        @Nullable RDAPartyInvite invite
    ) {
        /**
         * Validates the immutable accept result.
         */
        public AcceptResult {
            Objects.requireNonNull(status, "status");
        }
    }

    /**
     * Enumerates party invite decline outcomes.
     */
    public enum DeclineStatus {
        DECLINED,
        NO_INVITE
    }

    /**
     * Structured result returned by {@link #decline(UUID, UUID)}.
     *
     * @param status decline outcome
     * @param invite resolved invite row, when available
     */
    public record DeclineResult(
        @NotNull DeclineStatus status,
        @Nullable RDAPartyInvite invite
    ) {
        /**
         * Validates the immutable decline result.
         */
        public DeclineResult {
            Objects.requireNonNull(status, "status");
        }
    }

    /**
     * Enumerates party leave outcomes.
     */
    public enum LeaveStatus {
        LEFT,
        DISBANDED,
        NOT_IN_PARTY
    }

    /**
     * Structured result returned by {@link #leave(UUID)}.
     *
     * @param status leave outcome
     * @param party resolved party involved in the leave operation, when available
     * @param newLeaderUuid replacement leader UUID when leadership changed
     */
    public record LeaveResult(
        @NotNull LeaveStatus status,
        @Nullable RDAParty party,
        @Nullable UUID newLeaderUuid
    ) {
        /**
         * Validates the immutable leave result.
         */
        public LeaveResult {
            Objects.requireNonNull(status, "status");
        }
    }

    /**
     * Enumerates party kick outcomes.
     */
    public enum KickStatus {
        KICKED,
        NOT_IN_PARTY,
        NOT_LEADER,
        TARGET_NOT_IN_PARTY,
        CANNOT_KICK_SELF
    }

    /**
     * Structured result returned by {@link #kick(UUID, UUID)}.
     *
     * @param status kick outcome
     * @param party resolved party involved in the kick operation, when available
     * @param targetPlayerUuid kicked player UUID when the kick succeeded
     */
    public record KickResult(
        @NotNull KickStatus status,
        @Nullable RDAParty party,
        @Nullable UUID targetPlayerUuid
    ) {
        /**
         * Validates the immutable kick result.
         */
        public KickResult {
            Objects.requireNonNull(status, "status");
        }
    }

    /**
     * Enumerates party disband outcomes.
     */
    public enum DisbandStatus {
        DISBANDED,
        NOT_IN_PARTY,
        NOT_LEADER
    }

    /**
     * Structured result returned by {@link #disband(UUID)}.
     *
     * @param status disband outcome
     * @param party resolved party involved in the disband operation, when available
     */
    public record DisbandResult(
        @NotNull DisbandStatus status,
        @Nullable RDAParty party
    ) {
        /**
         * Validates the immutable disband result.
         */
        public DisbandResult {
            Objects.requireNonNull(status, "status");
        }
    }
}
