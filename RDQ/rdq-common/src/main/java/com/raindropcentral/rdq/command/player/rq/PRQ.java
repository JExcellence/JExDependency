package com.raindropcentral.rdq.command.player.rq;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.view.admin.AdminOverviewView;
import com.raindropcentral.rdq.view.bounty.BountyMainView;
import com.raindropcentral.rdq.view.ranks.RankMainView;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Command
@SuppressWarnings("unused")
public class PRQ extends PlayerCommand {
    
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
     * <p>
     * Checks for the required permission and opens the admin overview view for the player.
     * </p>
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
        
        if (this.hasNoPermission(
            player,
            EPRQPermission.COMMAND
        )) {
            return;
        }
        
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
                }/*
                this.rdq.getViewFrame().open(
                    MainOverviewView.class,
                    player,
                    Map.of(
                        "plugin",
                        this.rdq
                    )
                );*/
            }
            case QUESTS -> {
                if (this.hasNoPermission(
                    player,
                    EPRQPermission.QUESTS
                )) {
                    return;
                }
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
            case PERKS -> {
                if (this.hasNoPermission(
                    player,
                    EPRQPermission.PERKS
                )) {
                    return;
                }/*
                this.rdq.getViewFrame().open(
                    PerksOverviewView.class,
                    player,
                    Map.of(
                        "plugin",
                        this.rdq
                    )
                );*/
            }
            default -> {
                new I18n.Builder("rq.help", player).includePrefix().build().sendMessage();
            }
        }
    }
    
    /**
     * Provides tab completion suggestions for the command.
     * <p>
     * Currently returns an empty list, as there are no suggestions for this command.
     * </p>
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
            this.hasNoPermission(
                player,
                EPRQPermission.COMMAND
            )
        ) {
            return new ArrayList<>();
        }
        
        if (
            args.length == 1
        ) {
            List<String> suggestions = new ArrayList<>(Arrays.stream(EPRQAction.values()).map(Enum::name).toList());
            return StringUtil.copyPartialMatches(
                args[0].toLowerCase(),
                suggestions,
                new ArrayList<>()
            );
        }
        return new ArrayList<>();
    }
}