package com.raindropcentral.rdq.command.player.rq;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.view.bounty.BountyMainView;
import com.raindropcentral.rdq.view.rank.view.RankMainView;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Primary player-facing entry point for the {@code /prq} command tree.
 * <p>
 * The root command is auto-registered by the {@link Command} annotation and
 * orchestrates navigation to edition-aware views such as the bounty browser.
 * Permission guards sourced from {@link ERQPermission} ensure only eligible
 * players gain access to administrative tooling while keeping public surfaces
 * available to all permitted users.
 * </p>
 *
 * @implNote The handler intentionally keeps routing logic lightweight so that
 * per-action methods can describe their own permission checks and view
 * transitions. This mirrors the rest of the RDQ command suite and keeps
 * tracking of additions confined to a single switch statement.
 *
 * @see PRQSection
 * @see ERQPermission
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Command
@SuppressWarnings("unused")
public final class PRQ extends PlayerCommand {

    /**
     * Active plugin context that supplies view frames and edition-aware
     * managers used to fulfil command actions.
     */
    private final RDQ rdq;

    /**
     * Creates the command handler with the resolved command section and RDQ
     * runtime.
     *
     * @implNote The constructor stores the {@link RDQ} instance so that view
     * lookups remain centralized in the handler methods. This avoids repeated
     * dependency resolution during command execution.
     *
     * @param commandSection section metadata produced by {@link PRQSection}
     *                       defining the root command and evaluation context
     * @param rdq            plugin instance exposing views, managers, and
     *                       edition controls for downstream actions
     */
    public PRQ(
            final @NotNull PRQSection commandSection,
            final @NotNull RDQ rdq
    ) {
        super(commandSection);
        this.rdq = rdq;
    }

    /**
     * Executes the player command, selecting an {@link EPRQAction} based on the
     * first argument, enforcing base permissions, and delegating to the
     * respective action handler.
     *
     * @implSpec The lookup uses {@link #enumParameterOrElse(String[], int, Class,
     * Enum) enumParameterOrElse} so downstream handlers only run after
     * permission checks succeed.
     * Implementations adding new actions should update the {@code switch}
     * statement so that tab completion and execution stay aligned.
     *
     * @param player invoking player
     * @param label  alias used for invocation
     * @param args   command arguments supplied by the player
     */
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

    /**
     * Provides tab completions for the command by filtering available
     * {@link EPRQAction} values when the base permission is satisfied.
     *
     * @implNote Only the first argument is supported for completions. Further
     * arguments are ignored intentionally so that future action-specific
     * completions can be implemented inside each handler without conflicting
     * with the root command.
     *
     * @param player player requesting completions
     * @param label  alias used for invocation
     * @param args   current command arguments
     * @return matching completions limited to actions the player can access
     */
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

    /**
     * Routes the player to administrative tooling when the admin permission is
     * granted.
     *
     * @implNote The administrative view remains disabled while the UI is under
     * revision. The method keeps the current signature so that the commented
     * implementation can be restored without altering callers.
     *
     * @param player player executing the command
     * @param args   command arguments including optional plugin name hints
     */
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

    /**
     * Opens the bounty view when the player holds the bounty permission.
     *
     * @param player player executing the command
     */
    private void handleBountyCommand(final @NotNull Player player) {
        if (this.hasNoPermission(player, ERQPermission.BOUNTY)) {
            return;
        }

        this.rdq.getViewFrame().open(
                BountyMainView.class,
                player
        );
    }

    /**
     * Entry point for the main overview guarded by the main permission.
     *
     * @param player player executing the command
     */
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

    /**
     * Handles quest overview navigation when the quest permission passes.
     *
     * @param player player executing the command
     */
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

    /**
     * Directs players to the ranks UI if the rank permission is available.
     *
     * @param player player executing the command
     */
    private void handleRanksCommand(final @NotNull Player player) {
        if (this.hasNoPermission(player, ERQPermission.RANKS)) {
            return;
        }

        this.rdq.getViewFrame().open(
                RankMainView.class,
                player,
                Map.of("plugin", this.rdq)
        );
    }

    /**
     * Opens the perks interface for players holding the perks permission.
     *
     * @param player player executing the command
     */
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

    /**
     * Sends the localized help message, providing guidance when no specific
     * action is selected or available.
     *
     * @param player player requesting help
     */
    private void handleHelpCommand(final @NotNull Player player) {
        TranslationService.create(
                TranslationKey.of("rq.help"),
                player
        ).withPrefix().send();
    }
}