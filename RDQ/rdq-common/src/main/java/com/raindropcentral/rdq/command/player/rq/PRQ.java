package com.raindropcentral.rdq.command.player.rq;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.view.bounty.BountyMainView;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles the execution and tab completion of the RaindropQuests command for players.
 * <p>
 * This command allows players to interact with various RDQ systems including:
 * <ul>
 * <li>Admin panel (requires admin permission)</li>
 * <li>Bounty system</li>
 * <li>Main overview</li>
 * <li>Quest system</li>
 * <li>Rank system</li>
 * <li>Perk system</li>
 * </ul>
 * Command is automatically registered using {@link Command} annotation.
 * </p>
 *
 * @author ItsRainingHP, JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
@Command
@SuppressWarnings("unused")
public final class PRQ extends PlayerCommand {

    private final RDQ rdq;

    public PRQ(
            final @NotNull PRQSection commandSection,
            final @NotNull RDQ rdq
    ) {
        super(commandSection);
        this.rdq = rdq;
    }

    @Override
    protected void onPlayerInvocation(
            final @NotNull Player player,
            final @NotNull String label,
            final @NotNull String[] args
    ) {
        if (this.hasNoPermission(player, ERQPermission.COMMAND)) {
            return;
        }

        final EPRQAction action = enumParameterOrElse(
                args,
                0,
                EPRQAction.class,
                EPRQAction.HELP
        );

        switch (action) {
            case ADMIN -> this.handleAdminCommand(player, args);
            case BOUNTY -> this.handleBountyCommand(player);
            case MAIN -> this.handleMainCommand(player);
            case QUESTS -> this.handleQuestsCommand(player);
            case RANKS -> this.handleRanksCommand(player);
            case PERKS -> this.handlePerksCommand(player);
            default -> this.handleHelpCommand(player);
        }
    }

    @Override
    protected List<String> onPlayerTabCompletion(
            final @NotNull Player player,
            final @NotNull String label,
            final @NotNull String[] args
    ) {
        if (this.hasNoPermission(player, ERQPermission.COMMAND)) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            final List<String> suggestions = Arrays.stream(EPRQAction.values())
                    .map(Enum::name)
                    .map(String::toLowerCase)
                    .toList();

            return StringUtil.copyPartialMatches(
                    args[0].toLowerCase(),
                    suggestions,
                    new ArrayList<>()
            );
        }

        return new ArrayList<>();
    }

    private void handleAdminCommand(
            final @NotNull Player player,
            final @NotNull String[] args
    ) {
        if (this.hasNoPermission(player, ERQPermission.ADMIN)) {
            return;
        }
/*
        this.rdq.getViewFrame().open(
                AdminOverviewView.class,
                player,
                Map.of(
                        "plugin", this.rdq,
                        "pluginName", args.length >= 2 ? stringParameter(args, 1) : ""
                )
        );*/
    }

    private void handleBountyCommand(final @NotNull Player player) {
        if (this.hasNoPermission(player, ERQPermission.BOUNTY)) {
            return;
        }

        this.rdq.getViewFrame().open(
                BountyMainView.class,
                player
        );
    }

    private void handleMainCommand(final @NotNull Player player) {
        if (this.hasNoPermission(player, ERQPermission.MAIN)) {
            return;
        }
/*
        this.rdq.getViewFrame().open(
                MainOverviewView.class,
                player,
                Map.of("plugin", this.rdq)
        );*/
    }

    private void handleQuestsCommand(final @NotNull Player player) {
        if (this.hasNoPermission(player, ERQPermission.QUESTS)) {
            return;
        }
/*
        this.rdq.getViewFrame().open(
                QuestOverviewView.class,
                player,
                Map.of("plugin", this.rdq)
        );*/
    }

    private void handleRanksCommand(final @NotNull Player player) {
        if (this.hasNoPermission(player, ERQPermission.RANKS)) {
            return;
        }
/*
        this.rdq.getViewFrame().open(
                RankMainView.class,
                player,
                Map.of("plugin", this.rdq)
        );*/
    }

    private void handlePerksCommand(final @NotNull Player player) {
        if (this.hasNoPermission(player, ERQPermission.PERKS)) {
            return;
        }
/*
        this.rdq.getViewFrame().open(
                PerksOverviewView.class,
                player,
                Map.of("plugin", this.rdq)
        );*/
    }

    private void handleHelpCommand(final @NotNull Player player) {
        TranslationService.create(
                TranslationKey.of("rq.help"),
                player
        ).withPrefix().send();
    }
}