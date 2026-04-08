package com.raindropcentral.rdq.view.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
import com.raindropcentral.rdq.database.entity.quest.QuestTask;
import com.raindropcentral.rdq.model.quest.QuestProgress;
import com.raindropcentral.rdq.model.quest.TaskProgress;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Detailed quest view showing tasks, rewards, progress, and actions.
 * <p>
 * Layout (6 rows):
 * <pre>
 * X X X X I X X X X   row 0 — border + quest info at slot 4
 * X T T T T T T T X   row 1 — up to 7 task slots
 * X T T T T T T T X   row 2 — overflow task slots
 * X R R R R R R R X   row 3 — up to 7 reward slots
 * X X X X P X X X X   row 4 — border + overall progress at slot 40
 * B X X X A X X X X   row 5 — back(0), action button(49)
 * </pre>
 *
 * @author RaindropCentral
 * @version 2.0.0
 */
public class QuestDetailView extends BaseView {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");

    private final State<RDQ> rdq = initialState("plugin");
    private final State<Quest> quest = initialState("quest");
    private final State<QuestCategory> category = initialState("category");
    private final MutableState<QuestProgress> questProgress = mutableState(null);

    /** Enum representing the action button state. */
    private enum ActionState { LOADING, START, ABANDON, UNAVAILABLE }

    /** Current action button state. */
    private final MutableState<ActionState> actionState = mutableState(ActionState.LOADING);
    /** Failure reason when action is unavailable. */
    private final MutableState<String> actionFailReason = mutableState("");

    private static final int INFO_SLOT     = 4;
    private static final int PROGRESS_SLOT = 40;
    private static final int ACTION_SLOT   = 49;
    private static final int[] TASK_SLOTS   = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private static final int[] REWARD_SLOTS = {28, 29, 30, 31, 32, 33, 34};

    /**
     * Constructs a new quest detail view, navigating back to the quest list.
     */
    public QuestDetailView() {
        super(QuestListView.class);
    }

    @Override
    protected void handleBackButtonClick(final @NotNull SlotClickContext click) {
        final RDQ plugin = rdq.get(click);
        final QuestCategory cat = category.get(click);
        if (plugin == null || cat == null) {
            click.closeForPlayer();
            return;
        }
        try {
            click.openForPlayer(QuestListView.class, Map.of("plugin", plugin, "category", cat));
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to navigate back to QuestListView", e);
            click.closeForPlayer();
        }
    }

    @Override
    protected String getKey() {
        return "view.quest.detail";
    }

    @Override
    protected int getUpdateSchedule() {
        // Update every 2 seconds (40 ticks) to refresh task progress
        // The suppliers will automatically re-evaluate when render.update() is called
        return 40;
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext open) {
        final Quest q = quest.get(open);
        return Map.of("quest", q != null ? q.getIdentifier() : "Unknown");
    }

