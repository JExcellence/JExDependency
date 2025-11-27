package com.raindropcentral.rdq.command.player.rq;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.view.bounty.BountyCreationView;
import com.raindropcentral.rdq.view.bounty.BountyMainView;
import com.raindropcentral.rdq.view.perks.PerkMainView;
import com.raindropcentral.rdq.view.rank.view.RankMainView;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;


@Command
@SuppressWarnings("unused")
public final class PRQ extends PlayerCommand {

    private final RDQ rdq;

    public PRQ(@NotNull PRQSection commandSection, @NotNull RDQ rdq) {
        super(commandSection);
        this.rdq = rdq;
    }

    @Override
    protected void onPlayerInvocation(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (hasNoPermission(player, ERQPermission.COMMAND)) return;

        var action = enumParameterOrElse(args, 0, EPRQAction.class, EPRQAction.HELP);
        switch (action) {
            case ADMIN -> handleAdminCommand(player, args);
            case BOUNTY -> handleBountyCommand(player, args);
            case MAIN -> handleMainCommand(player);
            case QUESTS -> handleQuestsCommand(player);
            case RANKS -> handleRanksCommand(player);
            case PERKS -> handlePerksCommand(player, args);
            default -> handleHelpCommand(player);
        }
    }

    @Override
    protected List<String> onPlayerTabCompletion(@NotNull Player player, @NotNull String label, @NotNull String[] args) {
        if (hasNoPermission(player, ERQPermission.COMMAND)) return new ArrayList<>();

        // First argument: main action (/rq <action>)
        if (args.length == 1) {
            var suggestions = Arrays.stream(EPRQAction.values())
                    .map(Enum::name)
                    .map(String::toLowerCase)
                    .toList();
            return StringUtil.copyPartialMatches(args[0].toLowerCase(), suggestions, new ArrayList<>());
        }

        // Second argument after /rq bounty -> suggest "create"
        if (args.length == 2) {
            var action = enumParameterOrElse(args, 0, EPRQAction.class, EPRQAction.HELP);
            if (action == EPRQAction.BOUNTY) {
                var suggestions = List.of("create");
                return StringUtil.copyPartialMatches(args[1].toLowerCase(), suggestions, new ArrayList<>());
            }
        }

        // Third argument for /rq bounty create <player> -> suggest online player names
        if (args.length == 3) {
            var action = enumParameterOrElse(args, 0, EPRQAction.class, EPRQAction.HELP);
            if (action == EPRQAction.BOUNTY && "create".equalsIgnoreCase(args[1])) {
                return StringUtil.copyPartialMatches(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), new ArrayList<>());
            }
        }

        return new ArrayList<>();
    }

    private void handleAdminCommand(@NotNull Player player, @NotNull String[] args) {
        if (hasNoPermission(player, ERQPermission.ADMIN)) return;
        
        if (args.length >= 2 && args[1].equalsIgnoreCase("reload")) {
            if (args.length >= 3 && args[2].equalsIgnoreCase("ranks")) {
                player.sendMessage("§aReloading rank system...");
                rdq.reloadRankSystem().thenRun(() -> {
                    player.sendMessage("§aRank system reloaded successfully!");
                }).exceptionally(ex -> {
                    player.sendMessage("§cFailed to reload rank system: " + ex.getMessage());
                    return null;
                });
                return;
            }
        }
        
        // Admin view disabled during UI revision
        player.sendMessage("§cUsage: /rq admin reload ranks");
    }

    private void handleBountyCommand(@NotNull Player player, @NotNull String[] args) {
        if (hasNoPermission(player, ERQPermission.BOUNTY)) return;

        // /rq bounty -> open main bounty view
        if (args.length == 1) {
            rdq.getViewFrame().open(BountyMainView.class, player, Map.of("plugin", rdq));
            return;
        }

        // /rq bounty create <player>
        if (args.length >= 3 && args[1].equalsIgnoreCase("create")) {
            String targetName = args[2];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
                TranslationService.create(TranslationKey.of("bounty.error.player_not_found"), player)
                        .withPrefix()
                        .with("player", targetName)
                        .send();
                return;
            }

            rdq.getViewFrame().open(BountyCreationView.class, player, Map.of(
                    "plugin", rdq,
                    "target", Optional.of(target),
                    "reward_items", new HashSet<>(),
                    "reward_currencies", new HashMap<>(),
                    "bounty", Optional.empty(),
                    "inserted_items", new HashMap<>()
            ));
        }
    }

    private void handleMainCommand(@NotNull Player player) {
        if (hasNoPermission(player, ERQPermission.MAIN)) return;
        // Main overview view disabled during UI revision
    }

    private void handleQuestsCommand(@NotNull Player player) {
        if (hasNoPermission(player, ERQPermission.QUESTS)) return;
        // Quest overview view disabled during UI revision
    }

    private void handleRanksCommand(@NotNull Player player) {
        if (hasNoPermission(player, ERQPermission.RANKS)) return;
        rdq.getViewFrame().open(RankMainView.class, player, Map.of("plugin", rdq));
    }

    private void handlePerksCommand(@NotNull Player player, @NotNull String[] args) {
        if (hasNoPermission(player, ERQPermission.PERKS)) return;

        if (args.length >= 2) {
            var subCommand = args[1].toLowerCase();
            switch (subCommand) {
                case "list" -> handlePerkListCommand(player);
                case "info" -> handlePerkInfoCommand(player, args);
                case "activate" -> handlePerkActivateCommand(player, args);
                case "deactivate" -> handlePerkDeactivateCommand(player, args);
                default -> openPerksView(player);
            }
        } else {
            openPerksView(player);
        }
    }

    private void handlePerkListCommand(@NotNull Player player) {
        var perkInitManager = rdq.getPerkInitializationManager();
        var perkManager = perkInitManager.getPerkManager();
        var registry = perkInitManager.getPerkRegistry();
        var perks = registry.getAll();

        if (perks.isEmpty()) {
            TranslationService.create(TranslationKey.of("perk.error.no_perks"), player).withPrefix().send();
            return;
        }

        TranslationService.create(TranslationKey.of("perk.command.list_header"), player).withPrefix().send();
        perks.forEach(perk -> {
            var active = perkManager.isActive(player, perk.getId());
            var status = active ? "✓" : "✗";
            TranslationService.create(TranslationKey.of("perk.command.list_entry"), player)
                    .with("perk", perk.getDisplayName())
                    .with("status", status)
                    .send();
        });
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

        if (!runtime.get().canActivate(player)) {
            TranslationService.create(TranslationKey.of("perk.error.activation_denied"), player)
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

    private void openPerksView(@NotNull Player player) {
        rdq.getViewFrame().open(PerkMainView.class, player, Map.of("plugin", rdq));
    }

    private void handleHelpCommand(@NotNull Player player) {
        TranslationService.create(TranslationKey.of("rq.help"), player).withPrefix().send();
    }
}
