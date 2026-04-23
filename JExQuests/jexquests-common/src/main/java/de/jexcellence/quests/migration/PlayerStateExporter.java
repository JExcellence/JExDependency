package de.jexcellence.quests.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.api.PlayerSnapshot;
import de.jexcellence.quests.database.entity.PlayerPerk;
import de.jexcellence.quests.database.entity.PlayerQuestProgress;
import de.jexcellence.quests.database.entity.PlayerRank;
import de.jexcellence.quests.database.entity.PlayerTaskProgress;
import de.jexcellence.quests.database.entity.QuestsPlayer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Async one-shot exporter that gathers a player's full JExQuests
 * state into a {@link PlayerSnapshot} and writes it to
 * {@code plugins/JExQuests/exports/<uuid>_<timestamp>.json}.
 *
 * <p>The snapshot captures {@link QuestsPlayer} (profile),
 * {@link PlayerQuestProgress} (every attempt, all statuses),
 * {@link PlayerTaskProgress}, {@link PlayerRank}, and
 * {@link PlayerPerk} rows. Bounties are intentionally omitted —
 * they're server-scoped ephemeral state.
 */
public final class PlayerStateExporter {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String EXPORT_DIR = "exports";

    private final JExQuests quests;
    private final JExLogger logger;
    private final ObjectMapper mapper;

