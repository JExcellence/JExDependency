package com.raindropcentral.rdq.view.quest;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
import com.raindropcentral.rdq.model.quest.ActiveQuest;
import com.raindropcentral.rdq.model.quest.QuestState;
import com.raindropcentral.rdq.model.quest.QuestStateInfo;
import com.raindropcentral.rdq.service.quest.QuestService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Paginated view displaying all quests within a category.
 * <p>
 * Each quest item shows:
 * <ul>
 *   <li>Quest name and description</li>
 *   <li>Difficulty level with color coding</li>
 *   <li>Task count (e.g. "3 tasks")</li>
 *   <li>Active badge and progress if the player is currently on this quest</li>
 *   <li>Completed badge if already finished</li>
 * </ul>
 * </p>
 *
 * @author RaindropCentral
 * @version 2.0.0
 */
public class QuestListView extends APaginatedView<Quest> {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");

    private final State<RDQ> rdq = initialState("plugin");
    private final State<QuestCategory> category = initialState("category");

    /**
     * Pre-fetched active quests keyed by quest identifier.
     * Populated in {@link #getAsyncPaginationSource} before rendering.
     */
    private final MutableState<Map<String, ActiveQuest>> activeQuestMap = mutableState(new HashMap<>());

    /**
     * Pre-fetched quest states keyed by quest identifier.
     * Populated in {@link #getAsyncPaginationSource} before rendering.
     */
    private final MutableState<Map<String, QuestStateInfo>> questStateMap = mutableState(new HashMap<>());

    /**
     * Constructs a new quest list view, navigating back to the category view.
     */
    public QuestListView() {
        super(QuestCategoryView.class);
    }

    @Override
    protected void handleBackButtonClick(final @NotNull SlotClickContext click) {
        final RDQ plugin = rdq.get(click);
        if (plugin == null) {
            click.closeForPlayer();
            return;
        }
        try {
            click.openForPlayer(QuestCategoryView.class, Map.of("plugin", plugin));
        } catch (final Exception e) {
            org.bukkit.Bukkit.getLogger().warning("Failed to navigate back to QuestCategoryView: " + e.getMessage());
            click.closeForPlayer();
        }
    }

    @Override
    protected String getKey() {
        return "view.quest.list";
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext open) {
        final QuestCategory cat = category.get(open);
        if (cat == null) {
            return Map.of("category", "Unknown");
        }
        return Map.of("category", cat.getIdentifier());
    }

    /**
     * Loads quests for the category and, in parallel, fetches the player's active quests
     * and quest states so each item can show live progress without a second async call at render time.
     *
     * @param context the inventory context
     * @return a future resolving to the sorted quest list
     */
    @Override
    protected @NotNull CompletableFuture<List<Quest>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final RDQ plugin = rdq.get(context);
        final QuestCategory cat = category.get(context);

