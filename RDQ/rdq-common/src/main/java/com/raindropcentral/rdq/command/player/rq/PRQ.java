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

package com.raindropcentral.rdq.command.player.rq;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.view.admin.AdminOverviewView;
import com.raindropcentral.rdq.view.bounty.BountyMainView;
import com.raindropcentral.rdq.view.main.MainOverviewView;
import com.raindropcentral.rdq.view.perks.PerkOverviewView;
import com.raindropcentral.rdq.view.quest.QuestCategoryView;
import com.raindropcentral.rdq.view.ranks.RankMainView;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents the PRQ API type.
 */
@Command
@SuppressWarnings("unused")
public class PRQ extends PlayerCommand {

    private static final String PERKS_SCOREBOARD_TYPE = "perks";
    
    /**
     * The main plugin instance.
     */
    private final RDQ rdq;
    
    /**
     * Constructs a new {@code PAdmin} command handler.
     *
     * @param commandSection the command section configuration
     * @param rdq            the main plugin instance
     */
    public PRQ(
        final @NotNull PRQSection commandSection,
        final @NotNull RDQ rdq
    ) {
        
        super(commandSection);
        this.rdq = rdq;
    }
    
    /**
     * Handles the command execution when a player invokes it.
 *
 * <p>Checks for the required permission and opens the admin overview view for the player.
     *
     * @param player the player who executed the command
     * @param label  the command label used
     * @param args   the command arguments
     */
    @Override
    protected void onPlayerInvocation(
        final @NotNull Player player,
        final @NotNull String label,
        final @NotNull String[] args
    ) {

        EPRQAction action = enumParameterOrElse(
            args,
            0,
            EPRQAction.class,
            EPRQAction.HELP
        );
        
        switch (action) {
            case ADMIN -> {
                if (
                    this.hasNoPermission(
                        player,
                        EPRQPermission.ADMIN
                    )
                ) {
                    return;
                }
                
                if (
                    this.rdq.getLuckPermsService() == null
                ) {
                    new I18n.Builder("rq.no_luckperms_installed", player).includePrefix().build().sendMessage();
                    return;
                }
                
                this.rdq.getViewFrame().open(
                    AdminOverviewView.class,
                    player,
                    Map.of(
                        "plugin",
                        this.rdq,
                        "pluginName",
                        args.length >= 1 ?
                        stringParameter(
                            args,
                            0
                        ) :
                        ""
                    )
                );
            }
            case BOUNTY -> {
                if (this.hasNoPermission(
                    player,
                    EPRQPermission.BOUNTY
                )) {
                    return;
                }
                this.rdq.getViewFrame().open(
                    BountyMainView.class,
                    player,
                    Map.of(
                        "plugin",
                        this.rdq
                    )
                );
            }
            case MAIN -> {
                if (this.hasNoPermission(
                    player,
                    EPRQPermission.MAIN
                )) {
                    return;
                }
                this.rdq.getViewFrame().open(
                    MainOverviewView.class,
                    player,
                    Map.of(
                        "plugin",
                        this.rdq
                    )
                );
            }
            case QUESTS -> {
                if (this.hasNoPermission(
                    player,
                    EPRQPermission.QUESTS
                )) {
                    return;
                }
                this.rdq.getViewFrame().open(
                    QuestCategoryView.class,
                    player,
                    Map.of(
                        "plugin",
                        this.rdq
                    )
                );
            }
            case RANKS -> {
                if (this.hasNoPermission(
                    player,
                    EPRQPermission.RANKS
                )) {
                    return;
                }
                this.rdq.getViewFrame().open(
                    RankMainView.class,
                    player,
                    Map.of(
                        "plugin",
                        this.rdq
                    )
                );
            }
            case SCOREBOARD -> this.handleScoreboardCommand(player, args);
            case PERKS -> {
                if (this.hasNoPermission(
                    player,
                    EPRQPermission.PERKS
                )) {
                    return;
                }
                
                // Load player data synchronously
                final var rdqPlayerOpt = this.rdq.getPlayerRepository().findByAttributes(
                    Map.of("uniqueId", player.getUniqueId())
                );
                
                if (rdqPlayerOpt.isEmpty()) {
                    new I18n.Builder("error.player_not_found", player).includePrefix().build().sendMessage();
                    return;
                }
                
                this.rdq.getViewFrame().open(
                    PerkOverviewView.class,
                    player,
                    Map.of(
                        "plugin",
                        this.rdq,
                        "player",
                        rdqPlayerOpt.get()
                    )
                );
            }
            default -> {
                if (! this.canAccessAnyAction(player)) {
                    this.hasNoPermission(
                        player,
                        EPRQPermission.COMMAND
                    );
                    return;
                }
                new I18n.Builder("rq.help", player).includePrefix().build().sendMessage();
            }
        }
    }
    