    public PlayerStateExporter(@NotNull JExQuests quests) {
        this.quests = quests;
        this.logger = quests.logger();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Builds the snapshot + writes to disk. Returns the absolute path
     * to the exported file or an errored future when no state was
     * found / serialization failed.
     */
    public @NotNull CompletableFuture<Path> exportAsync(@NotNull UUID playerUuid) {
        return buildSnapshotAsync(playerUuid)
                .thenCompose(snapshot -> CompletableFuture.supplyAsync(() -> writeSnapshot(snapshot)));
    }

    /**
     * Bulk export — enumerates every row in {@code jexquests_player}
     * and dumps each player's snapshot into a single timestamped
     * batch folder under {@code plugins/JExQuests/exports/batch-<ts>/}.
     * Failures are collected but don't short-circuit the sweep; the
     * returned {@link BatchResult} reports per-player outcome so admins
     * can retry the stragglers.
     *
     * <p>Export runs sequentially to avoid hammering the Hibernate
     * session manager with N concurrent profile loads — the bottleneck
     * on large shards is IO, not CPU.
     */
    public @NotNull CompletableFuture<BatchResult> exportAllAsync() {
        return this.quests.questsPlayerService().repository().findAllAsync().thenCompose(rows -> {
            if (rows.isEmpty()) {
                return CompletableFuture.completedFuture(new BatchResult(
                        resolveExportsRoot(), 0, 0, List.of()));
            }
            final Path batchDir = resolveExportsRoot().resolve(
                    "batch-" + LocalDateTime.now().format(FILE_TS));
            try {
                Files.createDirectories(batchDir);
            } catch (final IOException ex) {
                final var failed = new CompletableFuture<BatchResult>();
                failed.completeExceptionally(new IllegalStateException(
                        "batch dir creation failed: " + ex.getMessage(), ex));
                return failed;
            }
            return runSequentially(rows, batchDir);
        });
    }

    private @NotNull CompletableFuture<BatchResult> runSequentially(
            @NotNull List<QuestsPlayer> rows, @NotNull Path batchDir
    ) {
        final java.util.List<String> failures = new java.util.concurrent.CopyOnWriteArrayList<>();
        final int[] successCounter = {0};
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (final QuestsPlayer row : rows) {
            final UUID uuid = row.getUniqueId();
            chain = chain.thenCompose(ignored -> buildSnapshotAsync(uuid)
                    .thenAccept(snapshot -> writeSnapshotTo(snapshot, batchDir))
                    .thenAccept(v -> successCounter[0]++)
                    .exceptionally(ex -> {
                        this.logger.warn("batch export failed for {}: {}", uuid, ex.getMessage());
                        failures.add(uuid + " → " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
                        return null;
                    }));
        }
        return chain.thenApply(v -> new BatchResult(
                batchDir, successCounter[0], failures.size(), List.copyOf(failures)));
    }

    private void writeSnapshotTo(@NotNull PlayerSnapshot snapshot, @NotNull Path batchDir) {
        try {
            final String filename = snapshot.playerUuid()
                    + "_" + snapshot.exportedAt().format(FILE_TS) + ".json";
            final Path target = batchDir.resolve(filename);
            this.mapper.writeValue(target.toFile(), snapshot);
        } catch (final IOException ex) {
            throw new IllegalStateException("batch write failed: " + ex.getMessage(), ex);
        }
    }

    private @NotNull Path resolveExportsRoot() {
        return this.quests.getPlugin().getDataFolder().toPath().resolve(EXPORT_DIR);
    }

    /** Builds the snapshot without writing — useful for tests / API callers. */
    public @NotNull CompletableFuture<PlayerSnapshot> buildSnapshotAsync(@NotNull UUID playerUuid) {
        final var questsPlayerFuture = this.quests.questsPlayerService().findAsync(playerUuid);
        final var questProgressFuture = this.quests.questService().questProgress().findByPlayerAsync(playerUuid);
        final var rankFuture = this.quests.rankService().playerRanks(playerUuid);
        final var perkFuture = this.quests.perkService().ownedAsync(playerUuid);

        return CompletableFuture.allOf(questsPlayerFuture, questProgressFuture, rankFuture, perkFuture)
                .thenCompose(ignored -> {
                    final var profile = questsPlayerFuture.join();
                    final var questRows = questProgressFuture.join();
                    final var rankRows = rankFuture.join();
                    final var perkRows = perkFuture.join();

                    // Task rows are per-quest; collect across the player's quest set.
                    final var taskFutures = questRows.stream()
                            .map(row -> this.quests.questService().taskProgressAsync(
                                    playerUuid, row.getQuestIdentifier()))
                            .toList();
                    return CompletableFuture.allOf(taskFutures.toArray(new CompletableFuture[0]))
                            .thenApply(v -> {
                                final java.util.List<PlayerTaskProgress> tasks = new java.util.ArrayList<>();
                                for (final var f : taskFutures) tasks.addAll(f.join());
                                return assemble(playerUuid, profile.orElse(null), questRows, tasks, rankRows, perkRows);
                            });
                });
    }

    private @NotNull PlayerSnapshot assemble(
            @NotNull UUID playerUuid,
            QuestsPlayer profile,
            @NotNull List<PlayerQuestProgress> questRows,
            @NotNull List<PlayerTaskProgress> taskRows,
            @NotNull List<PlayerRank> rankRows,
            @NotNull List<PlayerPerk> perkRows
    ) {
        final String playerName = resolveName(playerUuid);
        final PlayerSnapshot.Profile profileRecord = new PlayerSnapshot.Profile(
                profile != null && profile.isQuestSidebarEnabled(),
                profile != null && profile.isPerkSidebarEnabled(),
                profile != null ? profile.getTrackedQuestIdentifier() : null,
                profile != null ? profile.getActiveRankTree() : null
        );
        final PlayerSnapshot unsigned = new PlayerSnapshot(
                PlayerSnapshot.CURRENT_SCHEMA,
                LocalDateTime.now(),
                playerUuid,
                playerName,
                "JExQuests",
                resolveServerId(),
                null, // integrityHash filled after canonical serialization
                profileRecord,
                questRows.stream().map(PlayerStateExporter::questProgressOf).toList(),
                taskRows.stream().map(PlayerStateExporter::taskProgressOf).toList(),
                rankRows.stream().map(PlayerStateExporter::rankStateOf).toList(),
                perkRows.stream().map(PlayerStateExporter::perkOwnershipOf).toList()
        );
        return unsigned.withIntegrityHash(SnapshotIntegrity.hash(unsigned, this.mapper));
    }

    /**
     * Resolves the {@code server.id} config value — stamped into
     * every snapshot so the receiving shard can tell where an import
     * came from. Empty string when the operator hasn't configured one.
     */
    private @NotNull String resolveServerId() {
        final String raw = this.quests.getPlugin().getConfig().getString("server.id", "");
        return raw != null ? raw : "";
    }

    private @NotNull Path writeSnapshot(@NotNull PlayerSnapshot snapshot) {
        try {
            final Path dir = this.quests.getPlugin().getDataFolder().toPath().resolve(EXPORT_DIR);
            Files.createDirectories(dir);
            final String filename = snapshot.playerUuid()
                    + "_" + snapshot.exportedAt().format(FILE_TS) + ".json";
            final Path target = dir.resolve(filename);
            this.mapper.writeValue(target.toFile(), snapshot);
            this.logger.info("Exported player {} → {}", snapshot.playerUuid(), target);
            return target;
        } catch (final IOException ex) {
            throw new IllegalStateException("export write failed: " + ex.getMessage(), ex);
        }
    }

    private static @NotNull String resolveName(@NotNull UUID uuid) {
        final var offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() != null ? offline.getName() : uuid.toString();
    }

    private static @NotNull PlayerSnapshot.QuestProgress questProgressOf(@NotNull PlayerQuestProgress row) {
        return new PlayerSnapshot.QuestProgress(
                row.getQuestIdentifier(),
                row.getStatus().name(),
                row.getCurrentTaskIndex(),
                row.getCompletionCount(),
                row.getStartedAt(),
                row.getCompletedAt(),
                row.getExpiresAt(),
                row.getLastUpdatedAt()
        );
    }

    private static @NotNull PlayerSnapshot.TaskProgress taskProgressOf(@NotNull PlayerTaskProgress row) {
        return new PlayerSnapshot.TaskProgress(
                row.getQuestIdentifier(),
                row.getTaskIdentifier(),
                row.getProgress(),
                row.getTarget(),
                row.isCompleted(),
                row.getLastUpdatedAt()
        );
    }

    private static @NotNull PlayerSnapshot.RankState rankStateOf(@NotNull PlayerRank row) {
        return new PlayerSnapshot.RankState(
                row.getTreeIdentifier(),
                row.getCurrentRankIdentifier(),
                row.getPromotedAt(),
                row.getProgressionPercent(),
                row.isTreeCompleted(),
                row.getTreeCompletedAt()
        );
    }

    /**
     * Outcome of a {@link #exportAllAsync()} sweep.
     *
     * @param outputDirectory root folder every snapshot was written into
     * @param successCount number of players exported without error
     * @param failureCount number of players whose export failed
     * @param failures per-player failure lines ({@code "<uuid> → <reason>"}) —
     *                 empty when the batch was fully successful
     */
    public record BatchResult(
            @NotNull Path outputDirectory,
            int successCount,
            int failureCount,
            @NotNull List<String> failures
    ) {
        public int totalCount() {
            return this.successCount + this.failureCount;
        }
    }

    private static @NotNull PlayerSnapshot.PerkOwnership perkOwnershipOf(@NotNull PlayerPerk row) {
        return new PlayerSnapshot.PerkOwnership(
                row.getPerkIdentifier(),
                row.isEnabled(),
                row.getUnlockedAt(),
                row.getLastActivatedAt(),
                row.getActivationCount()
        );
    }
}
