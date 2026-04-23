package de.jexcellence.quests.command;

import com.raindropcentral.commands.v2.CommandContext;
import com.raindropcentral.commands.v2.CommandHandler;
import de.jexcellence.jextranslate.R18nManager;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.QuestsPlayer;
import de.jexcellence.quests.service.QuestsPlayerService;
import de.jexcellence.quests.service.RankService;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/** JExCommand 2.0 handlers for {@code /rank}. */
public final class RankHandler {

    private final JExQuests quests;
    private final RankService rankService;
    private final QuestsPlayerService questsPlayers;

    public RankHandler(@NotNull JExQuests quests) {
        this.quests = quests;
        this.rankService = quests.rankService();
        this.questsPlayers = quests.questsPlayerService();
    }

    public @NotNull Map<String, CommandHandler> handlerMap() {
        return Map.ofEntries(
                Map.entry("rank", this::onBrowse),
                Map.entry("rank.info", this::onInfo),
                Map.entry("rank.promote", this::onPromote),
                Map.entry("rank.path", this::onPath),
                Map.entry("rank.top", this::onTop)
        );
    }

    private void onBrowse(@NotNull CommandContext ctx) {
        ctx.asPlayer().ifPresentOrElse(
                this::openRankDashboard,
                () -> r18n().msg("error.player-only").prefix().send(ctx.sender())
        );
    }

    /**
     * /rank landing — prints the header plus a summary of the player's
     * currently-active path and rank, then opens the tree overview.
     * Giving a chat line as well as a GUI means the player sees
     * translated feedback every time the command runs, not just when
     * the view happens to render correctly.
     */
    private void openRankDashboard(@NotNull Player player) {
        this.questsPlayers.findAsync(player.getUniqueId()).thenAccept(opt -> {
            final String activeTree = opt.map(QuestsPlayer::getActiveRankTree).orElse(null);
            r18n().msg("rank.header").send(player);
            if (activeTree == null) {
                r18n().msg("rank.current").prefix()
                        .with("rank", "—")
                        .with("path", "—")
                        .send(player);
            } else {
                this.rankService.playerRankRepository()
                        .findAsync(player.getUniqueId(), activeTree)
                        .thenAccept(rankOpt -> {
                            final String rankId = rankOpt.map(r -> r.getCurrentRankIdentifier()).orElse("—");
                            r18n().msg("rank.current").prefix()
                                    .with("rank", rankId)
                                    .with("path", activeTree)
                                    .send(player);
                        });
            }
            r18n().msg("rank.usage").prefix().send(player);
        });

        this.quests.viewFrame().open(
                de.jexcellence.quests.view.RankMainView.class,
                player,
                Map.of("plugin", this.quests)
        );
    }

    /**
     * {@code /rank info [player] [tree]} — prints the active tree, the
     * current rank, and the gated-requirement summary for the player's
     * next promotion. The old implementation just echoed back the
     * command arguments; this version actually resolves the player's
     * {@link de.jexcellence.quests.database.entity.PlayerRank} row and
     * the requirement describer on the next rank so players can see
     * exactly what they still need to fulfil.
     */
    private void onInfo(@NotNull CommandContext ctx) {
        final OfflinePlayer target = ctx.get("player", OfflinePlayer.class).orElseGet(() ->
                ctx.asPlayer().map(p -> (OfflinePlayer) p).orElse(null));
        if (target == null) {
            r18n().msg("error.player-only").prefix().send(ctx.sender());
            return;
        }
        final String explicitTree = ctx.get("tree", String.class).orElse(null);
        this.questsPlayers.findAsync(target.getUniqueId()).thenAccept(optProfile -> {
            final String treeId = explicitTree != null
                    ? explicitTree
                    : optProfile.map(QuestsPlayer::getActiveRankTree).orElse(null);
            if (treeId == null || treeId.isBlank()) {
                r18n().msg("rank.info.no-path").prefix()
                        .with("player", target.getName() != null ? target.getName() : "?")
                        .send(ctx.sender());
                return;
            }
            emitInfoLines(ctx, target, treeId);
        });
    }