    /**
     * Provides tab completion suggestions for the command.
 *
 * <p>Currently returns an empty list, as there are no suggestions for this command.
     *
     * @param player the player requesting tab completion
     * @param label  the command label used
     * @param args   the current command arguments
     *
     * @return a list of tab completion suggestions (currently empty)
     */
    @Override
    protected List<String> onPlayerTabCompletion(
        final @NotNull Player player,
        final @NotNull String label,
        final @NotNull String[] args
    ) {

        if (
            args.length == 1
        ) {
            List<String> suggestions = new ArrayList<>(
                Arrays.stream(EPRQAction.values())
                    .filter(action -> action != EPRQAction.HELP)
                    .filter(action -> this.canAccessAction(
                        player,
                        action
                    ))
                    .map(Enum::name)
                    .map(name -> name.toLowerCase(Locale.ROOT))
                    .toList()
            );
            return StringUtil.copyPartialMatches(
                args[0].toLowerCase(),
                suggestions,
                new ArrayList<>()
            );
        }
        if (
            args.length == 2
                && "scoreboard".equalsIgnoreCase(args[0])
                && this.hasPermission(player, EPRQPermission.SCOREBOARD)
        ) {
            return StringUtil.copyPartialMatches(
                args[1].toLowerCase(),
                List.of(PERKS_SCOREBOARD_TYPE),
                new ArrayList<>()
            );
        }
        return new ArrayList<>();
    }

    private boolean canAccessAnyAction(
        final @NotNull Player player
    ) {

        return Arrays.stream(EPRQAction.values())
            .filter(action -> action != EPRQAction.HELP)
            .anyMatch(action -> this.canAccessAction(
                player,
                action
            ));
    }

    private boolean canAccessAction(
        final @NotNull Player player,
        final @NotNull EPRQAction action
    ) {

        return switch (action) {
            case ADMIN -> this.hasPermission(
                player,
                EPRQPermission.ADMIN
            );
            case BOUNTY -> this.hasPermission(
                player,
                EPRQPermission.BOUNTY
            );
            case MAIN -> this.hasPermission(
                player,
                EPRQPermission.MAIN
            );
            case QUESTS -> this.hasPermission(
                player,
                EPRQPermission.QUESTS
            );
            case RANKS -> this.hasPermission(
                player,
                EPRQPermission.RANKS
            );
            case SCOREBOARD -> this.hasPermission(
                player,
                EPRQPermission.SCOREBOARD
            );
            case PERKS -> this.hasPermission(
                player,
                EPRQPermission.PERKS
            );
            case HELP -> this.canAccessAnyAction(player);
        };
    }

    private void handleScoreboardCommand(
        final @NotNull Player player,
        final @NotNull String[] args
    ) {
        if (this.hasNoPermission(player, EPRQPermission.SCOREBOARD)) {
            return;
        }

        if (args.length != 2 || !PERKS_SCOREBOARD_TYPE.equalsIgnoreCase(args[1])) {
            new I18n.Builder("rq.scoreboard.syntax", player)
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        if (
            this.rdq.getPerkSidebarScoreboardService() == null
                || this.rdq.getPlayerRepository() == null
        ) {
            return;
        }

        final RDQPlayer playerData = this.getOrCreatePlayerData(player);
        final String messageKey;
        if (this.rdq.getPerkSidebarScoreboardService().isActive(player)) {
            this.rdq.getPerkSidebarScoreboardService().disable(player);
            playerData.setPerkSidebarScoreboardEnabled(false);
            messageKey = "rq.scoreboard.disabled";
        } else {
            this.rdq.getPerkSidebarScoreboardService().enable(player);
            playerData.setPerkSidebarScoreboardEnabled(true);
            messageKey = "rq.scoreboard.enabled";
        }

        this.rdq.getPlayerRepository().update(playerData);
        new I18n.Builder(messageKey, player)
            .includePrefix()
            .build()
            .sendMessage();
    }

    private @NotNull RDQPlayer getOrCreatePlayerData(
        final @NotNull Player player
    ) {
        final var existingPlayer = this.rdq.getPlayerRepository().findByAttributes(
            Map.of("uniqueId", player.getUniqueId())
        );
        if (existingPlayer.isPresent()) {
            return existingPlayer.get();
        }

        final RDQPlayer newPlayer = new RDQPlayer(player);
        this.rdq.getPlayerRepository().create(newPlayer);
        return newPlayer;
    }
}
