package com.raindropcentral.rdq.command.player.rq;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.view.bounty.BountyMainView;
import com.raindropcentral.rdq.view.perks.PerkListViewFrame;
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
 * @version 1.0.2
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
            case PERKS -> this.handlePerksCommand(player, args);
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
     * Handles perk-related commands with sub-commands for listing, info, activation, and deactivation.
     * Falls back to opening the perks GUI if no valid sub-command is provided.
     *
     * @param player player executing the command
     * @param args command arguments including the 'perks' action
     */
    private void handlePerksCommand(final @NotNull Player player, final @NotNull String[] args) {
        if (this.hasNoPermission(player, ERQPermission.PERKS)) {
            return;
        }

        // Check if there are additional arguments for sub-commands
        if (args.length >= 2) {
            final String subCommand = args[1].toLowerCase();

            switch (subCommand) {
                case "list" -> this.handlePerkListCommand(player);
                case "info" -> this.handlePerkInfoCommand(player, args);
                case "activate" -> this.handlePerkActivateCommand(player, args);
                case "deactivate" -> this.handlePerkDeactivateCommand(player, args);
                default -> this.openPerksView(player); // Invalid sub-command, open GUI
            }
        } else {
            // No sub-command provided, open the perks GUI
            this.openPerksView(player);
        }
    }

    /**
     * Lists all available perks with their current status.
     *
     * @param player the player executing the command
     */
    private void handlePerkListCommand(final @NotNull Player player) {
        final var perkInitManager = this.rdq.getPerkInitializationManager();
        final var perkManager = perkInitManager.getPerkManager();
        final var registry = perkInitManager.getPerkRegistry();
        final var perks = registry.getAll();

        if (perks.isEmpty()) {
            TranslationService.create(TranslationKey.of("perk.error.no_perks"), player)
                    .withPrefix()
                    .send();
            return;
        }

        TranslationService.create(TranslationKey.of("perk.command.list_header"), player)
                .withPrefix()
                .send();

        for (final var perk : perks) {
            final boolean active = perkManager.isActive(player, perk.getId());
            final String status = active ? "✓" : "✗";
            TranslationService.create(TranslationKey.of("perk.command.list_entry"), player)
                    .with("perk", perk.getDisplayName())
                    .with("status", status)
                    .send();
        }
    }

    /**
     * Displays detailed information about a specific perk.
     *
     * @param player the player executing the command
     * @param args command arguments, expecting perk ID at index 2
     */
    private void handlePerkInfoCommand(final @NotNull Player player, final @NotNull String[] args) {

        final var perkInitManager = this.rdq.getPerkInitializationManager();
        final var perkManager = perkInitManager.getPerkManager();

        if (args.length < 3) {
            TranslationService.create(TranslationKey.of("perk.error.not_found"), player)
                    .with("perk", "missing_id")
                    .withPrefix()
                    .send();
            return;
        }

        final String perkId = args[2];
        final var perk = perkInitManager.getPerkRegistry().get(perkId);

        if (perk == null) {
            TranslationService.create(TranslationKey.of("perk.error.not_found"), player)
                    .with("perk", perkId)
                    .withPrefix()
                    .send();
            return;
        }

        final boolean active = perkManager.isActive(player, perk.getId());

        TranslationService.create(TranslationKey.of("perk.command.info_header"), player)
                .withPrefix()
                .with("perk", perk.getDisplayName())
                .send();

        TranslationService.create(TranslationKey.of("perk.command.info_display_name"), player)
                .with("name", perk.getDisplayName())
                .send();

        TranslationService.create(TranslationKey.of("perk.command.info_description"), player)
                .with("description", perk.getDisplayName())
                .send();

        TranslationService.create(TranslationKey.of("perk.command.info_status"), player)
                .with("status", active ? "Active" : "Inactive")
                .send();

        TranslationService.create(TranslationKey.of("perk.command.info_category"), player)
                .with("category", perk.getTypeId())
                .send();
    }

    /**
     * Activates a perk for the player.
     *
     * @param player the player executing the command
     * @param args command arguments, expecting perk ID at index 2
     */
    private void handlePerkActivateCommand(final @NotNull Player player, final @NotNull String[] args) {
        if (args.length < 3) {
            TranslationService.create(TranslationKey.of("perk.error.not_found"), player)
                    .with("perk", "missing_id")
                    .withPrefix()
                    .send();
            return;
        }

        if (!player.hasPermission("rdq.perk.activate")) {
            TranslationService.create(TranslationKey.of("perk.error.no_permission"), player)
                    .with("permission", "rdq.perk.activate")
                    .withPrefix()
                    .send();
            return;
        }

        final String perkId = args[2];
        final var initializationManager = this.rdq.getPerkInitializationManager();
        final var perkManager = initializationManager.getPerkManager();
        final var runtime = perkManager.findRuntime(perkId);

        if (runtime.isEmpty()) {
            TranslationService.create(TranslationKey.of("perk.error.not_found"), player)
                    .with("perk", perkId)
                    .withPrefix()
                    .send();
            return;
        }

        final var perk = initializationManager.getPerkRegistry().get(perkId);
        if (perk == null) {
            TranslationService.create(TranslationKey.of("perk.error.not_found"), player)
                    .with("perk", perkId)
                    .withPrefix()
                    .send();
            return;
        }

        if (runtime.get().isActive(player)) {
            TranslationService.create(TranslationKey.of("perk.error.already_active"), player)
                    .with("perk", perk.getDisplayName())
                    .withPrefix()
                    .send();
            return;
        }

        if (!perkManager.activate(player, perkId)) {
            TranslationService.create(TranslationKey.of("perk.error.activation_failed"), player)
                    .with("perk", perk.getDisplayName())
                    .withPrefix()
                    .send();
            return;
        }

        TranslationService.create(TranslationKey.of("perk.command.activated"), player)
                .withPrefix()
                .with("perk", perk.getDisplayName())
                .send();
    }

    /**
     * Deactivates a perk for the player.
     *
     * @param player the player executing the command
     * @param args command arguments, expecting perk ID at index 2
     */
    private void handlePerkDeactivateCommand(final @NotNull Player player, final @NotNull String[] args) {
        if (args.length < 3) {
            TranslationService.create(TranslationKey.of("perk.error.not_found"), player)
                    .with("perk", "missing_id")
                    .withPrefix()
                    .send();
            return;
        }

        if (!player.hasPermission("rdq.perk.deactivate")) {
            TranslationService.create(TranslationKey.of("perk.error.no_permission"), player)
                    .with("permission", "rdq.perk.deactivate")
                    .withPrefix()
                    .send();
            return;
        }

        final String perkId = args[2];
        final var initializationManager = this.rdq.getPerkInitializationManager();
        final var perkManager = initializationManager.getPerkManager();
        final var runtime = perkManager.findRuntime(perkId);

        if (runtime.isEmpty()) {
            TranslationService.create(TranslationKey.of("perk.error.not_found"), player)
                    .with("perk", perkId)
                    .withPrefix()
                    .send();
            return;
        }

        final var perk = initializationManager.getPerkRegistry().get(perkId);
        if (perk == null) {
            TranslationService.create(TranslationKey.of("perk.error.not_found"), player)
                    .with("perk", perkId)
                    .withPrefix()
                    .send();
            return;
        }

        if (!runtime.get().isActive(player)) {
            TranslationService.create(TranslationKey.of("perk.error.not_active"), player)
                    .with("perk", perk.getDisplayName())
                    .withPrefix()
                    .send();
            return;
        }

        if (!perkManager.deactivate(player, perkId)) {
            TranslationService.create(TranslationKey.of("perk.error.deactivation_failed"), player)
                    .with("perk", perk.getDisplayName())
                    .withPrefix()
                    .send();
            return;
        }

        TranslationService.create(TranslationKey.of("perk.command.deactivated"), player)
                .withPrefix()
                .with("perk", perk.getDisplayName())
                .send();
    }

    /**
     * Opens the perks GUI view for the player.
     *
     * @param player the player to open the view for
     */
    private void openPerksView(final @NotNull Player player) {
        this.rdq.getViewFrame().open(
                PerkListViewFrame.class,
                player,
                Map.of("plugin", this.rdq, "player", this.rdq.getPlayerRepository().findByUuidAsync(player.getUniqueId()))
        );
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