        if (cat == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        final QuestService questService = plugin.getQuestService();
        final Player player = context.getPlayer();

        final CompletableFuture<List<Quest>> questsFuture =
                questService.getQuestsByCategory(cat.getIdentifier());

        final CompletableFuture<Map<String, ActiveQuest>> activeFuture =
                questService.getActiveQuests(player.getUniqueId())
                        .thenApply(list -> list.stream()
                                .collect(Collectors.toMap(ActiveQuest::questIdentifier, a -> a)))
                        .exceptionally(ex -> {
                            org.bukkit.Bukkit.getLogger().warning("Failed to load active quests for player: " + ex.getMessage());
                            return Map.of();
                        });

        return CompletableFuture.allOf(questsFuture, activeFuture)
                .thenCompose(v -> {
                    try {
                        final List<Quest> allQuests = questsFuture.join();
                        activeQuestMap.set(activeFuture.join(), context);

                        // Load quest states for all quests
                        final List<CompletableFuture<Map.Entry<String, com.raindropcentral.rdq.model.quest.QuestStateInfo>>> stateFutures =
                                allQuests.stream()
                                        .map(quest -> questService.getQuestState(player.getUniqueId(), quest.getIdentifier())
                                                .thenApply(state -> Map.entry(quest.getIdentifier(), state)))
                                        .collect(Collectors.toList());

                        return CompletableFuture.allOf(stateFutures.toArray(new CompletableFuture[0]))
                                .thenApply(v2 -> {
                                    final Map<String, com.raindropcentral.rdq.model.quest.QuestStateInfo> stateMap =
                                            stateFutures.stream()
                                                    .map(CompletableFuture::join)
                                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                                    questStateMap.set(stateMap, context);

                                    final List<Quest> enabledQuests = allQuests.stream()
                                            .filter(quest -> {
                                                boolean enabled = quest.isEnabled();
                                                return enabled;
                                            })
                                            .sorted(java.util.Comparator.comparingInt(
                                                    q -> q.getDifficulty().ordinal()))
                                            .collect(Collectors.toList());
                                    return enabledQuests;
                                });
                    } catch (Exception e) {
                        org.bukkit.Bukkit.getLogger().severe("[QuestListView] Error in thenApply: " + e.getMessage());
                        e.printStackTrace();
                        throw e;
                    }
                })
                .exceptionally(ex -> {
                    org.bukkit.Bukkit.getLogger().severe("Failed to load quests for category: " + cat.getIdentifier() + " - " + ex.getMessage());
                    ex.printStackTrace();
                    return List.of();
                });
    }

    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull Quest quest
    ) {
        final Player player = context.getPlayer();
        final Map<String, ActiveQuest> active = activeQuestMap.get(context);
        final Map<String, com.raindropcentral.rdq.model.quest.QuestStateInfo> states = questStateMap.get(context);

        final ActiveQuest activeQuest = active != null ? active.get(quest.getIdentifier()) : null;
        final com.raindropcentral.rdq.model.quest.QuestStateInfo stateInfo = states != null ? states.get(quest.getIdentifier()) : null;

        builder.renderWith(() -> buildQuestItem(player, quest, activeQuest, stateInfo))
                .onClick(click -> handleQuestClick(click, quest));
    }

    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
    }

    /**
     * Builds the item stack for a quest entry.
     *
     * @param player      the viewing player
     * @param quest       the quest
     * @param activeQuest the player's active progress, or null if not active
     * @param stateInfo   the quest state information
     * @return the configured item stack
     */
    private @NotNull org.bukkit.inventory.ItemStack buildQuestItem(
            final @NotNull Player player,
            final @NotNull Quest quest,
            final @Nullable ActiveQuest activeQuest,
            final @Nullable com.raindropcentral.rdq.model.quest.QuestStateInfo stateInfo
    ) {
        try {
            final Material material = parseMaterial(quest.getIcon().getMaterial(), Material.PAPER);
            final Component name = new I18n.Builder(quest.getIcon().getDisplayNameKey(), player).build().component();
            final List<Component> lore = buildQuestLore(player, quest, activeQuest, stateInfo);

            final org.bukkit.inventory.ItemStack item = UnifiedBuilderFactory.item(material)
                    .setName(name)
                    .setLore(lore)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();

            return item;
        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger().severe("[QuestListView] Error building quest item: " + e.getMessage());
            e.printStackTrace();
            // Return a fallback item
            return new org.bukkit.inventory.ItemStack(Material.BARRIER);
        }
    }

    /**
     * Builds the lore for a quest item, including status, difficulty, task count, and progress.
     *
     * @param player      the viewing player
     * @param quest       the quest
     * @param activeQuest the player's active progress, or null
     * @param stateInfo   the quest state information
     * @return the lore lines
     */
    private @NotNull List<Component> buildQuestLore(
            final @NotNull Player player,
            final @NotNull Quest quest,
            final @Nullable ActiveQuest activeQuest,
            final @Nullable com.raindropcentral.rdq.model.quest.QuestStateInfo stateInfo
    ) {
        final List<Component> lore = new ArrayList<>();

        // Description
        lore.addAll(new I18n.Builder(quest.getIcon().getDescriptionKey(), player).build().children());
        lore.add(Component.empty());

        // Difficulty
        lore.add(new I18n.Builder("view.quest.list.difficulty." + quest.getDifficulty().name().toLowerCase(), player)
                .build().component());

        // Task count
        final int taskCount = quest.getTasks().size();
        lore.add(new I18n.Builder("view.quest.list.tasks", player)
                .withPlaceholder("count", taskCount)
                .build().component());

        // State-based information
        if (stateInfo != null) {
            lore.add(Component.empty());
            addStateLore(lore, player, quest, stateInfo, activeQuest);
        }

        // Click hint
        lore.add(Component.empty());
        addClickHint(lore, player, stateInfo, activeQuest);

        return lore;
    }

    /**
     * Adds state-specific lore lines.
     *
     * @param lore        the lore list to add to
     * @param player      the viewing player
     * @param quest       the quest
     * @param stateInfo   the quest state information
     * @param activeQuest the active quest data, if any
     */
    private void addStateLore(
            final @NotNull List<Component> lore,
            final @NotNull Player player,
            final @NotNull Quest quest,
            final @NotNull com.raindropcentral.rdq.model.quest.QuestStateInfo stateInfo,
            final @Nullable ActiveQuest activeQuest
    ) {
        final String stateKey = "view.quest.list.state." + stateInfo.state().name().toLowerCase();
        lore.add(new I18n.Builder(stateKey, player).build().component());

        switch (stateInfo.state()) {
            case ACTIVE:
                if (activeQuest != null) {
                    lore.add(new I18n.Builder("view.quest.list.progress", player)
                            .withPlaceholder("completed", activeQuest.completedTasks())
                            .withPlaceholder("total", activeQuest.totalTasks())
                            .withPlaceholder("percent", (int) activeQuest.progressPercentage())
                            .build().component());
                    lore.add(buildProgressBar(activeQuest.progressPercentage()));
                }
                break;

            case LOCKED:
                if (!stateInfo.missingRequirements().isEmpty()) {
                    lore.add(new I18n.Builder("view.quest.list.requirements.missing", player).build().component());
                    for (final String req : stateInfo.missingRequirements()) {
                        lore.add(new I18n.Builder("view.quest.list.requirements.item", player)
                                .withPlaceholder("requirement", req)
                                .build().component());
                    }
                }
                break;

            case COMPLETED:
            case FINISHED:
                if (stateInfo.completionHistory() != null) {
                    final int count = stateInfo.completionHistory().getCompletionCount();
                    lore.add(new I18n.Builder("view.quest.list.completed.count", player)
                            .withPlaceholder("count", count)
                            .build().component());
                }
                break;

            case ON_COOLDOWN:
                final String timeRemaining = formatDuration(stateInfo.remainingCooldownSeconds());
                lore.add(new I18n.Builder("view.quest.list.cooldown.remaining", player)
                        .withPlaceholder("time", timeRemaining)
                        .build().component());
                lore.add(new I18n.Builder("view.quest.list.cooldown.hint", player).build().component());
                break;

            case MAX_COMPLETIONS:
                lore.add(new I18n.Builder("view.quest.list.max_completions.reached", player)
                        .withPlaceholder("max", quest.getMaxCompletions())
                        .build().component());
                break;

            case AVAILABLE:
            case AVAILABLE_TO_RESTART:
                // Show reward preview for available quests
                if (!quest.getRewards().isEmpty()) {
                    lore.add(new I18n.Builder("view.quest.list.rewards.header", player).build().component());
                    final int maxRewards = 3;
                    final List<com.raindropcentral.rdq.database.entity.quest.QuestReward> rewards = quest.getRewards();
                    for (int i = 0; i < Math.min(maxRewards, rewards.size()); i++) {
                        final com.raindropcentral.rdq.database.entity.quest.QuestReward reward = rewards.get(i);
                        lore.add(Component.text("  • " + reward.getReward().getTypeId()));
                    }
                    if (rewards.size() > maxRewards) {
                        lore.add(new I18n.Builder("view.quest.list.rewards.more", player)
                                .withPlaceholder("count", rewards.size() - maxRewards)
                                .build().component());
                    }
                }
                break;
        }
    }

    /**
     * Adds the appropriate click hint based on quest state.
     *
     * @param lore        the lore list to add to
     * @param player      the viewing player
     * @param stateInfo   the quest state information
     * @param activeQuest the active quest data, if any
     */
    private void addClickHint(
            final @NotNull List<Component> lore,
            final @NotNull Player player,
            final @Nullable com.raindropcentral.rdq.model.quest.QuestStateInfo stateInfo,
            final @Nullable ActiveQuest activeQuest
    ) {
        if (stateInfo == null) {
            lore.add(new I18n.Builder("view.quest.list.click.view_details", player).build().component());
            return;
        }

        switch (stateInfo.state()) {
            case AVAILABLE:
                lore.add(new I18n.Builder("view.quest.list.click.start", player).build().component());
                break;
            case AVAILABLE_TO_RESTART:
                lore.add(new I18n.Builder("view.quest.list.click.restart", player).build().component());
                break;
            case ACTIVE:
                lore.add(new I18n.Builder("view.quest.list.click.view_progress", player).build().component());
                break;
            default:
                lore.add(new I18n.Builder("view.quest.list.click.view_details", player).build().component());
                break;
        }
    }

    /**
     * Formats a duration in seconds to a human-readable string.
     *
     * @param seconds the duration in seconds
     * @return the formatted string
     */
    private @NotNull String formatDuration(final long seconds) {
        final long hours = seconds / 3600;
        final long minutes = (seconds % 3600) / 60;
        final long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }

    /**
     * Builds a simple text progress bar component.
     *
     * @param percent the completion percentage (0-100)
     * @return the progress bar component
     */
    private @NotNull Component buildProgressBar(final double percent) {
        final int filled = (int) (percent / 10);
        final StringBuilder bar = new StringBuilder("<gray>[</gray>");
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("<green>|</green>");
            } else {
                bar.append("<dark_gray>|</dark_gray>");
            }
        }
        bar.append("<gray>]</gray>");
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(bar.toString());
    }

    /**
     * Opens the quest detail view for the clicked quest.
     *
     * @param click the click context
     * @param quest the clicked quest
     */
    private void handleQuestClick(
            final @NotNull SlotClickContext click,
            final @NotNull Quest quest
    ) {
        final Player player = click.getPlayer();
        final RDQ plugin = rdq.get(click);
        final QuestCategory cat = category.get(click);

        click.closeForPlayer();
        plugin.getViewFrame().open(QuestDetailView.class, player, Map.of(
                "plugin", plugin,
                "quest", quest,
                "category", cat
        ));
    }

    /**
     * Parses a material name safely with a fallback.
     *
     * @param name     the material name
     * @param fallback the fallback material
     * @return the resolved material
     */
    private static @NotNull Material parseMaterial(
            final @NotNull String name,
            final @NotNull Material fallback
    ) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (final IllegalArgumentException e) {
            return fallback;
        }
    }
}