    private void emitInfoLines(@NotNull CommandContext ctx, @NotNull OfflinePlayer target, @NotNull String treeId) {
        final var rankService = this.rankService;
        rankService.trees().findByIdentifierAsync(treeId).thenAcceptBoth(
                rankService.playerRankRepository().findAsync(target.getUniqueId(), treeId),
                (optTree, optPr) -> {
                    if (optTree.isEmpty()) {
                        r18n().msg("rank.path-not-found").prefix().with("path", treeId)
                                .with("reason", "disabled or removed").send(ctx.sender());
                        return;
                    }
                    if (optPr.isEmpty()) {
                        r18n().msg("rank.info.not-enrolled").prefix()
                                .with("player", target.getName() != null ? target.getName() : "?")
                                .with("path", treeId).send(ctx.sender());
                        return;
                    }
                    final var playerRank = optPr.get();
                    r18n().msg("rank.header").send(ctx.sender());
                    r18n().msg("rank.info.summary").prefix()
                            .with("player", target.getName() != null ? target.getName() : "?")
                            .with("path", treeId)
                            .with("rank", playerRank.getCurrentRankIdentifier())
                            .with("progress", String.valueOf(playerRank.getProgressionPercent()))
                            .with("completed", String.valueOf(playerRank.isTreeCompleted()))
                            .send(ctx.sender());
                    // Enumerate immediate next ranks so the player sees every
                    // branch option plus its gate in one readable block.
                    rankService.ranks().findByTreeAsync(optTree.get()).thenAccept(ranks -> {
                        final var current = ranks.stream()
                                .filter(r -> r.getIdentifier().equals(playerRank.getCurrentRankIdentifier()))
                                .findFirst().orElse(null);
                        if (current == null) return;
                        if (current.nextRankList().isEmpty()) {
                            r18n().msg("rank.info.at-end").prefix().send(ctx.sender());
                            return;
                        }
                        for (final String nextId : current.nextRankList()) {
                            final var next = ranks.stream()
                                    .filter(r -> r.getIdentifier().equals(nextId))
                                    .findFirst().orElse(null);
                            if (next == null) continue;
                            r18n().msg("rank.info.next-option").prefix()
                                    .with("rank", next.getIdentifier())
                                    .with("requirements", de.jexcellence.quests.view.RequirementDescriber
                                            .describe(next.getRequirementData()))
                                    .with("rewards", de.jexcellence.quests.view.RewardDescriber
                                            .describe(next.getRewardData()))
                                    .send(ctx.sender());
                        }
                    });
                });
    }

    private void onPromote(@NotNull CommandContext ctx) {
        final Player player = ctx.asPlayer().orElseThrow();
        resolveTree(player.getUniqueId(), ctx).thenAccept(tree -> {
            if (tree == null) {
                r18n().msg("rank.path-not-found").prefix().with("path", "(none)").send(player);
                return;
            }
            this.rankService.promoteAsync(player.getUniqueId(), tree).thenAccept(result -> {
                switch (result.status()) {
                    case PROMOTED -> r18n().msg("rank.promoted").prefix()
                            .with("rank", result.rank())
                            .with("rewards", "—")
                            .send(player);
                    case REQUIREMENTS_NOT_MET -> r18n().msg("rank.requirements-missing").prefix()
                            .with("requirements", "see /rank info").send(player);
                    case TREE_COMPLETED -> r18n().msg("rank.max-reached").prefix().send(player);
                    case NOT_ENROLLED, NOT_FOUND -> r18n().msg("rank.path-not-found").prefix().with("path", tree).send(player);
                    case ERROR -> r18n().msg("error.unknown").prefix().with("error", result.error() != null ? result.error() : "?").send(player);
                }
            });
        });
    }

    private void onPath(@NotNull CommandContext ctx) {
        final Player player = ctx.asPlayer().orElseThrow();
        final String tree = ctx.require("tree", String.class);
        this.rankService.enrollAsync(player.getUniqueId(), tree).thenAccept(pr -> {
            if (pr == null) {
                r18n().msg("rank.path-not-found").prefix().with("path", tree).send(player);
                return;
            }
            this.questsPlayers.trackAsync(player.getUniqueId()).thenAccept(row -> {
                if (row == null) return;
                row.setActiveRankTree(tree);
                this.questsPlayers.repository().update(row);
                r18n().msg("rank.path-switched").prefix().with("path", tree).send(player);
            });
        });
    }

    private void onTop(@NotNull CommandContext ctx) {
        final String tree = ctx.require("tree", String.class);
        final int limit = ctx.get("limit", Integer.class).orElse(10);
        ctx.asPlayer().ifPresentOrElse(
                player -> this.quests.viewFrame().open(
                        de.jexcellence.quests.view.RankTopView.class,
                        player,
                        Map.of("plugin", this.quests, "tree", tree)
                ),
                () -> emitTopInChat(ctx, tree, limit)
        );
    }

    private void emitTopInChat(@NotNull CommandContext ctx, @NotNull String tree, int limit) {
        this.rankService.topAsync(tree, limit).thenAccept(entries -> {
            if (entries.isEmpty()) {
                r18n().msg("rank.top-empty").prefix().send(ctx.sender());
                return;
            }
            for (final var entry : entries) {
                r18n().msg("rank.top-entry").prefix()
                        .with("position", String.valueOf(entry.position()))
                        .with("player", entry.playerName())
                        .with("rank", entry.rankIdentifier())
                        .with("path", entry.treeIdentifier())
                        .send(ctx.sender());
            }
        });
    }

    private @NotNull java.util.concurrent.CompletableFuture<String> resolveTree(@NotNull UUID uuid, @NotNull CommandContext ctx) {
        final String explicit = ctx.get("tree", String.class).orElse(null);
        if (explicit != null) return java.util.concurrent.CompletableFuture.completedFuture(explicit);
        return this.questsPlayers.findAsync(uuid).thenApply(opt -> opt.map(QuestsPlayer::getActiveRankTree).orElse(null));
    }

    private static R18nManager r18n() { return R18nManager.getInstance(); }
}
