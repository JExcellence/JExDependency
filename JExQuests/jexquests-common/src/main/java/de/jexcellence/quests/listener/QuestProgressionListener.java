package de.jexcellence.quests.listener;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.api.QuestObjective;
import de.jexcellence.quests.database.entity.PlayerQuestProgress;
import de.jexcellence.quests.database.entity.Quest;
import de.jexcellence.quests.database.entity.QuestTask;
import de.jexcellence.quests.service.QuestObjectiveCodec;
import de.jexcellence.quests.service.QuestService;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Drives quest progression from vanilla gameplay events. For each
 * relevant event the listener enumerates the player's active quests,
 * resolves each task's objective, and calls
 * {@link QuestService#incrementTaskAsync} when the event matches.
 *
 * <p>Supported objective kinds:
 * {@link QuestObjective.BlockBreak}, {@link QuestObjective.BlockPlace},
 * {@link QuestObjective.EntityKill}, {@link QuestObjective.ItemPickup},
 * {@link QuestObjective.ItemCraft}. {@link QuestObjective.Custom}
 * kinds are ignored — downstream plugins listen for their own events.
 */
public final class QuestProgressionListener implements Listener {

    private final JExQuests quests;
    private final QuestService questService;
    private final JExLogger logger;

    public QuestProgressionListener(@NotNull JExQuests quests) {
        this.quests = quests;
        this.questService = quests.questService();
        this.logger = quests.logger();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        final Material material = event.getBlock().getType();
        dispatch(event.getPlayer().getUniqueId(), objective ->
                objective instanceof QuestObjective.BlockBreak b && materialMatches(b.material(), material));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        final Material material = event.getBlockPlaced().getType();
        dispatch(event.getPlayer().getUniqueId(), objective ->
                objective instanceof QuestObjective.BlockPlace p && materialMatches(p.material(), material));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(@NotNull EntityDeathEvent event) {
        final Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        final EntityType type = event.getEntityType();
        this.logger.info("[progress] {} killed {} — dispatching", killer.getName(), type);
        dispatch(killer.getUniqueId(), objective ->
                objective instanceof QuestObjective.EntityKill k && entityMatches(k.entityType(), type));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(@NotNull EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        final Material material = event.getItem().getItemStack().getType();
        dispatch(player.getUniqueId(), objective ->
                objective instanceof QuestObjective.ItemPickup p && materialMatches(p.material(), material));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(@NotNull CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        final Material material = event.getRecipe().getResult().getType();
        final long crafted = computeCraftCount(event);
        dispatchWithDelta(player.getUniqueId(), crafted, objective ->
                objective instanceof QuestObjective.ItemCraft c && materialMatches(c.material(), material));
    }

    /**
     * Compute how many items a single {@link CraftItemEvent} actually
     * produces. Bukkit fires one event per click regardless of whether
     * the player used a single click (one recipe output) or a
     * shift-click (fills the player inventory from the recipe matrix).
     * Without this, shift-clicking 64 bread counted as one craft —
     * which is how {@code craft 16 bread} tasks stayed stuck at 1/16.
     *
     * <p>Algorithm:
     * <ul>
     *   <li>Non-shift click → result stack amount (usually 1)</li>
     *   <li>Shift click → {@code min(non-zero ingredient stack sizes) × result amount}.
     *       This matches vanilla behaviour: the shift-craft loop stops
     *       when any one ingredient runs out.</li>
     * </ul>
     */
    private static long computeCraftCount(@NotNull CraftItemEvent event) {
        final int resultAmount = Math.max(1, event.getRecipe().getResult().getAmount());
        if (!event.isShiftClick()) return resultAmount;

        int minIngredient = Integer.MAX_VALUE;
        for (final org.bukkit.inventory.ItemStack slot : event.getInventory().getMatrix()) {
            if (slot == null || slot.getType().isAir() || slot.getAmount() <= 0) continue;
            if (slot.getAmount() < minIngredient) minIngredient = slot.getAmount();
        }
        if (minIngredient == Integer.MAX_VALUE) return resultAmount;
        return (long) minIngredient * resultAmount;
    }

    /**
     * Shared dispatch — one unit of progress per event. Covers
     * entity-kill, block-break, block-place, item-pickup, player-join —
     * each fires exactly once per occurrence and should count as 1.
     */
    private void dispatch(@NotNull UUID playerUuid, @NotNull ObjectiveMatcher matcher) {
        dispatchWithDelta(playerUuid, 1L, matcher);
    }

    /**
     * Custom-delta dispatch — used by events where a single Bukkit event
     * actually represents multiple progress units (notably shift-click
     * crafting, where one {@link CraftItemEvent} produces up to 64× the
     * recipe output). The delta flows into {@code incrementTaskAsync}
     * so the task counter jumps by the real amount.
     */
    private void dispatchWithDelta(@NotNull UUID playerUuid, long delta, @NotNull ObjectiveMatcher matcher) {
        if (delta <= 0) return;
        this.questService.activeForPlayerAsync(playerUuid).thenAccept(active -> {
            this.logger.info("[progress] {} active quests for {} (delta={})", active.size(), playerUuid, delta);
            if (active.isEmpty()) return;
            for (final PlayerQuestProgress progress : active) {
                matchAndIncrement(playerUuid, progress, matcher, delta);
            }
        }).exceptionally(ex -> {
            this.logger.error("[progress] dispatch failed for {}: {}", playerUuid, ex.toString());
            return null;
        });
    }

    private void matchAndIncrement(
            @NotNull UUID playerUuid,
            @NotNull PlayerQuestProgress progress,
            @NotNull ObjectiveMatcher matcher,
            long delta
    ) {
        this.questService.quests()
                .findByIdentifierAsync(progress.getQuestIdentifier())
                .thenCompose(optQuest -> {
                    if (optQuest.isEmpty()) {
                        this.logger.warn("[progress] quest {} not found for progress row", progress.getQuestIdentifier());
                        return CompletableFuture.completedFuture(java.util.List.<QuestTask>of());
                    }
                    return loadTasks(optQuest.get());
                })
                .thenAccept(tasks -> {
                    if (tasks.isEmpty()) return;
                    for (final QuestTask task : tasks) {
                        final QuestObjective objective = decodeObjective(task.getObjectiveData(), task.getTaskIdentifier());
                        if (objective == null) {
                            this.logger.warn("[progress] task {} has no objective data or failed to decode",
                                    task.getTaskIdentifier());
                            continue;
                        }
                        if (!matcher.matches(objective)) continue;
                        this.logger.info("[progress] MATCH on {}/{} += {}",
                                progress.getQuestIdentifier(), task.getTaskIdentifier(), delta);
                        this.questService.incrementTaskAsync(
                                        playerUuid, progress.getQuestIdentifier(), task.getTaskIdentifier(), delta)
                                .thenAccept(incremented -> this.logger.info(
                                        "[progress] increment {}/{} → justCompleted={}",
                                        progress.getQuestIdentifier(), task.getTaskIdentifier(), incremented))
                                .exceptionally(ex -> {
                                    this.logger.error("[progress] increment {}/{} failed: {}",
                                            progress.getQuestIdentifier(), task.getTaskIdentifier(), ex.toString());
                                    return null;
                                });
                        return;
                    }
                })
                .exceptionally(ex -> {
                    this.logger.error("[progress] matchAndIncrement failed for {}/{}: {}",
                            playerUuid, progress.getQuestIdentifier(), ex.toString());
                    return null;
                });
    }

    private @NotNull CompletableFuture<java.util.List<QuestTask>> loadTasks(@NotNull Quest quest) {
        return this.quests.questService().tasks().findByQuestAsync(quest);
    }

    private QuestObjective decodeObjective(String json, @NotNull String taskId) {
        try {
            return QuestObjectiveCodec.decode(json);
        } catch (final RuntimeException ex) {
            this.logger.warn("skipping task {}: {}", taskId, ex.getMessage());
            return null;
        }
    }

    private static boolean materialMatches(@NotNull String key, @NotNull Material material) {
        if (key.equalsIgnoreCase("*") || key.equalsIgnoreCase("any")) return true;
        return material.name().equalsIgnoreCase(key);
    }

    private static boolean entityMatches(@NotNull String key, @NotNull EntityType type) {
        if (key.equalsIgnoreCase("*") || key.equalsIgnoreCase("any")) return true;
        return type.name().equalsIgnoreCase(key);
    }

    @FunctionalInterface
    private interface ObjectiveMatcher {
        boolean matches(@NotNull QuestObjective objective);
    }
}
