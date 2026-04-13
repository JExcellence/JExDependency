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

package com.raindropcentral.rda.command.player.ra;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rda.ActivationMode;
import com.raindropcentral.rda.PartyService;
import com.raindropcentral.rda.RDA;
import com.raindropcentral.rda.SkillType;
import com.raindropcentral.rda.database.entity.RDAParty;
import com.raindropcentral.rda.database.entity.RDAPartyInvite;
import com.raindropcentral.rda.database.entity.RDAPartyMember;
import com.raindropcentral.rda.view.RaMainView;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Player command entrypoint for the Raindrop Abilities menus, casting, and party system.
 *
 * @author Codex
 * @since 1.0.0
 * @version 1.3.0
 */
@Command
@SuppressWarnings("unused")
public final class PRA extends PlayerCommand {

    private final RDA rda;

    /**
     * Creates the player command entrypoint for the RDA runtime.
     *
     * @param commandSection mapped command configuration
     * @param rda active RDA runtime
     */
    public PRA(
        final @NotNull PRASection commandSection,
        final @NotNull RDA rda
    ) {
        super(commandSection);
        this.rda = Objects.requireNonNull(rda, "rda");
    }

    /**
     * Routes supported RDA player commands.
     *
     * @param player the player who executed the command
     * @param label the command label used
     * @param args the command arguments
     */
    @Override
    protected void onPlayerInvocation(
        final @NotNull Player player,
        final @NotNull String label,
        final @NotNull String[] args
    ) {
        final EPRAAction action = this.enumParameterOrElse(args, 0, EPRAAction.class, EPRAAction.MAIN);
        switch (action) {
            case MAIN -> this.handleMainAction(player);
            case CAST -> this.handleCastAction(player, args);
            case PARTY -> this.handlePartyAction(player, args);
        }
    }

    /**
     * Provides tab completion for RDA player commands.
     *
     * @param player the player requesting completion
     * @param label the command label used
     * @param args the current command arguments
     * @return matching completions
     */
    @Override
    protected List<String> onPlayerTabCompletion(
        final @NotNull Player player,
        final @NotNull String label,
        final @NotNull String[] args
    ) {
        if (args.length == 1) {
            final ArrayList<String> completions = new ArrayList<>();
            if (this.hasPermission(player, EPRAPermission.MAIN)) {
                completions.add("main");
            }
            if (this.hasPermission(player, EPRAPermission.CAST)) {
                completions.add("cast");
            }
            if (this.hasPermission(player, EPRAPermission.PARTY)) {
                completions.add("party");
            }
            return StringUtil.copyPartialMatches(args[0].toLowerCase(Locale.ROOT), completions, new ArrayList<>());
        }

        if (args.length == 2
            && "cast".equalsIgnoreCase(args[0])
            && this.hasPermission(player, EPRAPermission.CAST)) {
            return StringUtil.copyPartialMatches(
                args[1].toLowerCase(Locale.ROOT),
                this.rda.getEnabledSkills().stream().map(skillType -> skillType.name().toLowerCase(Locale.ROOT)).toList(),
                new ArrayList<>()
            );
        }

        if (args.length == 2
            && "party".equalsIgnoreCase(args[0])
            && this.hasPermission(player, EPRAPermission.PARTY)) {
            return StringUtil.copyPartialMatches(
                args[1].toLowerCase(Locale.ROOT),
                List.of("info", "invite", "accept", "decline", "leave", "kick", "disband"),
                new ArrayList<>()
            );
        }

        if (args.length == 3
            && "party".equalsIgnoreCase(args[0])
            && this.hasPermission(player, EPRAPermission.PARTY)) {
            return switch (args[1].toLowerCase(Locale.ROOT)) {
                case "invite" -> StringUtil.copyPartialMatches(
                    args[2].toLowerCase(Locale.ROOT),
                    Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> !name.equalsIgnoreCase(player.getName()))
                        .toList(),
                    new ArrayList<>()
                );
                case "kick" -> StringUtil.copyPartialMatches(
                    args[2].toLowerCase(Locale.ROOT),
                    this.getKickTargets(player),
                    new ArrayList<>()
                );
                case "accept", "decline" -> StringUtil.copyPartialMatches(
                    args[2].toLowerCase(Locale.ROOT),
                    this.getPendingInviteNames(player),
                    new ArrayList<>()
                );
                default -> List.of();
            };
        }

