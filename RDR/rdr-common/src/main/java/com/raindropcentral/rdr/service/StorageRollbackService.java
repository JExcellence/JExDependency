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

package com.raindropcentral.rdr.service;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.configs.ConfigSection;
import com.raindropcentral.rdr.database.entity.RDRPlayer;
import com.raindropcentral.rdr.database.entity.RRollbackSnapshot;
import com.raindropcentral.rdr.database.entity.RRollbackStorageSnapshot;
import com.raindropcentral.rdr.database.entity.RStorage;
import com.raindropcentral.rdr.database.entity.RollbackTriggerType;
import com.raindropcentral.rdr.database.repository.RRDRPlayer;
import com.raindropcentral.rdr.database.repository.RRRollbackSnapshot;
import com.raindropcentral.rdr.database.repository.RRStorage;
import com.raindropcentral.rdr.view.StorageAdminForceCloseSupport;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Captures, persists, browses, and restores RDR rollback snapshots.
 *
 * <p>This service centralizes the snapshot lifecycle used by automatic triggers, manual admin
 * backups, and admin restore flows so command handlers and views share the same persistence and
 * restore behavior.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageRollbackService {

    /**
     * Snapshot target token representing the player's main inventory set.
     */
    public static final String TARGET_TYPE_PLAYER_SET = "player_set";

    /**
     * Snapshot target token representing one persisted RDR storage.
     */
    public static final String TARGET_TYPE_STORAGE = "storage";

    private final RDR plugin;

    /**
     * Creates the rollback service for the active RDR runtime.
     *
     * @param plugin active plugin runtime
     * @throws NullPointerException if {@code plugin} is {@code null}
     */
    public StorageRollbackService(final @NotNull RDR plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Returns whether rollback is enabled globally in config.
     *
     * @return {@code true} when rollback features are enabled
     */
    public boolean isRollbackEnabled() {
        return this.plugin.getDefaultConfig().isRollbackEnabled();
    }

    /**
     * Returns whether the supplied trigger should currently create snapshots.
     *
     * @param triggerType trigger to evaluate
     * @return {@code true} when snapshots should be created for that trigger
     * @throws NullPointerException if {@code triggerType} is {@code null}
     */
    public boolean isCaptureEnabled(final @NotNull RollbackTriggerType triggerType) {
        final RollbackTriggerType validatedTriggerType = Objects.requireNonNull(triggerType, "triggerType");
        final ConfigSection config = this.plugin.getDefaultConfig();
        if (!config.isRollbackEnabled()) {
            return false;
        }

        return switch (validatedTriggerType) {
            case DEATH -> config.isRollbackDeathTriggerEnabled();
            case JOIN -> config.isRollbackJoinTriggerEnabled();
            case LEAVE -> config.isRollbackLeaveTriggerEnabled();
            case WORLD_CHANGE -> config.isRollbackWorldChangeTriggerEnabled();
            case MANUAL_BACKUP -> config.isRollbackManualBackupTriggerEnabled();
            case PRE_RESTORE_SAFETY -> true;
        };
    }

    /**
     * Captures and persists one rollback snapshot for an online player.
     *
     * @param target target player whose state should be saved
     * @param triggerType trigger producing the snapshot
     * @return future containing {@code true} when the snapshot was saved
     * @throws NullPointerException if any required argument is {@code null}
     */
    public @NotNull CompletableFuture<Boolean> capturePlayerSnapshotAsync(
        final @NotNull Player target,
        final @NotNull RollbackTriggerType triggerType
    ) {
        return this.capturePlayerSnapshotAsync(target, triggerType, null, null, null, target.getWorld().getName());
    }

    /**
     * Captures and persists one rollback snapshot for an online player with explicit metadata.
     *
     * @param target target player whose state should be saved
     * @param triggerType trigger producing the snapshot
     * @param actorUuid actor UUID for admin-driven actions, or {@code null} for automatic snapshots
     * @param actorName actor name for admin-driven actions, or {@code null} for automatic snapshots
     * @param fromWorldName previous world name, or {@code null} when not applicable
     * @param toWorldName current/destination world name, or {@code null} when not applicable
     * @return future containing {@code true} when the snapshot was saved
     * @throws NullPointerException if {@code target} or {@code triggerType} is {@code null}
     */
    public @NotNull CompletableFuture<Boolean> capturePlayerSnapshotAsync(
        final @NotNull Player target,
        final @NotNull RollbackTriggerType triggerType,
        final @Nullable UUID actorUuid,
        final @Nullable String actorName,
        final @Nullable String fromWorldName,
        final @Nullable String toWorldName
    ) {
        Objects.requireNonNull(target, "target");
        final RollbackTriggerType validatedTriggerType = Objects.requireNonNull(triggerType, "triggerType");
        if (!this.isCaptureEnabled(validatedTriggerType) || !this.hasRepositories()) {
            return CompletableFuture.completedFuture(false);
        }

        return this.captureCurrentStateAsync(target, validatedTriggerType, actorUuid, actorName, fromWorldName, toWorldName)
            .thenCompose(capturedState -> capturedState == null
                ? CompletableFuture.completedFuture(false)
                : this.supplyAsync(() -> this.persistCapturedState(capturedState)));
    }

    /**
     * Captures snapshots for every currently online player.
     *
     * @param actor admin actor who initiated the backup, or {@code null} when none is available
     * @return future containing a summary of attempted and saved snapshots
     */
    public @NotNull CompletableFuture<ManualBackupResult> backupAllOnlinePlayersAsync(final @Nullable Player actor) {
        if (!this.isCaptureEnabled(RollbackTriggerType.MANUAL_BACKUP) || !this.hasRepositories()) {
            return CompletableFuture.completedFuture(new ManualBackupResult(0, 0));
        }

        final UUID actorUuid = actor == null ? null : actor.getUniqueId();
        final String actorName = actor == null ? null : actor.getName();

        return this.captureOnlineStatesAsync(RollbackTriggerType.MANUAL_BACKUP, actorUuid, actorName)
            .thenCompose(capturedStates -> this.supplyAsync(() -> {
                int savedSnapshots = 0;
                for (final CapturedPlayerState capturedState : capturedStates) {
                    if (this.persistCapturedState(capturedState)) {
                        savedSnapshots++;
                    }
                }

                this.plugin.getLogger().info(
                    "RDR manual backup saved " + savedSnapshots + "/" + capturedStates.size()
                        + " online player snapshot(s)"
                        + (actorName == null ? "" : " by " + actorName + " (" + actorUuid + ")")
                );
                return new ManualBackupResult(capturedStates.size(), savedSnapshots);
            }));
    }

    /**
     * Returns every snapshot for one player ordered newest-first.
     *
     * @param playerUuid target player UUID
     * @return immutable newest-first snapshot list
     * @throws NullPointerException if {@code playerUuid} is {@code null}
     */
    public @NotNull List<RRollbackSnapshot> getPlayerSnapshots(final @NotNull UUID playerUuid) {
        final RRRollbackSnapshot snapshotRepository = this.plugin.getRollbackSnapshotRepository();
        return snapshotRepository == null ? List.of() : snapshotRepository.findByPlayerUuid(playerUuid);
    }

    /**
     * Returns one snapshot with child storage snapshots loaded.
     *
     * @param snapshotId snapshot identifier
     * @return matching snapshot, or {@code null} when none exists
     */
    public @Nullable RRollbackSnapshot getSnapshot(final long snapshotId) {
        final RRRollbackSnapshot snapshotRepository = this.plugin.getRollbackSnapshotRepository();
        return snapshotRepository == null ? null : snapshotRepository.findByIdWithChildren(snapshotId);
    }

    /**
     * Restores one snapshot target for the selected player.
     *
     * @param actor admin actor performing the restore
     * @param snapshotId snapshot identifier to restore from
     * @param targetType target token representing the player set or one storage
     * @param storageSnapshotId storage child identifier when restoring a storage
     * @return future containing the restore result
     * @throws NullPointerException if {@code actor} or {@code targetType} is {@code null}
     */
    public @NotNull CompletableFuture<RestoreResult> restoreSnapshotTargetAsync(
        final @NotNull Player actor,
        final long snapshotId,
        final @NotNull String targetType,
        final @Nullable Long storageSnapshotId
    ) {
        Objects.requireNonNull(actor, "actor");
        final String normalizedTargetType = this.normalizeTargetType(targetType);
        if (normalizedTargetType == null || !this.hasRepositories()) {
            return CompletableFuture.completedFuture(new RestoreResult(RestoreStatus.INVALID_TARGET, null, null, 0));
        }

        return this.supplyAsync(() -> this.getSnapshot(snapshotId))
            .thenCompose(snapshot -> {
                if (snapshot == null) {
                    return CompletableFuture.completedFuture(
                        new RestoreResult(RestoreStatus.SNAPSHOT_MISSING, null, null, 0)
                    );
                }

                final String targetDisplayName = this.resolvePlayerDisplayName(snapshot);
                return this.resolveOnlinePlayerAsync(snapshot.getTargetPlayerUuid())
                    .thenCompose(target -> {
                        if (target == null) {
                            return CompletableFuture.completedFuture(
                                new RestoreResult(RestoreStatus.TARGET_OFFLINE, targetDisplayName, null, 0)
                            );
                        }

                        return this.captureCurrentStateAsync(
                            target,
                            RollbackTriggerType.PRE_RESTORE_SAFETY,
                            actor.getUniqueId(),
                            actor.getName(),
                            null,
                            target.getWorld().getName()
                        ).thenCompose(safetyState -> this.supplyAsync(() -> {
                            if (safetyState == null || !this.persistCapturedState(safetyState)) {
                                return new RestoreResult(RestoreStatus.SAFETY_SNAPSHOT_FAILED, targetDisplayName, null, 0);
                            }
                            return null;
                        })).thenCompose(safetyFailure -> {
                            if (safetyFailure != null) {
                                return CompletableFuture.completedFuture(safetyFailure);
                            }

                            if (TARGET_TYPE_PLAYER_SET.equals(normalizedTargetType)) {
                                return this.runSync(() -> this.restorePlayerSet(target, snapshot))
                                    .thenApply(closedViews -> {
                                        this.logRestore(actor, snapshot, normalizedTargetType, null, RestoreStatus.SUCCESS);
                                        return new RestoreResult(RestoreStatus.SUCCESS, targetDisplayName, null, closedViews);
                                    });
                            }

                            final RRollbackStorageSnapshot storageSnapshot = this.findStorageSnapshot(snapshot, storageSnapshotId);
                            if (storageSnapshot == null) {
                                return CompletableFuture.completedFuture(
                                    new RestoreResult(RestoreStatus.STORAGE_SNAPSHOT_MISSING, targetDisplayName, null, 0)
                                );
                            }

                            return this.supplyAsync(() -> this.resolveLiveStorage(snapshot, storageSnapshot))
                                .thenCompose(liveStorage -> {
                                    if (liveStorage == null || liveStorage.getId() == null) {
                                        this.logRestore(
                                            actor,
                                            snapshot,
                                            normalizedTargetType,
                                            storageSnapshot.getStorageKey(),
                                            RestoreStatus.STORAGE_MISSING
                                        );
                                        return CompletableFuture.completedFuture(
                                            new RestoreResult(
                                                RestoreStatus.STORAGE_MISSING,
                                                targetDisplayName,
                                                storageSnapshot.getStorageKey(),
                                                0
                                            )
                                        );
                                    }

                                    return this.runSync(() -> {
                                        final int closedViews = StorageAdminForceCloseSupport.closeStorageViews(
                                            this.plugin,
                                            liveStorage.getId()
                                        );
                                        target.closeInventory();
                                        return closedViews;
                                    }).thenCompose(closedViews -> this.supplyAsync(() -> {
                                        final RRStorage storageRepository = this.plugin.getStorageRepository();
                                        storageSnapshot.applyTo(liveStorage);
                                        storageRepository.update(liveStorage);
                                        this.logRestore(
                                            actor,
                                            snapshot,
                                            normalizedTargetType,
                                            storageSnapshot.getStorageKey(),
                                            RestoreStatus.SUCCESS
                                        );
                                        return new RestoreResult(
                                            RestoreStatus.SUCCESS,
                                            targetDisplayName,
                                            storageSnapshot.getStorageKey(),
                                            closedViews
                                        );
                                    }));
                                });
                        });
                    });
            });
    }

    private boolean persistCapturedState(final @NotNull CapturedPlayerState capturedState) {
        final RRDRPlayer playerRepository = this.plugin.getPlayerRepository();
        final RRRollbackSnapshot snapshotRepository = this.plugin.getRollbackSnapshotRepository();
        final RRStorage storageRepository = this.plugin.getStorageRepository();
        if (playerRepository == null || snapshotRepository == null || storageRepository == null) {
            return false;
        }

        final RDRPlayer persistedPlayer = this.getOrCreatePlayer(playerRepository, capturedState.playerUuid());
        final RRollbackSnapshot snapshot = new RRollbackSnapshot(
            persistedPlayer,
            capturedState.playerUuid(),
            capturedState.lastKnownPlayerName(),
            capturedState.triggerType(),
            this.plugin.getServerUuid(),
            capturedState.fromWorldName(),
            capturedState.toWorldName()
        );
        snapshot.setActorInfo(capturedState.actorUuid(), capturedState.actorName());
        snapshot.setMainInventory(capturedState.mainInventory());
        snapshot.setArmorInventory(capturedState.armorInventory());
        snapshot.setOffhandItem(capturedState.offhandItem());
        snapshot.setEnderChestInventory(capturedState.enderChestInventory());

        for (final RStorage storage : storageRepository.findByPlayerUuid(capturedState.playerUuid())) {
            new RRollbackStorageSnapshot(
                snapshot,
                storage.getId(),
                storage.getStorageKey(),
                storage.getInventorySize(),
                storage.getInventory(),
                storage.getHotkey(),
                storage.getTrustedPlayers(),
                storage.getTaxDebtEntries()
            );
        }

        snapshotRepository.create(snapshot);
        snapshotRepository.pruneOldestSnapshots(
            capturedState.playerUuid(),
            Math.max(1, this.plugin.getDefaultConfig().getRollbackMaxSnapshotsPerPlayer())
        );
        return true;
    }

    private @NotNull RDRPlayer getOrCreatePlayer(
        final @NotNull RRDRPlayer playerRepository,
        final @NotNull UUID playerUuid
    ) {
        final RDRPlayer existingPlayer = playerRepository.findByPlayer(playerUuid);
        if (existingPlayer != null) {
            return existingPlayer;
        }

        try {
            return playerRepository.create(new RDRPlayer(playerUuid));
        } catch (final RuntimeException exception) {
            final RDRPlayer concurrentPlayer = playerRepository.findByPlayer(playerUuid);
            if (concurrentPlayer != null) {
                return concurrentPlayer;
            }
            throw exception;
        }
    }

    private @Nullable RStorage resolveLiveStorage(
        final @NotNull RRollbackSnapshot snapshot,
        final @NotNull RRollbackStorageSnapshot storageSnapshot
    ) {
        final RRStorage storageRepository = this.plugin.getStorageRepository();
        final Long originalStorageId = storageSnapshot.getOriginalStorageId();
        if (originalStorageId != null) {
            final RStorage storageById = storageRepository.findWithPlayerById(originalStorageId);
            if (storageById != null) {
                return storageById;
            }
        }
        return storageRepository.findByPlayerAndStorageKey(snapshot.getTargetPlayerUuid(), storageSnapshot.getStorageKey());
    }

    private int restorePlayerSet(
        final @NotNull Player target,
        final @NotNull RRollbackSnapshot snapshot
    ) {
        target.closeInventory();

        final PlayerInventory inventory = target.getInventory();
        inventory.setStorageContents(this.toArray(snapshot.getMainInventory(), 36));
        inventory.setArmorContents(this.toArray(snapshot.getArmorInventory(), 4));
        inventory.setItemInOffHand(snapshot.getOffhandItem());

        final Inventory enderChest = target.getEnderChest();
        enderChest.setContents(this.toArray(snapshot.getEnderChestInventory(), enderChest.getSize()));
        target.updateInventory();
        return 1;
    }

    private @Nullable RRollbackStorageSnapshot findStorageSnapshot(
        final @NotNull RRollbackSnapshot snapshot,
        final @Nullable Long storageSnapshotId
    ) {
        if (storageSnapshotId == null) {
            return null;
        }

        for (final RRollbackStorageSnapshot storageSnapshot : snapshot.getStorageSnapshots()) {
            if (Objects.equals(storageSnapshot.getId(), storageSnapshotId)) {
                return storageSnapshot;
            }
        }
        return null;
    }

    private @Nullable String normalizeTargetType(final @Nullable String targetType) {
        if (targetType == null) {
            return null;
        }

        final String normalizedTargetType = targetType.trim().toLowerCase(java.util.Locale.ROOT);
        if (TARGET_TYPE_PLAYER_SET.equals(normalizedTargetType) || TARGET_TYPE_STORAGE.equals(normalizedTargetType)) {
            return normalizedTargetType;
        }
        return null;
    }

    private boolean hasRepositories() {
        return this.plugin.getPlayerRepository() != null
            && this.plugin.getStorageRepository() != null
            && this.plugin.getRollbackSnapshotRepository() != null;
    }

    private @NotNull CompletableFuture<List<CapturedPlayerState>> captureOnlineStatesAsync(
        final @NotNull RollbackTriggerType triggerType,
        final @Nullable UUID actorUuid,
        final @Nullable String actorName
    ) {
        if (Bukkit.isPrimaryThread()) {
            return CompletableFuture.completedFuture(this.captureOnlineStates(triggerType, actorUuid, actorName));
        }
        return this.runSync(() -> this.captureOnlineStates(triggerType, actorUuid, actorName));
    }

    private @NotNull List<CapturedPlayerState> captureOnlineStates(
        final @NotNull RollbackTriggerType triggerType,
        final @Nullable UUID actorUuid,
        final @Nullable String actorName
    ) {
        final List<CapturedPlayerState> capturedStates = new ArrayList<>();
        for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            capturedStates.add(this.captureCurrentState(
                onlinePlayer,
                triggerType,
                actorUuid,
                actorName,
                null,
                onlinePlayer.getWorld().getName()
            ));
        }
        return capturedStates;
    }

    private @NotNull CompletableFuture<@Nullable CapturedPlayerState> captureCurrentStateAsync(
        final @NotNull Player target,
        final @NotNull RollbackTriggerType triggerType,
        final @Nullable UUID actorUuid,
        final @Nullable String actorName,
        final @Nullable String fromWorldName,
        final @Nullable String toWorldName
    ) {
        if (Bukkit.isPrimaryThread()) {
            return CompletableFuture.completedFuture(
                this.captureCurrentState(target, triggerType, actorUuid, actorName, fromWorldName, toWorldName)
            );
        }
        return this.runSync(() -> this.captureCurrentState(target, triggerType, actorUuid, actorName, fromWorldName, toWorldName));
    }

    private @NotNull CapturedPlayerState captureCurrentState(
        final @NotNull Player target,
        final @NotNull RollbackTriggerType triggerType,
        final @Nullable UUID actorUuid,
        final @Nullable String actorName,
        final @Nullable String fromWorldName,
        final @Nullable String toWorldName
    ) {
        final PlayerInventory inventory = target.getInventory();
        return new CapturedPlayerState(
            target.getUniqueId(),
            target.getName(),
            triggerType,
            actorUuid,
            actorName,
            fromWorldName,
            toWorldName,
            this.captureInventory(inventory.getStorageContents()),
            this.captureInventory(inventory.getArmorContents()),
            inventory.getItemInOffHand(),
            this.captureInventory(target.getEnderChest().getContents())
        );
    }

    private @NotNull Map<Integer, ItemStack> captureInventory(final @Nullable ItemStack[] contents) {
        final java.util.Map<Integer, ItemStack> capturedInventory = new java.util.LinkedHashMap<>();
        if (contents == null) {
            return capturedInventory;
        }

        for (int index = 0; index < contents.length; index++) {
            final ItemStack itemStack = contents[index];
            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            capturedInventory.put(index, itemStack.clone());
        }
        return capturedInventory;
    }

    private @NotNull ItemStack[] toArray(
        final @NotNull Map<Integer, ItemStack> contents,
        final int size
    ) {
        final ItemStack[] items = new ItemStack[size];
        for (final Map.Entry<Integer, ItemStack> entry : contents.entrySet()) {
            final Integer slot = entry.getKey();
            final ItemStack itemStack = entry.getValue();
            if (slot == null || slot < 0 || slot >= size || itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            items[slot] = itemStack.clone();
        }
        return items;
    }

    private @NotNull String resolvePlayerDisplayName(final @NotNull RRollbackSnapshot snapshot) {
        final String lastKnownName = snapshot.getLastKnownPlayerName();
        if (lastKnownName != null && !lastKnownName.isBlank()) {
            return lastKnownName;
        }

        final String offlineName = Bukkit.getOfflinePlayer(snapshot.getTargetPlayerUuid()).getName();
        return offlineName == null || offlineName.isBlank()
            ? snapshot.getTargetPlayerUuid().toString()
            : offlineName;
    }

    private void logRestore(
        final @NotNull Player actor,
        final @NotNull RRollbackSnapshot snapshot,
        final @NotNull String targetType,
        final @Nullable String storageKey,
        final @NotNull RestoreStatus status
    ) {
        this.plugin.getLogger().info(
            "RDR restore actor=" + actor.getName() + " (" + actor.getUniqueId() + ")"
                + " target=" + this.resolvePlayerDisplayName(snapshot) + " (" + snapshot.getTargetPlayerUuid() + ")"
                + " snapshotId=" + snapshot.getId()
                + " trigger=" + snapshot.getTriggerType().name()
                + " targetType=" + targetType
                + (storageKey == null ? "" : " storageKey=" + storageKey)
                + " status=" + status.name()
        );
    }

    private @NotNull CompletableFuture<@Nullable Player> resolveOnlinePlayerAsync(final @NotNull UUID playerUuid) {
        if (Bukkit.isPrimaryThread()) {
            return CompletableFuture.completedFuture(Bukkit.getPlayer(playerUuid));
        }
        return this.runSync(() -> Bukkit.getPlayer(playerUuid));
    }

    private <T> @NotNull CompletableFuture<T> runSync(final @NotNull java.util.function.Supplier<T> supplier) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        this.plugin.getScheduler().runSync(() -> {
            try {
                future.complete(supplier.get());
            } catch (final Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    private <T> @NotNull CompletableFuture<T> supplyAsync(final @NotNull java.util.function.Supplier<T> supplier) {
        final Executor executor = this.plugin.getExecutor();
        return executor == null
            ? CompletableFuture.supplyAsync(supplier)
            : CompletableFuture.supplyAsync(supplier, executor);
    }

    /**
     * Summary of one manual all-online backup run.
     *
     * @param attemptedPlayers number of online players captured
     * @param savedSnapshots number of snapshots successfully persisted
     */
    public record ManualBackupResult(
        int attemptedPlayers,
        int savedSnapshots
    ) {
    }

    /**
     * Status of an attempted snapshot restore.
     */
    public enum RestoreStatus {
        SUCCESS,
        SNAPSHOT_MISSING,
        TARGET_OFFLINE,
        INVALID_TARGET,
        STORAGE_SNAPSHOT_MISSING,
        STORAGE_MISSING,
        SAFETY_SNAPSHOT_FAILED
    }

    /**
     * Result of an attempted snapshot restore.
     *
     * @param status restore status
     * @param targetDisplayName best-effort target player display name
     * @param storageKey restored storage key, or {@code null} for player-set restores
     * @param closedViews number of views closed as part of the restore
     */
    public record RestoreResult(
        @NotNull RestoreStatus status,
        @Nullable String targetDisplayName,
        @Nullable String storageKey,
        int closedViews
    ) {
    }

    private record CapturedPlayerState(
        @NotNull UUID playerUuid,
        @NotNull String lastKnownPlayerName,
        @NotNull RollbackTriggerType triggerType,
        @Nullable UUID actorUuid,
        @Nullable String actorName,
        @Nullable String fromWorldName,
        @Nullable String toWorldName,
        @NotNull Map<Integer, ItemStack> mainInventory,
        @NotNull Map<Integer, ItemStack> armorInventory,
        @Nullable ItemStack offhandItem,
        @NotNull Map<Integer, ItemStack> enderChestInventory
    ) {
        private CapturedPlayerState {
            Objects.requireNonNull(playerUuid, "playerUuid");
            Objects.requireNonNull(lastKnownPlayerName, "lastKnownPlayerName");
            Objects.requireNonNull(triggerType, "triggerType");
            Objects.requireNonNull(mainInventory, "mainInventory");
            Objects.requireNonNull(armorInventory, "armorInventory");
            Objects.requireNonNull(enderChestInventory, "enderChestInventory");
            offhandItem = offhandItem == null || offhandItem.isEmpty() ? null : offhandItem.clone();
        }
    }
}
