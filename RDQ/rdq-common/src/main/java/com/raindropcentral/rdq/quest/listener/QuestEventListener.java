package com.raindropcentral.rdq.quest.listener;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.QuestTask;
import com.raindropcentral.rdq.database.entity.quest.QuestUser;
import com.raindropcentral.rdq.cache.quest.QuestCacheManager;
import com.raindropcentral.rdq.service.quest.QuestProgressTracker;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Event listener for tracking quest progress from game events.
 * <p>
 * For each Bukkit event, this listener reads the {@code requirementData} JSON
 * stored on each {@link QuestTask}, matches the event against
 * {@code type} / {@code target} / {@code amount}, and calls
 * {@link QuestProgressTracker#updateProgress} when a task matches.
 * </p>
 *
 * <h1>Supported task types</h1>
 * <ul>
 *   <li>{@code KILL_MOBS} – EntityDeathEvent. Target = entity type or category
 *       ("HOSTILE", "PASSIVE", "NEUTRAL", or a concrete type like "ZOMBIE").</li>
 *   <li>{@code MINE_BLOCKS} / {@code BREAK_BLOCKS} – BlockBreakEvent. Target = block
 *       material name or "ORE" to match any ore.</li>
 *   <li>{@code PLACE_BLOCKS} – BlockPlaceEvent. Target = block material name.</li>
 *   <li>{@code CATCH_FISH} – PlayerFishEvent (CAUGHT_FISH state). Target ignored.</li>
 *   <li>{@code CONSUME_ITEM} – PlayerItemConsumeEvent. Target = item material name.</li>
 * </ul>
 *
 * @author RaindropCentral
 * @version 2.0.0
 */
public class QuestEventListener implements Listener {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    private static final Gson GSON = new Gson();

    private final QuestProgressTracker progressTracker;
    private final QuestCacheManager cacheManager;

    public QuestEventListener(@NotNull final RDQ plugin) {
        this.progressTracker = plugin.getQuestProgressTracker();
        this.cacheManager = plugin.getQuestCacheManager();
    }

    // -------------------------------------------------------------------------
    // KILL_MOBS
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(@NotNull final EntityDeathEvent event) {
        final Player player = event.getEntity().getKiller();
        if (player == null) return;

        final String entityType = event.getEntity().getType().name();
        final boolean isHostile = isHostile(event.getEntity());
        final boolean isPassive = isPassive(event.getEntity());

        processPlayerQuests(player.getUniqueId(), task -> {
            final JsonObject req = parseRequirement(task.getRequirementData());
            if (req == null) return;
            if (!isType(req, "KILL_MOBS")) return;

            final String target = getString(req, "target");
            if (target == null) return;

            final boolean matches = switch (target.toUpperCase()) {
                case "HOSTILE" -> isHostile;
                case "PASSIVE" -> isPassive;
                case "NEUTRAL" -> !isHostile && !isPassive;
                case "ANY"     -> true;
                default        -> target.equalsIgnoreCase(entityType);
            };

            if (matches) {
                updateTask(player.getUniqueId(), task, 1);
            }
        });
    }

    // -------------------------------------------------------------------------
    // MINE_BLOCKS / BREAK_BLOCKS
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(@NotNull final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final String blockType = event.getBlock().getType().name();

        processPlayerQuests(player.getUniqueId(), task -> {
            final JsonObject req = parseRequirement(task.getRequirementData());
            if (req == null) return;
            if (!isType(req, "MINE_BLOCKS") && !isType(req, "BREAK_BLOCKS")) return;

            final String target = getString(req, "target");
            if (target == null) return;

            final boolean matches = target.equalsIgnoreCase("ORE")
                    ? blockType.contains("_ORE")
                    : target.equalsIgnoreCase(blockType);

            if (matches) {
                updateTask(player.getUniqueId(), task, 1);
            }
        });
    }

    // -------------------------------------------------------------------------
    // PLACE_BLOCKS
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(@NotNull final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final String blockType = event.getBlock().getType().name();

        processPlayerQuests(player.getUniqueId(), task -> {
            final JsonObject req = parseRequirement(task.getRequirementData());
            if (req == null) return;
            if (!isType(req, "PLACE_BLOCKS")) return;

            final String target = getString(req, "target");
            if (target == null || !target.equalsIgnoreCase(blockType)) return;

            updateTask(player.getUniqueId(), task, 1);
        });
    }

    // -------------------------------------------------------------------------
    // CATCH_FISH
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(@NotNull final PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        final Player player = event.getPlayer();

        processPlayerQuests(player.getUniqueId(), task -> {
            final JsonObject req = parseRequirement(task.getRequirementData());
            if (req == null) return;
            if (!isType(req, "CATCH_FISH")) return;

            updateTask(player.getUniqueId(), task, 1);
        });
    }

    // -------------------------------------------------------------------------
    // CONSUME_ITEM
    // -------------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemConsume(@NotNull final PlayerItemConsumeEvent event) {
        final Player player = event.getPlayer();
        final String itemType = event.getItem().getType().name();

        processPlayerQuests(player.getUniqueId(), task -> {
            final JsonObject req = parseRequirement(task.getRequirementData());
            if (req == null) return;
            if (!isType(req, "CONSUME_ITEM")) return;

            final String target = getString(req, "target");
            if (target == null || !target.equalsIgnoreCase(itemType)) return;

            updateTask(player.getUniqueId(), task, 1);
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    @FunctionalInterface
    private interface TaskConsumer {
        void accept(QuestTask task);
    }

    /**
     * Iterates all active quests for {@code playerId} and their tasks,
     * calling {@code consumer} for each task.
     */
    private void processPlayerQuests(@NotNull final UUID playerId,
                                     @NotNull final TaskConsumer consumer) {
        try {
            final List<QuestUser> activeQuests = cacheManager.getPlayerQuests(playerId);
            for (final QuestUser questUser : activeQuests) {
                final List<QuestTask> tasks = questUser.getQuest().getTasks();
                for (final QuestTask task : tasks) {
                    try {
                        consumer.accept(task);
                    } catch (final Exception e) {
                        LOGGER.log(Level.FINE, "Error processing task " + task.getTaskIdentifier(), e);
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Error processing quest events for player " + playerId, e);
        }
    }

    private void updateTask(@NotNull final UUID playerId,
                            @NotNull final QuestTask task,
                            final int amount) {
        final String questId = task.getQuest().getIdentifier();
        final String taskId  = task.getTaskIdentifier();

        progressTracker.updateProgress(playerId, questId, taskId, amount)
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING,
                            "Failed to update progress for task " + taskId + " in quest " + questId, ex);
                    return null;
                });
    }

    /** Parses the {@code requirementData} JSON string; returns {@code null} on failure or empty input. */
    @Nullable
    private static JsonObject parseRequirement(@Nullable final String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (final Exception e) {
            return null;
        }
    }

    private static boolean isType(@NotNull final JsonObject req, @NotNull final String type) {
        final String t = getString(req, "type");
        return type.equalsIgnoreCase(t);
    }

    @Nullable
    private static String getString(@NotNull final JsonObject obj, @NotNull final String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return null;
        return obj.get(key).getAsString();
    }

    // -------------------------------------------------------------------------
    // Entity category helpers (no external dependency)
    // -------------------------------------------------------------------------

    private static boolean isHostile(@NotNull final Entity entity) {
        return entity instanceof org.bukkit.entity.Monster
                || entity instanceof org.bukkit.entity.Slime
                || entity instanceof org.bukkit.entity.Ghast
                || entity instanceof org.bukkit.entity.MagmaCube
                || entity instanceof org.bukkit.entity.Shulker
                || entity instanceof org.bukkit.entity.Phantom;
    }

    private static boolean isPassive(@NotNull final Entity entity) {
        return entity instanceof org.bukkit.entity.Animals
                || entity instanceof org.bukkit.entity.Squid
                || entity instanceof org.bukkit.entity.Bat
                || entity instanceof org.bukkit.entity.WaterMob;
    }
}
