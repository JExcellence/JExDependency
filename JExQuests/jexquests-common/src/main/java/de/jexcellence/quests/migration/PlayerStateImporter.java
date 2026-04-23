package de.jexcellence.quests.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.api.PlayerSnapshot;
import de.jexcellence.quests.database.entity.PlayerPerk;
import de.jexcellence.quests.database.entity.PlayerQuestProgress;
import de.jexcellence.quests.database.entity.PlayerRank;
import de.jexcellence.quests.database.entity.PlayerTaskProgress;
import de.jexcellence.quests.database.entity.QuestStatus;
import de.jexcellence.quests.database.entity.QuestsPlayer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Loads a {@link PlayerSnapshot} from disk and upserts every row into
 * the player's database state. Idempotent — running the same import
 * twice produces the same end state.
 *
 * <p>Supports <b>dry-run</b> mode: computes the diff between the
 * snapshot and the live database without writing anything, so admins
 * can preview what a production import will change before committing.
 *
 * <p>Upsert semantics per row family:
 * <ul>
 *   <li><b>QuestsPlayer</b> — find-or-create by uuid, overwrite profile fields</li>
 *   <li><b>PlayerQuestProgress</b> — find-by-(uuid, questIdentifier), overwrite</li>
 *   <li><b>PlayerTaskProgress</b> — find-by-(uuid, quest, task), overwrite</li>
 *   <li><b>PlayerRank</b> — find-by-(uuid, tree), overwrite</li>
 *   <li><b>PlayerPerk</b> — find-by-(uuid, perk), overwrite</li>
 * </ul>
 *
 * <p>Schema version is verified — unknown versions fail fast with a
 * clear error so forward-incompatible imports don't corrupt state.
 */
public final class PlayerStateImporter {

    private final JExQuests quests;
    private final JExLogger logger;
    private final ObjectMapper mapper;

    public PlayerStateImporter(@NotNull JExQuests quests) {
        this.quests = quests;
        this.logger = quests.logger();
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /** Reads + applies. Returns the parsed snapshot's player UUID on success. */
    public @NotNull CompletableFuture<UUID> importAsync(@NotNull Path source) {
        return CompletableFuture.supplyAsync(() -> readSnapshot(source))
                .thenCompose(this::applyAsync);
    }

    /**
     * Reads + diffs against live state <b>without</b> writing. Returns
     * a {@link ImportPreview} listing every row that would be created,
     * updated, or left unchanged.
     */
    public @NotNull CompletableFuture<ImportPreview> dryRunAsync(@NotNull Path source) {
        return CompletableFuture.supplyAsync(() -> readSnapshot(source)).thenCompose(this::previewAsync);
    }

    /**
     * Bulk import — imports every {@code *.json} file in the given
     * folder sequentially. Mirrors
     * {@link PlayerStateExporter#exportAllAsync()} on the inbound
     * side: per-file failures are isolated and reported in the
     * returned {@link BatchResult} rather than aborting the sweep.
     */
    public @NotNull CompletableFuture<BatchResult> importAllAsync(@NotNull Path folder) {
        return CompletableFuture.supplyAsync(() -> collectSnapshotFiles(folder))
                .thenCompose(files -> {
                    if (files.isEmpty()) {
                        return CompletableFuture.completedFuture(new BatchResult(folder, 0, 0, List.of()));
                    }
                    return runImportChain(files, folder);
                });
    }

    private @NotNull List<Path> collectSnapshotFiles(@NotNull Path folder) {
        if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException("not a directory: " + folder);
        }
        try (Stream<Path> stream = Files.list(folder)) {
            return stream
                    .filter(path -> Files.isRegularFile(path))
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted()
                    .toList();
        } catch (final IOException ex) {
            throw new IllegalStateException("failed to list " + folder + ": " + ex.getMessage(), ex);
        }
    }