    @Override
    protected @NotNull String[] getLayout() {
        return new String[]{
                "XXXXXXXXX",
                "XTTTTTTTX",
                "XTTTTTTTX",
                "XRRRRRRRX",
                "XXXXXXXXX",
                "         "
        };
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final Quest q = quest.get(render);
        final RDQ plugin = rdq.get(render);

        if (q == null) {
            render.slot(INFO_SLOT).renderWith(() -> UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(new I18n.Builder("view.quest.detail.error", player).build().component())
                    .build());
            return;
        }

        render.layoutSlot('X', UnifiedBuilderFactory.item(Material.GRAY_STAINED_GLASS_PANE)
                .setName(Component.text(" "))
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build());

        renderQuestInfo(render, player, q);
        renderTasks(render, player, q);
        renderRewards(render, player, q);

        // Register progress slot with supplier that reads from state
        render.slot(PROGRESS_SLOT).renderWith(() -> {
            final QuestProgress progress = questProgress.get(render);
            if (progress == null) {
                return UnifiedBuilderFactory.item(Material.GRAY_STAINED_GLASS_PANE)
                        .setName(new I18n.Builder("view.quest.detail.progress.not_started", player).build().component())
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build();
            }
            final int completed = (int) progress.taskProgress().stream().filter(TaskProgress::isCompleted).count();
            final int total = progress.taskProgress().size();
            final Component name = new I18n.Builder("view.quest.detail.progress.name", player)
                    .withPlaceholder("completed", completed)
                    .withPlaceholder("total", total)
                    .build().component();
            final List<Component> lore = new ArrayList<>();
            lore.add(buildProgressBar(progress.overallProgress()));
            lore.add(new I18n.Builder("view.quest.detail.progress.percent", player)
                    .withPlaceholder("percent", String.format("%.0f", progress.overallProgress()))
                    .build().component());
            return UnifiedBuilderFactory.item(Material.EXPERIENCE_BOTTLE)
                    .setName(name)
                    .setLore(lore)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        });

        // Register action slot upfront — supplier reads MutableState so it re-evaluates on update()
        render.slot(ACTION_SLOT).renderWith(() -> buildActionItem(render, player, q, plugin))
                .onClick(click -> handleActionClick(click, q, plugin));

        // Load active status async, then set state and trigger update on main thread
        plugin.getQuestService().getProgress(player.getUniqueId(), q.getIdentifier())
                .thenCompose(progressOpt -> {
                    if (progressOpt.isPresent()) {
                        return java.util.concurrent.CompletableFuture.completedFuture(
                                new LoadResult(progressOpt.get(), true, null));
                    }
                    return plugin.getQuestService().canStartQuest(player.getUniqueId(), q.getIdentifier())
                            .thenApply(result -> new LoadResult(null, false, result.success() ? null : result.failureReason()));
                })
                .thenAccept(result -> plugin.getPlatform().getScheduler().runAtEntity(player, () -> {
                    if (result.active()) {
                        actionState.set(ActionState.ABANDON, render);
                        questProgress.set(result.progress(), render);
                        updateProgressSlot(render, player, result.progress());
                        updateTasksWithProgress(render, player, q, result.progress());

                        // Start periodic progress refresh task
                        startProgressRefreshTask(render, player, q, plugin);
                    } else if (result.failReason() == null) {
                        actionState.set(ActionState.START, render);
                    } else {
                        actionState.set(ActionState.UNAVAILABLE, render);
                        actionFailReason.set(result.failReason(), render);
                    }
                    render.update();
                }))
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Failed to load quest progress", ex);
                    plugin.getPlatform().getScheduler().runAtEntity(player, () -> {
                        actionState.set(ActionState.START, render);
                        render.update();
                    });
                    return null;
                });
    }

    /**
     * Builds the action button item based on current {@link #actionState}.
     *
     * @param render the render context
     * @param player the viewing player
     * @param q      the quest
     * @param plugin the plugin instance
     * @return the item stack
     */
    private @NotNull org.bukkit.inventory.ItemStack buildActionItem(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @NotNull Quest q,
            final @NotNull RDQ plugin
    ) {
        final ActionState state = actionState.get(render);
        return switch (state) {
            case LOADING -> UnifiedBuilderFactory.item(Material.CLOCK)
                    .setName(new I18n.Builder("view.quest.detail.action.loading", player).build().component())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
            case START -> UnifiedBuilderFactory.item(Material.LIME_DYE)
                    .setName(new I18n.Builder("view.quest.detail.action.start.name", player).build().component())
                    .setLore(new I18n.Builder("view.quest.detail.action.start.lore", player).build().children())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .setGlowing(true)
                    .build();
            case ABANDON -> UnifiedBuilderFactory.item(Material.RED_DYE)
                    .setName(new I18n.Builder("view.quest.detail.action.abandon.name", player).build().component())
                    .setLore(new I18n.Builder("view.quest.detail.action.abandon.lore", player).build().children())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
            case UNAVAILABLE -> UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(new I18n.Builder("view.quest.detail.action.unavailable.name", player).build().component())
                    .setLore(new I18n.Builder("view.quest.detail.action.unavailable.lore", player)
                            .withPlaceholder("reason", actionFailReason.get(render))
                            .build().children())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        };
    }

    /**
     * Handles the action button click based on current state.
     *
     * @param click  the click context
     * @param q      the quest
     * @param plugin the plugin instance
     */
    private void handleActionClick(
            final @NotNull SlotClickContext click,
            final @NotNull Quest q,
            final @NotNull RDQ plugin
    ) {
        final ActionState state = actionState.get(click);
        if (state == ActionState.START) {
            handleStart(click, q, plugin);
        } else if (state == ActionState.ABANDON) {
            handleAbandon(click, q, plugin);
        }
    }

    /**
     * Updates the progress slot after async load.
     *
     * @param render   the render context
     * @param player   the viewing player
     * @param progress the quest progress
     */
    private void updateProgressSlot(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @Nullable QuestProgress progress
    ) {
        if (progress == null) return;
        final int completed = (int) progress.taskProgress().stream().filter(TaskProgress::isCompleted).count();
        final int total = progress.taskProgress().size();
        render.slot(PROGRESS_SLOT).renderWith(() -> {
            final Component name = new I18n.Builder("view.quest.detail.progress.name", player)
                    .withPlaceholder("completed", completed)
                    .withPlaceholder("total", total)
                    .build().component();
            final List<Component> lore = new ArrayList<>();
            lore.add(buildProgressBar(progress.overallProgress()));
            lore.add(new I18n.Builder("view.quest.detail.progress.percent", player)
                    .withPlaceholder("percent", String.format("%.0f", progress.overallProgress()))
                    .build().component());
            return UnifiedBuilderFactory.item(Material.EXPERIENCE_BOTTLE)
                    .setName(name)
                    .setLore(lore)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        });
    }

    /**
     * Re-renders task slots with live progress data.
     *
     * @param render   the render context
     * @param player   the viewing player
     * @param q        the quest
     * @param progress the quest progress
     */
    private void updateTasksWithProgress(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @NotNull Quest q,
            final @Nullable QuestProgress progress
    ) {
        if (progress == null) return;
        final List<QuestTask> tasks = q.getTasks();
        for (int i = 0; i < Math.min(tasks.size(), TASK_SLOTS.length); i++) {
            final QuestTask task = tasks.get(i);
            final int slot = TASK_SLOTS[i];
            final String taskId = task.getTaskIdentifier();
            final TaskProgress tp = progress.taskProgress().stream()
                    .filter(t -> t.taskName().equals(taskId))
                    .findFirst()
                    .orElse(null);
            render.slot(slot).renderWith(() -> buildTaskItem(player, task, tp));
        }
    }

    /** Internal DTO for async load result. */
    private record LoadResult(
            @Nullable QuestProgress progress,
            boolean active,
            @Nullable String failReason
    ) {}

    // -------------------------------------------------------------------------
    // Quest info (slot 4)
    // -------------------------------------------------------------------------

    /**
     * Renders the quest header item showing name, description, difficulty, and meta.
     *
     * @param render the render context
     * @param player the viewing player
     * @param q      the quest
     */
    private void renderQuestInfo(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @NotNull Quest q
    ) {
        render.slot(INFO_SLOT).renderWith(() -> {
            final Material mat = parseMaterial(q.getIcon().getMaterial(), Material.BOOK);
            final Component name = new I18n.Builder(q.getIcon().getDisplayNameKey(), player).build().component();

            final List<Component> lore = new ArrayList<>(
                    new I18n.Builder(q.getIcon().getDescriptionKey(), player).build().children());
            lore.add(Component.empty());

            lore.add(new I18n.Builder(
                    "view.quest.detail.difficulty." + q.getDifficulty().name().toLowerCase(), player)
                    .build().component());

            if (q.isRepeatable()) {
                lore.add(new I18n.Builder("view.quest.detail.info.repeatable", player).build().component());
            }
            if (q.getCooldown().getSeconds() > 0) {
                lore.add(new I18n.Builder("view.quest.detail.info.cooldown", player)
                        .withPlaceholder("time", formatDuration(q.getCooldown()))
                        .build().component());
            }
            if (q.getTimeLimit().getSeconds() > 0) {
                lore.add(new I18n.Builder("view.quest.detail.info.time_limit", player)
                        .withPlaceholder("time", formatDuration(q.getTimeLimit()))
                        .build().component());
            }

            return UnifiedBuilderFactory.item(mat)
                    .setName(name)
                    .setLore(lore)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        });
    }

    // -------------------------------------------------------------------------
    // Tasks (slots 10-16, 19-25)
    // -------------------------------------------------------------------------

    /**
     * Renders each task in its own slot. If the quest is active, task progress is shown.
     *
     * @param render the render context
     * @param player the viewing player
     * @param q      the quest
     */
    private void renderTasks(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @NotNull Quest q
    ) {
        final List<QuestTask> tasks = q.getTasks();
        for (int i = 0; i < Math.min(tasks.size(), TASK_SLOTS.length); i++) {
            final QuestTask task = tasks.get(i);
            final int slot = TASK_SLOTS[i];
            // Use supplier that reads from state - will auto-update when state changes
            render.slot(slot).renderWith(() -> {
                final QuestProgress progress = questProgress.get(render);
                final TaskProgress tp = progress != null ? progress.taskProgress().stream()
                        .filter(t -> t.taskName().equals(task.getTaskIdentifier()))
                        .findFirst()
                        .orElse(null) : null;
                return buildTaskItem(player, task, tp);
            });
        }
    }

    /**
     * Builds a task item, optionally showing live progress.
     *
     * @param player   the viewing player
     * @param task     the quest task
     * @param progress the task progress, or null if not active
     * @return the item stack
     */
    private @NotNull org.bukkit.inventory.ItemStack buildTaskItem(
            final @NotNull Player player,
            final @NotNull QuestTask task,
            final @Nullable TaskProgress progress
    ) {
        final Material mat;
        if (progress != null && progress.isCompleted()) {
            mat = Material.LIME_DYE;
        } else if (progress != null && progress.currentCount() > 0) {
            mat = Material.YELLOW_DYE;
        } else {
            mat = parseMaterial(task.getIcon().getMaterial(), Material.GRAY_DYE);
        }

        final Component name = new I18n.Builder(task.getIcon().getDisplayNameKey(), player).build().component();
        final List<Component> lore = new ArrayList<>(
                new I18n.Builder(task.getIcon().getDescriptionKey(), player).build().children());
        lore.add(Component.empty());

        // Difficulty
        lore.add(new I18n.Builder(
                "view.quest.detail.task.difficulty." + task.getDifficulty().name().toLowerCase(), player)
                .build().component());

        // Requirement hint from JSON
        final String reqHint = parseRequirementHint(task.getRequirementData());
        if (!reqHint.isEmpty()) {
            lore.add(new I18n.Builder("view.quest.detail.task.objective", player)
                    .withPlaceholder("objective", reqHint)
                    .build().component());
        }

        // Progress if active
        if (progress != null) {
            lore.add(Component.empty());
            lore.add(new I18n.Builder("view.quest.detail.task.progress", player)
                    .withPlaceholder("current", progress.currentCount())
                    .withPlaceholder("required", progress.requiredCount())
                    .build().component());
            lore.add(buildProgressBar(progress.progressPercentage() * 100));
            if (progress.isCompleted()) {
                lore.add(new I18n.Builder("view.quest.detail.task.completed", player).build().component());
            }
        }

        return UnifiedBuilderFactory.item(mat)
                .setName(name)
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    // -------------------------------------------------------------------------
    // Rewards (slots 28-34)
    // -------------------------------------------------------------------------

    /**
     * Renders quest rewards parsed from the JSON reward data.
     *
     * @param render the render context
     * @param player the viewing player
     * @param q      the quest
     */
    private void renderRewards(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @NotNull Quest q
    ) {
        final List<RewardEntry> rewards = parseRewards(q.getRewardData());

        if (rewards.isEmpty()) {
            render.slot(REWARD_SLOTS[3]).renderWith(() ->
                    UnifiedBuilderFactory.item(Material.GRAY_STAINED_GLASS_PANE)
                            .setName(new I18n.Builder("view.quest.detail.reward.none", player).build().component())
                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                            .build());
            return;
        }

        for (int i = 0; i < Math.min(rewards.size(), REWARD_SLOTS.length); i++) {
            final RewardEntry reward = rewards.get(i);
            final int slot = REWARD_SLOTS[i];
            render.slot(slot).renderWith(() -> buildRewardItem(player, reward));
        }
    }

    /**
     * Builds a reward item.
     *
     * @param player the viewing player
     * @param reward the reward entry
     * @return the item stack
     */
    private @NotNull org.bukkit.inventory.ItemStack buildRewardItem(
            final @NotNull Player player,
            final @NotNull RewardEntry reward
    ) {
        final Component line = switch (reward.type()) {
            case "CURRENCY" -> new I18n.Builder("view.quest.detail.reward.currency", player)
                    .withPlaceholder("amount", (int) reward.amount())
                    .withPlaceholder("currency", reward.label())
                    .build().component();
            case "EXPERIENCE" -> new I18n.Builder("view.quest.detail.reward.experience", player)
                    .withPlaceholder("amount", (int) reward.amount())
                    .build().component();
            case "ITEM" -> new I18n.Builder("view.quest.detail.reward.item", player)
                    .withPlaceholder("amount", (int) reward.amount())
                    .withPlaceholder("item", reward.label())
                    .build().component();
            case "PERK" -> new I18n.Builder("view.quest.detail.reward.perk", player)
                    .withPlaceholder("perk", reward.label())
                    .build().component();
            case "TITLE" -> new I18n.Builder("view.quest.detail.reward.title", player)
                    .withPlaceholder("title", reward.label())
                    .build().component();
            default -> new I18n.Builder("view.quest.detail.reward.special", player).build().component();
        };

        return UnifiedBuilderFactory.item(reward.material())
                .setName(new I18n.Builder("view.quest.detail.reward.header", player).build().component())
                .setLore(List.of(Component.empty(), line))
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .setGlowing(true)
                .build();
    }

    // -------------------------------------------------------------------------
    // Click handlers
    // -------------------------------------------------------------------------

    /**
     * Handles the start quest button click.
     *
     * @param click  the click context
     * @param q      the quest
     * @param plugin the plugin instance
     */
    private void handleStart(
            final @NotNull SlotClickContext click,
            final @NotNull Quest q,
            final @NotNull RDQ plugin
    ) {
        final Player player = click.getPlayer();

        plugin.getQuestService().startQuest(player.getUniqueId(), q.getIdentifier())
                .thenAccept(result -> plugin.getPlatform().getScheduler().runAtEntity(player, () -> {
                    if (result.success()) {
                        new I18n.Builder("view.quest.detail.message.started", player)
                                .withPlaceholder("quest", q.getIdentifier())
                                .build().sendMessage();
                        // Close the GUI after starting the quest
                        click.closeForPlayer();
                    } else {
                        new I18n.Builder("view.quest.detail.message.start_failed", player)
                                .withPlaceholder("reason", result.failureReason())
                                .build().sendMessage();
                    }
                }))
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Failed to start quest", ex);
                    return null;
                });
    }

    /**
     * Handles the abandon quest button click — opens the confirmation dialog.
     *
     * @param click  the click context
     * @param q      the quest
     * @param plugin the plugin instance
     */
    private void handleAbandon(
            final @NotNull SlotClickContext click,
            final @NotNull Quest q,
            final @NotNull RDQ plugin
    ) {
        final Player player = click.getPlayer();
        click.closeForPlayer();
        plugin.getViewFrame().open(QuestAbandonConfirmationView.class, player, Map.of(
                "plugin", plugin,
                "quest", q
        ));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a MiniMessage progress bar component.
     *
     * @param percent the completion percentage (0-100)
     * @return the progress bar component
     */
    private @NotNull Component buildProgressBar(final double percent) {
        final int filled = (int) Math.round(percent / 10.0);
        final StringBuilder bar = new StringBuilder("<gray>[</gray>");
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "<green>|</green>" : "<dark_gray>|</dark_gray>");
        }
        bar.append("<gray>]</gray>");
        return MiniMessage.miniMessage().deserialize(bar.toString());
    }

    /**
     * Parses a human-readable objective hint from task requirement JSON.
     *
     * @param requirementData the JSON string
     * @return a short hint string, or empty string if unparseable
     */
    private @NotNull String parseRequirementHint(final @Nullable String requirementData) {
        if (requirementData == null || requirementData.isBlank()) return "";
        try {
            final JsonObject obj = JsonParser.parseString(requirementData).getAsJsonObject();
            final String type = obj.has("type") ? obj.get("type").getAsString() : "";
            final String target = obj.has("target") ? obj.get("target").getAsString() : "";
            final int amount = obj.has("amount") ? obj.get("amount").getAsInt() : 0;
            if (!target.isEmpty() && amount > 0) {
                return amount + "x " + target.replace("_", " ").toLowerCase();
            }
            return type.isEmpty() ? "" : type.toLowerCase().replace("_", " ");
        } catch (final Exception e) {
            return "";
        }
    }

    /**
     * Parses reward entries from quest reward JSON.
     *
     * @param rewardData the JSON string
     * @return list of reward entries
     */
    private @NotNull List<RewardEntry> parseRewards(final @Nullable String rewardData) {
        final List<RewardEntry> list = new ArrayList<>();
        if (rewardData == null || rewardData.isBlank()) return list;
        try {
            final JsonObject obj = JsonParser.parseString(rewardData).getAsJsonObject();
            for (final Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                try {
                    final JsonObject r = entry.getValue().getAsJsonObject();
                    final String type = r.has("type") ? r.get("type").getAsString().toUpperCase() : "UNKNOWN";
                    final double amount = r.has("amount") ? r.get("amount").getAsDouble() : 0;
                    final String label = switch (type) {
                        case "CURRENCY" -> r.has("currency") ? r.get("currency").getAsString() : "coins";
                        case "ITEM" -> r.has("item") ? r.get("item").getAsString() : "item";
                        case "PERK" -> r.has("perk") ? r.get("perk").getAsString() : "perk";
                        case "TITLE" -> r.has("title") ? r.get("title").getAsString() : "title";
                        default -> "";
                    };
                    final Material mat = switch (type) {
                        case "CURRENCY" -> Material.GOLD_INGOT;
                        case "EXPERIENCE" -> Material.EXPERIENCE_BOTTLE;
                        case "PERK" -> Material.NETHER_STAR;
                        case "TITLE" -> Material.NAME_TAG;
                        default -> Material.DIAMOND;
                    };
                    list.add(new RewardEntry(type, mat, amount, label));
                } catch (final Exception ignored) {}
            }
        } catch (final Exception ignored) {}
        return list;
    }

    /**
     * Parses a material name safely with a fallback.
     *
     * @param name     the material name
     * @param fallback the fallback material
     * @return the resolved material
     */
    private static @NotNull Material parseMaterial(final @NotNull String name, final @NotNull Material fallback) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (final IllegalArgumentException e) {
            return fallback;
        }
    }

    /**
     * Formats a duration into a human-readable string.
     *
     * @param duration the duration
     * @return the formatted string
     */
    private @NotNull String formatDuration(final @NotNull Duration duration) {
        final long h = duration.toHours();
        final long m = duration.toMinutesPart();
        return h > 0 ? h + "h " + m + "m" : m + "m";
    }

    /**
     * Holds a parsed reward entry for display.
     *
     * @param type     the reward type string
     * @param material the display material
     * @param amount   the reward amount
     * @param label    the reward label (currency name, item name, etc.)
     */
    private record RewardEntry(
            @NotNull String type,
            @NotNull Material material,
            double amount,
            @NotNull String label
    ) {}

    /**
     * Starts a periodic task to refresh quest progress from the database.
     * This ensures the GUI updates when tasks are completed.
     *
     * @param render the render context
     * @param player the viewing player
     * @param q      the quest
     * @param plugin the plugin instance
     */
    private void startProgressRefreshTask(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @NotNull Quest q,
            final @NotNull RDQ plugin
    ) {
        // Schedule repeating task to refresh progress every 2 seconds
        plugin.getPlatform().getScheduler().runRepeatingAsync(() -> {
            // Check if player still has the GUI open
            if (!player.isOnline() || player.getOpenInventory().getTopInventory() != render.getContainer()) {
                return;
            }

            // Fetch fresh progress
            plugin.getQuestService().getProgress(player.getUniqueId(), q.getIdentifier())
                    .thenAccept(progressOpt -> {
                        plugin.getPlatform().getScheduler().runAtEntity(player, () -> {
                            if (progressOpt.isPresent()) {
                                // Update state and trigger re-render
                                questProgress.set(progressOpt.get(), render);
                                render.update();
                            } else {
                                // Quest no longer active - it was completed or abandoned
                                // Close the GUI
                                player.closeInventory();

                                // Send completion message
                                new I18n.Builder("view.quest.detail.message.completed", player)
                                        .withPlaceholder("quest", q.getIdentifier())
                                        .build().sendMessage();
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        LOGGER.log(Level.FINE, "Failed to refresh quest progress", ex);
                        return null;
                    });
        }, 40L, 40L); // Start after 2 seconds, repeat every 2 seconds
    }
}