        return List.of();
    }

    private void handleMainAction(final @NotNull Player player) {
        if (this.hasNoPermission(player, EPRAPermission.MAIN)) {
            return;
        }

        if (this.rda.getViewFrame() == null) {
            return;
        }

        this.rda.getViewFrame().open(RaMainView.class, player, Map.of("plugin", this.rda));
    }

    private void handleCastAction(final @NotNull Player player, final @NotNull String[] args) {
        if (this.hasNoPermission(player, EPRAPermission.CAST) || this.rda.getPlayerBuildService() == null) {
            return;
        }

        final SkillType skillType = this.enumParameterOrElse(args, 1, SkillType.class, null);
        if (skillType == null) {
            return;
        }

        this.rda.getPlayerBuildService().cast(player, skillType, ActivationMode.COMMAND);
    }

    private void handlePartyAction(final @NotNull Player player, final @NotNull String[] args) {
        if (this.hasNoPermission(player, EPRAPermission.PARTY)) {
            return;
        }

        final PartyService partyService = this.rda.getPartyService();
        if (partyService == null) {
            return;
        }

        final EPRAPartyAction partyAction = this.enumParameterOrElse(args, 1, EPRAPartyAction.class, EPRAPartyAction.INFO);
        switch (partyAction) {
            case INFO -> this.sendPartyInfo(player, partyService);
            case INVITE -> this.handlePartyInvite(player, args, partyService);
            case ACCEPT -> this.handlePartyAccept(player, args, partyService);
            case DECLINE -> this.handlePartyDecline(player, args, partyService);
            case LEAVE -> this.handlePartyLeave(player, partyService);
            case KICK -> this.handlePartyKick(player, args, partyService);
            case DISBAND -> this.handlePartyDisband(player, partyService);
        }
    }

    private void handlePartyInvite(
        final @NotNull Player player,
        final @NotNull String[] args,
        final @NotNull PartyService partyService
    ) {
        final OfflinePlayer target = this.offlinePlayerParameter(args, 2, true);
        final PartyService.InviteResult result = partyService.invite(player.getUniqueId(), target.getUniqueId());
        switch (result.status()) {
            case INVITED -> {
                this.sendPartyMessage(player, "ra_party.message.invite_sent", Map.of(
                    "target_name", this.resolvePlayerName(target),
                    "invite_timeout_seconds", partyService.getPartyConfig().inviteTimeoutSeconds()
                ));
                if (target.isOnline()) {
                    final Player onlineTarget = target.getPlayer();
                    if (onlineTarget != null) {
                        this.sendPartyMessage(onlineTarget, "ra_party.message.invite_notify", Map.of(
                            "leader_name", this.resolvePlayerName(player.getUniqueId()),
                            "invite_timeout_seconds", partyService.getPartyConfig().inviteTimeoutSeconds()
                        ));
                    }
                }
            }
            case SELF_TARGET -> this.sendPartyMessage(player, "ra_party.message.invite_self", Map.of());
            case TARGET_ALREADY_IN_PARTY -> this.sendPartyMessage(player, "ra_party.message.invite_target_in_party", Map.of(
                "target_name", this.resolvePlayerName(target)
            ));
            case NOT_LEADER -> this.sendPartyMessage(player, "ra_party.message.invite_not_leader", Map.of());
            case PARTY_FULL -> this.sendPartyMessage(player, "ra_party.message.invite_full", Map.of());
        }
    }

    private void handlePartyAccept(
        final @NotNull Player player,
        final @NotNull String[] args,
        final @NotNull PartyService partyService
    ) {
        final OfflinePlayer leader = this.offlinePlayerParameter(args, 2, true);
        final PartyService.AcceptResult result = partyService.accept(player.getUniqueId(), leader.getUniqueId());
        switch (result.status()) {
            case ACCEPTED -> {
                final String leaderName = result.party() == null
                    ? this.resolvePlayerName(leader)
                    : this.resolvePlayerName(result.party().getLeaderProfile().getPlayerUuid());
                this.sendPartyMessage(player, "ra_party.message.accept_success", Map.of(
                    "leader_name", leaderName
                ));
                if (result.invite() != null) {
                    this.sendOnlineNotification(
                        result.invite().getInviterUuid(),
                        "ra_party.message.accept_notify",
                        Map.of("target_name", this.resolvePlayerName(player.getUniqueId()))
                    );
                }
            }
            case NO_INVITE -> this.sendPartyMessage(player, "ra_party.message.accept_no_invite", Map.of(
                "leader_name", this.resolvePlayerName(leader)
            ));
            case ALREADY_IN_PARTY -> this.sendPartyMessage(player, "ra_party.message.accept_already_in_party", Map.of());
            case PARTY_FULL -> this.sendPartyMessage(player, "ra_party.message.accept_full", Map.of());
        }
    }

    private void handlePartyDecline(
        final @NotNull Player player,
        final @NotNull String[] args,
        final @NotNull PartyService partyService
    ) {
        final OfflinePlayer leader = this.offlinePlayerParameter(args, 2, true);
        final PartyService.DeclineResult result = partyService.decline(player.getUniqueId(), leader.getUniqueId());
        switch (result.status()) {
            case DECLINED -> {
                this.sendPartyMessage(player, "ra_party.message.decline_success", Map.of(
                    "leader_name", this.resolvePlayerName(leader)
                ));
                if (result.invite() != null) {
                    this.sendOnlineNotification(
                        result.invite().getInviterUuid(),
                        "ra_party.message.decline_notify",
                        Map.of("target_name", this.resolvePlayerName(player.getUniqueId()))
                    );
                }
            }
            case NO_INVITE -> this.sendPartyMessage(player, "ra_party.message.decline_no_invite", Map.of(
                "leader_name", this.resolvePlayerName(leader)
            ));
        }
    }

    private void handlePartyLeave(
        final @NotNull Player player,
        final @NotNull PartyService partyService
    ) {
        final PartyService.LeaveResult result = partyService.leave(player.getUniqueId());
        switch (result.status()) {
            case LEFT -> {
                this.sendPartyMessage(player, "ra_party.message.leave_success", Map.of());
                if (result.newLeaderUuid() != null) {
                    this.sendPartyMessage(player, "ra_party.message.leader_transferred", Map.of(
                        "leader_name", this.resolvePlayerName(result.newLeaderUuid())
                    ));
                }
            }
            case DISBANDED -> this.sendPartyMessage(player, "ra_party.message.leave_disbanded", Map.of());
            case NOT_IN_PARTY -> this.sendPartyMessage(player, "ra_party.message.not_in_party", Map.of());
        }
    }

    private void handlePartyKick(
        final @NotNull Player player,
        final @NotNull String[] args,
        final @NotNull PartyService partyService
    ) {
        final OfflinePlayer target = this.offlinePlayerParameter(args, 2, true);
        final PartyService.KickResult result = partyService.kick(player.getUniqueId(), target.getUniqueId());
        switch (result.status()) {
            case KICKED -> {
                this.sendPartyMessage(player, "ra_party.message.kick_success", Map.of(
                    "target_name", this.resolvePlayerName(target)
                ));
                if (target.isOnline()) {
                    final Player onlineTarget = target.getPlayer();
                    if (onlineTarget != null) {
                        this.sendPartyMessage(onlineTarget, "ra_party.message.kick_notify", Map.of(
                            "leader_name", this.resolvePlayerName(player.getUniqueId())
                        ));
                    }
                }
            }
            case NOT_IN_PARTY -> this.sendPartyMessage(player, "ra_party.message.not_in_party", Map.of());
            case NOT_LEADER -> this.sendPartyMessage(player, "ra_party.message.kick_not_leader", Map.of());
            case TARGET_NOT_IN_PARTY -> this.sendPartyMessage(player, "ra_party.message.kick_target_missing", Map.of(
                "target_name", this.resolvePlayerName(target)
            ));
            case CANNOT_KICK_SELF -> this.sendPartyMessage(player, "ra_party.message.kick_self", Map.of());
        }
    }

    private void handlePartyDisband(
        final @NotNull Player player,
        final @NotNull PartyService partyService
    ) {
        final PartyService.DisbandResult result = partyService.disband(player.getUniqueId());
        switch (result.status()) {
            case DISBANDED -> this.sendPartyMessage(player, "ra_party.message.disband_success", Map.of());
            case NOT_IN_PARTY -> this.sendPartyMessage(player, "ra_party.message.not_in_party", Map.of());
            case NOT_LEADER -> this.sendPartyMessage(player, "ra_party.message.disband_not_leader", Map.of());
        }
    }

    private void sendPartyInfo(
        final @NotNull Player player,
        final @NotNull PartyService partyService
    ) {
        final RDAParty party = partyService.getPartyForPlayer(player.getUniqueId());
        if (party == null) {
            this.sendPartyMessage(player, "ra_party.message.not_in_party", Map.of());
            return;
        }

        final List<RDAPartyMember> members = partyService.getMembers(party);
        final String memberNames = String.join(
            ", ",
            members.stream()
                .map(member -> this.resolvePlayerName(member.getPlayerProfile().getPlayerUuid()))
                .toList()
        );
        final Map<String, Object> placeholders = Map.of(
            "leader_name", this.resolvePlayerName(party.getLeaderProfile().getPlayerUuid()),
            "member_count", members.size(),
            "max_members", partyService.getPartyConfig().maxMembers(),
            "pending_invites", partyService.getPendingInviteCount(party),
            "member_names", memberNames
        );
        final String headerKey = partyService.getPartyConfig().hasUnlimitedMemberCap()
            ? "ra_party.message.info_header_unlimited"
            : "ra_party.message.info_header";
        this.sendPartyMessage(player, headerKey, placeholders);
        this.sendPartyMessage(player, "ra_party.message.info_members", placeholders);
    }

    private void sendPartyMessage(
        final @NotNull Player player,
        final @NotNull String key,
        final @NotNull Map<String, Object> placeholders
    ) {
        new I18n.Builder(key, player)
            .includePrefix()
            .withPlaceholders(placeholders)
            .build()
            .sendMessage();
    }

    private void sendOnlineNotification(
        final @NotNull UUID playerUuid,
        final @NotNull String key,
        final @NotNull Map<String, Object> placeholders
    ) {
        final Player onlinePlayer = Bukkit.getPlayer(Objects.requireNonNull(playerUuid, "playerUuid"));
        if (onlinePlayer != null) {
            this.sendPartyMessage(onlinePlayer, key, placeholders);
        }
    }

    private @NotNull List<String> getKickTargets(final @NotNull Player player) {
        final PartyService partyService = this.rda.getPartyService();
        if (partyService == null) {
            return List.of();
        }

        final RDAParty party = partyService.getPartyForPlayer(player.getUniqueId());
        if (party == null) {
            return List.of();
        }

        return partyService.getMembers(party).stream()
            .map(RDAPartyMember::getPlayerProfile)
            .map(profile -> profile.getPlayerUuid())
            .filter(playerUuid -> !playerUuid.equals(player.getUniqueId()))
            .map(this::resolvePlayerName)
            .distinct()
            .toList();
    }

    private @NotNull List<String> getPendingInviteNames(final @NotNull Player player) {
        final PartyService partyService = this.rda.getPartyService();
        if (partyService == null) {
            return List.of();
        }

        final Set<String> names = new LinkedHashSet<>();
        for (final RDAPartyInvite invite : partyService.getPendingInvites(player.getUniqueId())) {
            names.add(this.resolvePlayerName(invite.getInviterUuid()));
        }
        return List.copyOf(names);
    }

    private @NotNull String resolvePlayerName(final @NotNull OfflinePlayer player) {
        Objects.requireNonNull(player, "player");
        return this.resolvePlayerName(player.getUniqueId(), player.getName());
    }

    private @NotNull String resolvePlayerName(final @NotNull UUID playerUuid) {
        return this.resolvePlayerName(playerUuid, Bukkit.getOfflinePlayer(playerUuid).getName());
    }

    private @NotNull String resolvePlayerName(
        final @NotNull UUID playerUuid,
        final String configuredName
    ) {
        if (configuredName != null && !configuredName.isBlank()) {
            return configuredName;
        }
        return playerUuid.toString().substring(0, 8);
    }
}