    private @NotNull CompletableFuture<BatchResult> runImportChain(@NotNull List<Path> files, @NotNull Path folder) {
        final java.util.List<String> failures = new java.util.concurrent.CopyOnWriteArrayList<>();
        final int[] successCounter = {0};
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (final Path file : files) {
            chain = chain.thenCompose(ignored -> importAsync(file)
                    .thenAccept(uuid -> successCounter[0]++)
                    .exceptionally(ex -> {
                        this.logger.warn("batch import failed for {}: {}", file.getFileName(), ex.getMessage());
                        failures.add(file.getFileName() + " → "
                                + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
                        return null;
                    }));
        }
        return chain.thenApply(v -> new BatchResult(folder, successCounter[0], failures.size(), List.copyOf(failures)));
    }

    public @NotNull CompletableFuture<UUID> applyAsync(@NotNull PlayerSnapshot snapshot) {
        final var gate = verifySchema(snapshot, UUID.class);
        if (gate != null) return gate;
        return CompletableFuture.runAsync(() -> apply(snapshot))
                .thenApply(ignored -> snapshot.playerUuid())
                .exceptionally(ex -> {
                    this.logger.error("import failed for {}: {}", snapshot.playerUuid(), ex.getMessage());
                    throw new IllegalStateException("import failed: " + ex.getMessage(), ex);
                });
    }

    public @NotNull CompletableFuture<ImportPreview> previewAsync(@NotNull PlayerSnapshot snapshot) {
        final var gate = verifySchema(snapshot, ImportPreview.class);
        if (gate != null) return gate;
        return CompletableFuture.supplyAsync(() -> preview(snapshot))
                .exceptionally(ex -> {
                    this.logger.error("preview failed for {}: {}", snapshot.playerUuid(), ex.getMessage());
                    throw new IllegalStateException("preview failed: " + ex.getMessage(), ex);
                });
    }

    private <T> CompletableFuture<T> verifySchema(@NotNull PlayerSnapshot snapshot, @NotNull Class<T> kind) {
        if (snapshot.schemaVersion() > PlayerSnapshot.CURRENT_SCHEMA) {
            final var failed = new CompletableFuture<T>();
            failed.completeExceptionally(new IllegalArgumentException(
                    "snapshot schema " + snapshot.schemaVersion()
                            + " is newer than this plugin supports (" + PlayerSnapshot.CURRENT_SCHEMA
                            + ") — upgrade JExQuests before importing"));
            return failed;
        }
        if (snapshot.schemaVersion() < PlayerSnapshot.CURRENT_SCHEMA) {
            this.logger.info("importing legacy schema v{} snapshot (current is v{}) — older fields left null",
                    snapshot.schemaVersion(), PlayerSnapshot.CURRENT_SCHEMA);
        }
        if (!SnapshotIntegrity.verify(snapshot, this.mapper)) {
            final var failed = new CompletableFuture<T>();
            failed.completeExceptionally(new IllegalStateException(
                    "snapshot integrity hash mismatch — the file was modified or corrupted since export"));
            return failed;
        }
        return null;
    }

    private void apply(@NotNull PlayerSnapshot snapshot) {
        applyProfile(snapshot);
        applyQuests(snapshot);
        applyTasks(snapshot);
        applyRanks(snapshot);
        applyPerks(snapshot);
        this.logger.info("Imported player {} ({} quests, {} tasks, {} ranks, {} perks)",
                snapshot.playerUuid(),
                snapshot.questProgress().size(),
                snapshot.taskProgress().size(),
                snapshot.ranks().size(),
                snapshot.perks().size());
    }

    private @NotNull ImportPreview preview(@NotNull PlayerSnapshot snapshot) {
        final List<String> changes = new ArrayList<>();
        final int[] counts = new int[]{0, 0, 0}; // created, updated, unchanged

        previewProfile(snapshot, changes, counts);
        previewQuests(snapshot, changes, counts);
        previewTasks(snapshot, changes, counts);
        previewRanks(snapshot, changes, counts);
        previewPerks(snapshot, changes, counts);

        return new ImportPreview(
                snapshot.playerUuid(), snapshot.playerName(),
                counts[0], counts[1], counts[2], List.copyOf(changes));
    }

    private void applyProfile(@NotNull PlayerSnapshot snapshot) {
        final var row = this.quests.questsPlayerService().repository().findOrCreate(snapshot.playerUuid());
        row.setQuestSidebarEnabled(snapshot.profile().questSidebarEnabled());
        row.setPerkSidebarEnabled(snapshot.profile().perkSidebarEnabled());
        row.setTrackedQuestIdentifier(snapshot.profile().trackedQuestIdentifier());
        row.setActiveRankTree(snapshot.profile().activeRankTree());
        this.quests.questsPlayerService().repository().update(row);
    }

    private void previewProfile(@NotNull PlayerSnapshot snapshot, @NotNull List<String> changes, int[] counts) {
        final QuestsPlayer existing = this.quests.questsPlayerService()
                .findAsync(snapshot.playerUuid()).join().orElse(null);
        if (existing == null) {
            counts[0]++;
            changes.add("+ profile (new)");
            return;
        }
        final boolean differs =
                existing.isQuestSidebarEnabled() != snapshot.profile().questSidebarEnabled()
                        || existing.isPerkSidebarEnabled() != snapshot.profile().perkSidebarEnabled()
                        || !Objects.equals(existing.getTrackedQuestIdentifier(), snapshot.profile().trackedQuestIdentifier())
                        || !Objects.equals(existing.getActiveRankTree(), snapshot.profile().activeRankTree());
        if (differs) {
            counts[1]++;
            changes.add("~ profile");
        } else {
            counts[2]++;
        }
    }

    private void applyQuests(@NotNull PlayerSnapshot snapshot) {
        final var repo = this.quests.questService().questProgress();
        for (final PlayerSnapshot.QuestProgress qp : snapshot.questProgress()) {
            final PlayerQuestProgress existing = repo.findAsync(snapshot.playerUuid(), qp.questIdentifier())
                    .join().orElse(null);
            final PlayerQuestProgress row = existing != null
                    ? existing
                    : new PlayerQuestProgress(snapshot.playerUuid(), qp.questIdentifier());
            row.setStatus(parseQuestStatus(qp.status()));
            row.setCurrentTaskIndex(qp.currentTaskIndex());
            row.setCompletionCount(qp.completionCount());
            row.setStartedAt(qp.startedAt());
            row.setCompletedAt(qp.completedAt());
            row.setExpiresAt(qp.expiresAt());
            row.setLastUpdatedAt(qp.lastUpdatedAt() != null ? qp.lastUpdatedAt() : LocalDateTime.now());
            if (existing != null) repo.update(row);
            else repo.create(row);
        }
    }

    private void previewQuests(@NotNull PlayerSnapshot snapshot, @NotNull List<String> changes, int[] counts) {
        final var repo = this.quests.questService().questProgress();
        for (final PlayerSnapshot.QuestProgress qp : snapshot.questProgress()) {
            final PlayerQuestProgress existing = repo.findAsync(snapshot.playerUuid(), qp.questIdentifier())
                    .join().orElse(null);
            if (existing == null) {
                counts[0]++;
                changes.add("+ quest " + qp.questIdentifier() + " (" + qp.status() + ")");
                continue;
            }
            final QuestStatus incoming = parseQuestStatus(qp.status());
            final boolean differs = existing.getStatus() != incoming
                    || existing.getCurrentTaskIndex() != qp.currentTaskIndex()
                    || existing.getCompletionCount() != qp.completionCount();
            if (differs) {
                counts[1]++;
                changes.add("~ quest " + qp.questIdentifier()
                        + " (" + existing.getStatus() + " → " + incoming + ")");
            } else {
                counts[2]++;
            }
        }
    }

    private void applyTasks(@NotNull PlayerSnapshot snapshot) {
        final var repo = this.quests.questService().taskProgress();
        for (final PlayerSnapshot.TaskProgress tp : snapshot.taskProgress()) {
            final PlayerTaskProgress existing = repo.findAsync(
                    snapshot.playerUuid(), tp.questIdentifier(), tp.taskIdentifier()).join().orElse(null);
            final PlayerTaskProgress row = existing != null ? existing : new PlayerTaskProgress(
                    snapshot.playerUuid(), tp.questIdentifier(), tp.taskIdentifier(), tp.target());
            row.setProgress(tp.progress());
            row.setTarget(tp.target());
            row.setCompleted(tp.completed());
            row.setLastUpdatedAt(tp.lastUpdatedAt() != null ? tp.lastUpdatedAt() : LocalDateTime.now());
            if (existing != null) repo.update(row);
            else repo.create(row);
        }
    }

    private void previewTasks(@NotNull PlayerSnapshot snapshot, @NotNull List<String> changes, int[] counts) {
        final var repo = this.quests.questService().taskProgress();
        for (final PlayerSnapshot.TaskProgress tp : snapshot.taskProgress()) {
            final PlayerTaskProgress existing = repo.findAsync(
                    snapshot.playerUuid(), tp.questIdentifier(), tp.taskIdentifier()).join().orElse(null);
            if (existing == null) {
                counts[0]++;
                changes.add("+ task " + tp.questIdentifier() + "/" + tp.taskIdentifier()
                        + " (" + tp.progress() + "/" + tp.target() + ")");
                continue;
            }
            final boolean differs = existing.getProgress() != tp.progress()
                    || existing.getTarget() != tp.target()
                    || existing.isCompleted() != tp.completed();
            if (differs) {
                counts[1]++;
                changes.add("~ task " + tp.questIdentifier() + "/" + tp.taskIdentifier()
                        + " (" + existing.getProgress() + " → " + tp.progress() + ")");
            } else {
                counts[2]++;
            }
        }
    }

    private void applyRanks(@NotNull PlayerSnapshot snapshot) {
        final var repo = this.quests.rankService().playerRankRepository();
        for (final PlayerSnapshot.RankState rs : snapshot.ranks()) {
            final PlayerRank existing = repo.findAsync(snapshot.playerUuid(), rs.treeIdentifier())
                    .join().orElse(null);
            final PlayerRank row = existing != null ? existing : new PlayerRank(
                    snapshot.playerUuid(), rs.treeIdentifier(), rs.currentRankIdentifier());
            row.setCurrentRankIdentifier(rs.currentRankIdentifier());
            row.setPromotedAt(rs.promotedAt());
            row.setProgressionPercent(rs.progressionPercent());
            row.setTreeCompleted(rs.treeCompleted());
            row.setTreeCompletedAt(rs.treeCompletedAt());
            if (existing != null) repo.update(row);
            else repo.create(row);
        }
    }

    private void previewRanks(@NotNull PlayerSnapshot snapshot, @NotNull List<String> changes, int[] counts) {
        final var repo = this.quests.rankService().playerRankRepository();
        for (final PlayerSnapshot.RankState rs : snapshot.ranks()) {
            final PlayerRank existing = repo.findAsync(snapshot.playerUuid(), rs.treeIdentifier())
                    .join().orElse(null);
            if (existing == null) {
                counts[0]++;
                changes.add("+ rank " + rs.treeIdentifier() + " (" + rs.currentRankIdentifier() + ")");
                continue;
            }
            final boolean differs = !Objects.equals(existing.getCurrentRankIdentifier(), rs.currentRankIdentifier())
                    || existing.getProgressionPercent() != rs.progressionPercent()
                    || existing.isTreeCompleted() != rs.treeCompleted();
            if (differs) {
                counts[1]++;
                changes.add("~ rank " + rs.treeIdentifier()
                        + " (" + existing.getCurrentRankIdentifier() + " → " + rs.currentRankIdentifier() + ")");
            } else {
                counts[2]++;
            }
        }
    }

    private void applyPerks(@NotNull PlayerSnapshot snapshot) {
        final var repo = this.quests.perkService().playerPerks();
        for (final PlayerSnapshot.PerkOwnership po : snapshot.perks()) {
            final PlayerPerk existing = repo.findAsync(snapshot.playerUuid(), po.perkIdentifier())
                    .join().orElse(null);
            final PlayerPerk row = existing != null ? existing : new PlayerPerk(
                    snapshot.playerUuid(), po.perkIdentifier());
            row.setEnabled(po.enabled());
            row.setLastActivatedAt(po.lastActivatedAt());
            row.setActivationCount(po.activationCount());
            if (existing != null) repo.update(row);
            else repo.create(row);
        }
    }

    private void previewPerks(@NotNull PlayerSnapshot snapshot, @NotNull List<String> changes, int[] counts) {
        final var repo = this.quests.perkService().playerPerks();
        for (final PlayerSnapshot.PerkOwnership po : snapshot.perks()) {
            final PlayerPerk existing = repo.findAsync(snapshot.playerUuid(), po.perkIdentifier())
                    .join().orElse(null);
            if (existing == null) {
                counts[0]++;
                changes.add("+ perk " + po.perkIdentifier() + (po.enabled() ? " (on)" : " (off)"));
                continue;
            }
            final boolean differs = existing.isEnabled() != po.enabled()
                    || existing.getActivationCount() != po.activationCount();
            if (differs) {
                counts[1]++;
                changes.add("~ perk " + po.perkIdentifier()
                        + " (" + existing.isEnabled() + " → " + po.enabled() + ")");
            } else {
                counts[2]++;
            }
        }
    }

    private @NotNull PlayerSnapshot readSnapshot(@NotNull Path source) {
        try {
            return this.mapper.readValue(source.toFile(), PlayerSnapshot.class);
        } catch (final IOException ex) {
            throw new IllegalStateException("import read failed: " + ex.getMessage(), ex);
        }
    }

    private static @NotNull QuestStatus parseQuestStatus(@NotNull String raw) {
        try {
            return QuestStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ex) {
            return QuestStatus.AVAILABLE;
        }
    }

    /**
     * Outcome of a {@link #importAllAsync(Path)} sweep.
     *
     * @param folder directory the sweep iterated over
     * @param successCount files imported cleanly
     * @param failureCount files that threw — their state was not written
     * @param failures per-file reason strings ({@code "<filename> → <reason>"})
     */
    public record BatchResult(
            @NotNull Path folder,
            int successCount,
            int failureCount,
            @NotNull List<String> failures
    ) {
        public int totalCount() {
            return this.successCount + this.failureCount;
        }
    }

    /**
     * Read-only diff report — what a real import of this snapshot
     * would change if committed right now. {@code createdCount} and
     * {@code updatedCount} sum across every row family;
     * {@link #changes()} is a flat, human-readable list prefixed with
     * {@code +} (create) or {@code ~} (update).
     */
    public record ImportPreview(
            @NotNull UUID playerUuid,
            @NotNull String playerName,
            int createdCount,
            int updatedCount,
            int unchangedCount,
            @NotNull List<String> changes
    ) {
        /** {@code true} when committing would write nothing. */
        public boolean isNoOp() {
            return this.createdCount == 0 && this.updatedCount == 0;
        }
    }
}
