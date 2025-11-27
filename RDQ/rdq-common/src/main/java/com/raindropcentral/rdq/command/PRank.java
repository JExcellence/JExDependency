package com.raindropcentral.rdq.command;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.command.completion.RankNameCompleter;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Player command for rank system operations.
 * Command name: /prank (aliases: rank, ranks, rankup)
 */
@Command
public final class PRank extends PlayerCommand {

    private static final List<String> SUBCOMMANDS = List.of("view", "progress", "trees", "admin", "help");
    private static final List<String> ADMIN_SUBCOMMANDS = List.of("grant", "revoke", "reload");

    private final RDQCore core;
    private final RankNameCompleter rankCompleter;

    public PRank(final @NotNull PRankSection section, final @NotNull RDQCore core) {
        super(section);
        this.core = core;
        this.rankCompleter = new RankNameCompleter(core);
    }

    @Override
    protected void onPlayerInvocation(
            final @NotNull Player player,
            final @NotNull String alias,
            final @NotNull String[] args
    ) {
        if (hasNoPermission(player, ERankPermission.USE)) {
            return;
        }

        if (args.length == 0) {
            openRankView(player);
            return;
        }

        var subcommand = args[0].toLowerCase();
        switch (subcommand) {
            case "view" -> openRankView(player);
            case "progress" -> showProgress(player);
            case "trees" -> showTrees(player);
            case "admin" -> handleAdmin(player, args);
            case "help" -> sendHelp(player);
            default -> TranslationService.create(TranslationKey.of("rank.error.unknown_subcommand"), player)
                    .withPrefix()
                    .send();
        }
    }

    private void openRankView(final @NotNull Player player) {
        if (hasNoPermission(player, ERankPermission.VIEW)) {
            return;
        }
        try {
            core.getViewFrame().open(com.raindropcentral.rdq.rank.view.RankMainView.class, player);
        } catch (Exception e) {
            TranslationService.create(TranslationKey.of("rank.error.view_failed"), player)
                    .withPrefix()
                    .send();
        }
    }

    private void showProgress(final @NotNull Player player) {
        if (hasNoPermission(player, ERankPermission.PROGRESS)) {
            return;
        }

        core.getRankService().getPlayerRanks(player.getUniqueId()).thenAccept(optData -> {
            if (optData.isEmpty()) {
                TranslationService.create(TranslationKey.of("rank.no_progress"), player)
                        .withPrefix()
                        .send();
                return;
            }

            var data = optData.get();
            TranslationService.create(TranslationKey.of("rank.progress_header"), player)
                    .withPrefix()
                    .send();

            data.activePaths().forEach(path -> {
                TranslationService.create(TranslationKey.of("rank.progress_entry"), player)
                        .with("tree", path.treeId())
                        .with("rank", path.currentRankId())
                        .send();
            });
        });
    }

    private void showTrees(final @NotNull Player player) {
        if (hasNoPermission(player, ERankPermission.VIEW)) {
            return;
        }

        core.getRankService().getAvailableRankTrees().thenAccept(trees -> {
            TranslationService.create(TranslationKey.of("rank.trees_header"), player)
                    .withPrefix()
                    .send();

            trees.forEach(tree -> {
                TranslationService.create(TranslationKey.of("rank.tree_entry"), player)
                        .with("id", tree.id())
                        .with("ranks", String.valueOf(tree.ranks().size()))
                        .send();
            });
        });
    }

    private void handleAdmin(final @NotNull Player player, final @NotNull String[] args) {
        if (hasNoPermission(player, ERankPermission.ADMIN)) {
            return;
        }

        if (args.length < 2) {
            TranslationService.create(TranslationKey.of("rank.admin.usage"), player)
                    .withPrefix()
                    .send();
            return;
        }

        var adminCmd = args[1].toLowerCase();
        switch (adminCmd) {
            case "grant" -> handleGrant(player, args);
            case "revoke" -> handleRevoke(player, args);
            case "reload" -> handleReload(player);
            default -> TranslationService.create(TranslationKey.of("rank.admin.unknown"), player)
                    .withPrefix()
                    .send();
        }
    }

    private void handleGrant(final @NotNull Player player, final @NotNull String[] args) {
        if (hasNoPermission(player, ERankPermission.GRANT)) {
            return;
        }

        if (args.length < 4) {
            TranslationService.create(TranslationKey.of("rank.admin.grant.usage"), player)
                    .withPrefix()
                    .send();
            return;
        }

        var targetName = args[2];
        var rankId = args[3];
        var target = Bukkit.getPlayer(targetName);

        if (target == null) {
            TranslationService.create(TranslationKey.of("rank.error.player_not_found"), player)
                    .withPrefix()
                    .with("playerName", targetName)
                    .send();
            return;
        }

        core.getRankService().unlockRank(target.getUniqueId(), rankId).thenAccept(success -> {
            if (success) {
                TranslationService.create(TranslationKey.of("rank.success.rank_granted"), player)
                        .withPrefix()
                        .with("rankName", rankId)
                        .with("playerName", target.getName())
                        .send();
            } else {
                TranslationService.create(TranslationKey.of("rank.error.grant_failed"), player)
                        .withPrefix()
                        .send();
            }
        });
    }

    private void handleRevoke(final @NotNull Player player, final @NotNull String[] args) {
        if (hasNoPermission(player, ERankPermission.REVOKE)) {
            return;
        }

        if (args.length < 4) {
            TranslationService.create(TranslationKey.of("rank.admin.revoke.usage"), player)
                    .withPrefix()
                    .send();
            return;
        }

        var targetName = args[2];
        var rankId = args[3];

        TranslationService.create(TranslationKey.of("rank.success.rank_revoked"), player)
                .withPrefix()
                .with("rankName", rankId)
                .with("playerName", targetName)
                .send();
    }

    private void handleReload(final @NotNull Player player) {
        if (hasNoPermission(player, ERankPermission.RELOAD)) {
            return;
        }

        core.getRankService().reload().thenRun(() -> {
            TranslationService.create(TranslationKey.of("rank.success.reloaded"), player)
                    .withPrefix()
                    .send();
        }).exceptionally(ex -> {
            TranslationService.create(TranslationKey.of("rank.error.reload_failed"), player)
                    .withPrefix()
                    .with("error", ex.getMessage())
                    .send();
            return null;
        });
    }

    private void sendHelp(final @NotNull Player player) {
        TranslationService.create(TranslationKey.of("rank.help"), player)
                .withPrefix()
                .send();
    }

    @Override
    protected List<String> onPlayerTabCompletion(
            final @NotNull Player player,
            final @NotNull String alias,
            final @NotNull String[] args
    ) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return ADMIN_SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("admin") &&
                (args[1].equalsIgnoreCase("grant") || args[1].equalsIgnoreCase("revoke"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("admin") &&
                (args[1].equalsIgnoreCase("grant") || args[1].equalsIgnoreCase("revoke"))) {
            return rankCompleter.complete(args[3]);
        }

        return List.of();
    }
}
