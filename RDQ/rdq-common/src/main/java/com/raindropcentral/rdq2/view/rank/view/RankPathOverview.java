/*
package com.raindropcentral.rdq2.view.rank.view;

import com.raindropcentral.rdq2.RDQ;
import com.raindropcentral.rdq2.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq2.database.entity.rank.RPlayerRank;
import com.raindropcentral.rdq2.database.entity.rank.RPlayerRankUpgradeProgress;
import com.raindropcentral.rdq2.database.entity.rank.RRank;
import com.raindropcentral.rdq2.database.entity.rank.RRankTree;
import com.raindropcentral.rdq2.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq2.manager.rank.RankRequirementProgressManager;
import com.raindropcentral.rdq2.view.rank.ERankStatus;
import com.raindropcentral.rdq2.view.rank.RankNode;
import com.raindropcentral.rdq2.view.rank.grid.GridPosition;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.heads.view.Down;
import com.raindropcentral.rplatform.utility.heads.view.Next;
import com.raindropcentral.rplatform.utility.heads.view.Previous;
import com.raindropcentral.rplatform.utility.heads.view.Return;
import com.raindropcentral.rplatform.utility.heads.view.Up;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class RankPathOverview extends BaseView {

    private static final Logger LOGGER = CentralLogger.getLogger(RankPathOverview.class.getName());

    private final State<RDQ> rdq = this.initialState("plugin");
    private final State<RDQPlayer> currentPlayer = this.initialState("player");
    private final State<RRankTree> selectedRankTree = this.initialState("rankTree");
    private final State<Boolean> isPreviewMode = this.initialState("previewMode");

    private final MutableState<Integer> offsetX = this.mutableState(0);
    private final MutableState<Integer> offsetY = this.mutableState(0);
    private final MutableState<Long> lastRefreshAt = this.mutableState(System.currentTimeMillis());

    private final MutableState<Map<String, RankNode>> nodeGraph = this.mutableState(new HashMap<>());
    private final MutableState<Map<String, GridPosition>> nodeWorldPositions = this.mutableState(new HashMap<>());
    private final MutableState<Map<String, ERankStatus>> nodeStatuses = this.mutableState(new HashMap<>());
    private final MutableState<Set<String>> ownedNodeIds = this.mutableState(new HashSet<>());
    private final MutableState<Set<String>> pendingNodeIds = this.mutableState(new HashSet<>());

    private static final Map<GridPosition, Integer> SLOT_MAPPING = createSlotMapping();
    private static final List<Integer>              VALID_SLOTS  = createValidSlots();
    private static final GridPosition               ROOT_ANCHOR  = new GridPosition(3, 2);
    private static final int                        GRID_STEP    = 5;

    private static final int NAV_LEFT_SLOT  = 18;
    private static final int NAV_RIGHT_SLOT = 26;
    private static final int NAV_UP_SLOT    = 4;
    private static final int NAV_DOWN_SLOT  = 49;
    private static final int BACK_SLOT      = 45;
    private static final int CENTER_SLOT    = 53;
    private static final int PREVIEW_SLOT   = 8;

    private static final Material OWNED_MATERIAL      = Material.LIME_STAINED_GLASS_PANE;
    private static final Material PENDING_MATERIAL    = Material.YELLOW_STAINED_GLASS_PANE;
    private static final Material AVAILABLE_MATERIAL  = Material.ORANGE_STAINED_GLASS_PANE;
    private static final Material LOCKED_MATERIAL     = Material.RED_STAINED_GLASS_PANE;
    private static final Material CONNECTION_MATERIAL = Material.LIGHT_GRAY_STAINED_GLASS_PANE;
    private static final Material BACKGROUND_MATERIAL = Material.GRAY_STAINED_GLASS_PANE;
    private static final Material PREVIEW_MATERIAL    = Material.SPYGLASS;

    private @Nullable RankRequirementProgressManager progressManager;

    public RankPathOverview() {
        super();
    }

    @Override
    protected String getKey() {
        return "rank_path_overview_ui";
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        try {
            final RRankTree rankTree    = this.selectedRankTree.get(openContext);
            final boolean   previewMode = this.isPreviewMode.get(openContext);
            TranslatedMessage rankTreeDisplayName = this.extractRankTreeDisplayName(rankTree, openContext.getPlayer());
            if (previewMode) {
                rankTreeDisplayName = this.i18n("preview_prefix", openContext.getPlayer()).build().append(rankTreeDisplayName.component());
            }
            return Map.of("rank_tree_name", rankTreeDisplayName.asLegacyText());
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to generate title placeholders", exception);
            return Map.of("rank_tree_name", this.i18n("unknown_tree", openContext.getPlayer()).build().component());
        }
    }

    @Override
    public void onResume(@NotNull final Context origin, @NotNull final Context target) {
        try {
            final boolean previewMode = isPreviewMode.get(target);
            initializeAndCacheData(target, previewMode);
            renderDynamicRankGrid((RenderContext) target, target.getPlayer(), selectedRankTree.get(target), previewMode);
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to refresh data on resume", exception);
        }
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext renderContext, final @NotNull Player player) {
        try {
            final RRankTree rankTree = selectedRankTree.get(renderContext);
            final boolean previewMode = isPreviewMode.get(renderContext);
            
            if (rankTree == null) {
                renderErrorState(renderContext, player);
                return;
            }
            
            initializeAndCacheData(renderContext, previewMode);
            initializeViewOffsets(renderContext);
            renderStaticInterfaceComponents(renderContext, player, rankTree, previewMode);
            renderDynamicRankGrid(renderContext, player, rankTree, previewMode);
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Critical error during rank path overview render", exception);
            renderCriticalErrorState(renderContext, player);
        }
    }

    private void initializeAndCacheData(final @NotNull Context renderContext, final boolean previewMode) {
        try {
            final RRankTree rankTree = selectedRankTree.get(renderContext);
            final RDQPlayer rdqPlayer = currentPlayer.get(renderContext);
            final RDQ plugin = rdq.get(renderContext);
            
            final Map<String, RankNode> rankHierarchy = buildRankNodeHierarchy(rankTree);
            nodeGraph.set(rankHierarchy, renderContext);
            
            final Map<String, GridPosition> worldPositions = calculateRankWorldPositions(rankHierarchy);
            nodeWorldPositions.set(worldPositions, renderContext);
            
            if (!previewMode) {
                final Set<String> ownedRanks = loadOwnedRanks(plugin, rdqPlayer, rankTree);
                final Set<String> inProgressRanks = loadInProgressRanks(plugin, rdqPlayer, rankTree, renderContext.getPlayer());
                ownedNodeIds.set(ownedRanks, renderContext);
                pendingNodeIds.set(inProgressRanks, renderContext);
                
                final Map<String, ERankStatus> rankStatuses = calculateAllRankStatuses(rankHierarchy, ownedRanks, inProgressRanks, previewMode);
                nodeStatuses.set(rankStatuses, renderContext);
            } else {
                final Map<String, ERankStatus> previewStatuses = rankHierarchy.entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getRank().isInitialRank() ? ERankStatus.OWNED : ERankStatus.AVAILABLE
                    ));
                nodeStatuses.set(previewStatuses, renderContext);
            }
            
            lastRefreshAt.set(System.currentTimeMillis(), renderContext);
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to initialize and cache data", exception);
            nodeGraph.set(new HashMap<>(), renderContext);
            nodeWorldPositions.set(new HashMap<>(), renderContext);
            nodeStatuses.set(new HashMap<>(), renderContext);
            ownedNodeIds.set(new HashSet<>(), renderContext);
            pendingNodeIds.set(new HashSet<>(), renderContext);
        }
    }

    private @NotNull Set<String> loadOwnedRanks(final @NotNull RDQ plugin, final @NotNull RDQPlayer rdqPlayer, final @NotNull RRankTree rankTree) {
        try {
            final Set<String> ownedRanks = new HashSet<>();
            final RPlayerRank playerRank = this.getPlayerRankForTree(plugin, rdqPlayer, rankTree);
            if (playerRank != null && playerRank.getCurrentRank() != null) {
                final RRank currentRank = playerRank.getCurrentRank();
                ownedRanks.add(currentRank.getIdentifier());
                this.addProgressionPathRanks(currentRank, rankTree, ownedRanks);
            }
            return ownedRanks;
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to load owned ranks", exception);
            return new HashSet<>();
        }
    }

    private void addProgressionPathRanks(final @NotNull RRank targetRank, final @NotNull RRankTree rankTree, final @NotNull Set<String> ownedRanks) {
        final Map<String, RRank> rankMap = rankTree.getRanks().stream().collect(Collectors.toMap(RRank::getIdentifier, rank -> rank));
        final Set<String> visited = new HashSet<>();
        this.addProgressionPathRanksRecursive(targetRank, rankMap, ownedRanks, visited);
    }

    private void addProgressionPathRanksRecursive(final @NotNull RRank currentRank, final @NotNull Map<String, RRank> rankMap, final @NotNull Set<String> ownedRanks, final @NotNull Set<String> visited) {
        if (visited.contains(currentRank.getIdentifier())) {
            return;
        }
        visited.add(currentRank.getIdentifier());
        ownedRanks.add(currentRank.getIdentifier());
        for (final String previousRankId : currentRank.getPreviousRanks()) {
            final RRank previousRank = rankMap.get(previousRankId);
            if (previousRank != null) {
                this.addProgressionPathRanksRecursive(previousRank, rankMap, ownedRanks, visited);
            }
        }
    }

    private @NotNull Set<String> loadInProgressRanks(final @NotNull RDQ plugin, final @NotNull RDQPlayer rdqPlayer, final @NotNull RRankTree rankTree, final @NotNull Player player) {
        try {
            final Set<String> inProgressRanks = new HashSet<>();
            if (this.progressManager == null) {
                this.progressManager = new RankRequirementProgressManager(plugin);
            }
            for (final RRank rank : rankTree.getRanks()) {
                if (this.isRankInProgress(rdqPlayer, rank, player)) {
                    inProgressRanks.add(rank.getIdentifier());
                }
            }
            return inProgressRanks;
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to load in-progress ranks", exception);
            return new HashSet<>();
        }
    }

    private boolean isRankInProgress(final @NotNull RDQPlayer rdqPlayer, final @NotNull RRank rank, final @NotNull Player player) {
        try {
            if (rank.getUpgradeRequirements().isEmpty()) {
                return false;
            }
            if (this.progressManager == null) {
                return false;
            }
            final double overallProgress = progressManager.getRankOverallProgress(player, rdqPlayer, rank);
            final boolean allCompleted = progressManager.areAllRequirementsCompleted(player, rdqPlayer, rank);
            return overallProgress > 0.0 && !allCompleted;
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to check if rank is in progress: " + rank.getIdentifier(), exception);
            return false;
        }
    }

    private @NotNull Map<String, ERankStatus> calculateAllRankStatuses(final @NotNull Map<String, RankNode> rankHierarchy, final @NotNull Set<String> ownedRanks, final @NotNull Set<String> inProgressRanks, final boolean previewMode) {
        final Map<String, ERankStatus> statuses = new HashMap<>();
        if (previewMode) {
            for (final Map.Entry<String, RankNode> entry : rankHierarchy.entrySet()) {
                final RRank rank = entry.getValue().getRank();
                statuses.put(entry.getKey(), rank.isInitialRank() ? ERankStatus.OWNED : ERankStatus.AVAILABLE);
            }
            return statuses;
        }
        for (final Map.Entry<String, RankNode> entry : rankHierarchy.entrySet()) {
            final String   rankId   = entry.getKey();
            final RankNode rankNode = entry.getValue();
            if (ownedRanks.contains(rankId)) {
                statuses.put(rankId, ERankStatus.OWNED);
            } else if (inProgressRanks.contains(rankId)) {
                statuses.put(rankId, ERankStatus.IN_PROGRESS);
            } else if (this.arePrerequisitesMetCached(rankNode, ownedRanks)) {
                statuses.put(rankId, ERankStatus.AVAILABLE);
            } else {
                statuses.put(rankId, ERankStatus.LOCKED);
            }
        }
        return statuses;
    }

    private boolean arePrerequisitesMetCached(final @NotNull RankNode rankNode, final @NotNull Set<String> ownedRanks) {
        if (rankNode.getParents().isEmpty() || rankNode.getRank().isInitialRank()) {
            return true;
        }
        return rankNode.getParents().stream().anyMatch(parent -> ownedRanks.contains(parent.getRank().getIdentifier()));
    }

    private @NotNull TranslatedMessage extractRankTreeDisplayName(final @Nullable RRankTree rankTree, final @NotNull Player player) {
        if (rankTree == null) {
            return this.i18n("unknown_tree", player).build();
        }
        return TranslationService.create(TranslationKey.of(rankTree.getDisplayNameKey()), player).build();
    }

    private void initializeViewOffsets(final @NotNull RenderContext renderContext) {
        offsetX.set(0, renderContext);
        offsetY.set(0, renderContext);
    }

    private void renderStaticInterfaceComponents(final @NotNull RenderContext renderContext, final @NotNull Player player, final @NotNull RRankTree rankTree, final boolean previewMode) {
        try {
            renderNavigationControls(renderContext, player);
            renderUtilityButtons(renderContext, player, previewMode);
            if (previewMode) {
                renderPreviewModeIndicator(renderContext, player);
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to render static interface components", exception);
        }
    }

    private void renderNavigationControls(final @NotNull RenderContext renderContext, final @NotNull Player player) {
        this.renderNavigationArrow(renderContext, player, NAV_LEFT_SLOT, new Previous(), "left", -1, 0);
        this.renderNavigationArrow(renderContext, player, NAV_RIGHT_SLOT, new Next(), "right", 1, 0);
        this.renderNavigationArrow(renderContext, player, NAV_UP_SLOT, new Up(), "up", 0, -1);
        this.renderNavigationArrow(renderContext, player, NAV_DOWN_SLOT, new Down(), "down", 0, 1);
    }

    private void renderNavigationArrow(final @NotNull RenderContext renderContext, final @NotNull Player player, final int slotNumber, final @NotNull Object headProvider, final @NotNull String direction, final int deltaX, final int deltaY) {
        try {
            renderContext.slot(slotNumber)
                    .renderWith(() -> this.createNavigationArrowItem(headProvider, player, direction))
                    .onClick(clickContext -> this.handleNavigationClick(renderContext, deltaX, deltaY, direction));
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to render navigation arrow: " + direction, exception);
        }
    }

    private @NotNull ItemStack createNavigationArrowItem(final @NotNull Object headProvider, final @NotNull Player player, final @NotNull String direction) {
        try {
            final ItemStack headItem = this.extractHeadFromProvider(headProvider, player);
            return UnifiedBuilderFactory.item(headItem)
                    .setName(this.i18n("nav." + direction, player).build().component())
                    .setLore(List.of())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to create navigation arrow item for: " + direction, exception);
            return UnifiedBuilderFactory.item(Material.ARROW)
                    .setName(this.i18n("nav.fallback", player).with("direction", direction).build().component())
                    .setLore(List.of())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        }
    }

    private @NotNull ItemStack extractHeadFromProvider(final @NotNull Object headProvider, final @NotNull Player player) {
        if (headProvider instanceof Previous) {
            return ((Previous) headProvider).getHead(player);
        } else if (headProvider instanceof Next) {
            return ((Next) headProvider).getHead(player);
        } else if (headProvider instanceof Up) {
            return ((Up) headProvider).getHead(player);
        } else if (headProvider instanceof Down) {
            return ((Down) headProvider).getHead(player);
        }
        return UnifiedBuilderFactory.item(Material.ARROW).addItemFlags(ItemFlag.HIDE_ATTRIBUTES).build();
    }

    private void handleNavigationClick(final @NotNull RenderContext renderContext, final int deltaX, final int deltaY, final @NotNull String direction) {
        final int currentX = this.offsetX.get(renderContext);
        final int currentY = this.offsetY.get(renderContext);
        this.offsetX.set(currentX + deltaX, renderContext);
        this.offsetY.set(currentY + deltaY, renderContext);
    }

    private void renderUtilityButtons(final @NotNull RenderContext renderContext, final @NotNull Player player, final boolean previewMode) {
        this.renderBackButton(renderContext, player, previewMode);
        this.renderCenterViewButton(renderContext, player, previewMode);
    }

    private void renderBackButton(final @NotNull RenderContext renderContext, final @NotNull Player player, final boolean previewMode) {
        try {
            renderContext.slot(BACK_SLOT)
                    .renderWith(() -> this.createBackButtonItem(player, previewMode))
                    .onClick(SlotClickContext::back);
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to render back button", exception);
        }
    }

    private @NotNull ItemStack createBackButtonItem(final @NotNull Player player, final boolean previewMode) {
        try {
            final ItemStack backItem = new Return().getHead(player);
            if (previewMode) {
                final List<Component> lore = new ArrayList<>();
                lore.addAll(this.i18n("back.lore", player).build().splitLines());
                lore.addAll(this.i18n("preview_mode.lore", player).build().splitLines());
                return UnifiedBuilderFactory.item(backItem).addItemFlags(ItemFlag.HIDE_ATTRIBUTES).setLore(lore).build();
            }
            return backItem;
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to create back button item", exception);
            return new Return().getHead(player);
        }
    }

    private void renderCenterViewButton(final @NotNull RenderContext renderContext, final @NotNull Player player, final boolean previewMode) {
        try {
            renderContext.slot(CENTER_SLOT)
                    .renderWith(() -> this.createCenterViewButtonItem(player, previewMode))
                    .onClick(clickContext -> this.handleCenterViewClick(renderContext));
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to render center view button", exception);
        }
    }

    private @NotNull ItemStack createCenterViewButtonItem(final @NotNull Player player, final boolean previewMode) {
        try {
            return UnifiedBuilderFactory.item(Material.COMPASS)
                    .setName(this.i18n("center_view.name", player).build().component())
                    .setLore(this.i18n("center_view.lore", player).build().splitLines())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to create center view button item", exception);
            return UnifiedBuilderFactory.item(Material.COMPASS)
                    .setName(this.i18n("center_view.fallback", player).build().component())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        }
    }

    private void handleCenterViewClick(final @NotNull RenderContext renderContext) {
        LOGGER.log(Level.FINE, "Resetting view to center position");
        this.offsetX.set(0, renderContext);
        this.offsetY.set(0, renderContext);
    }

    private void renderPreviewModeIndicator(final @NotNull RenderContext renderContext, final @NotNull Player player) {
        try {
            renderContext.slot(PREVIEW_SLOT)
                    .renderWith(() -> this.createPreviewModeIndicatorItem(player));
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to render preview mode indicator", exception);
        }
    }

    private @NotNull ItemStack createPreviewModeIndicatorItem(final @NotNull Player player) {
        try {
            return UnifiedBuilderFactory.item(PREVIEW_MATERIAL)
                    .setName(this.i18n("preview_mode.name", player).build().component())
                    .setLore(this.i18n("preview_mode.lore", player).build().splitLines())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to create preview mode indicator item", exception);
            return UnifiedBuilderFactory.item(PREVIEW_MATERIAL)
                    .setName(this.i18n("preview_mode.fallback", player).build().component())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        }
    }

    private void renderDynamicRankGrid(final @NotNull RenderContext renderContext, final @NotNull Player player, final @NotNull RRankTree rankTree, final boolean previewMode) {
        try {
            final Map<String, RankNode>     rankNodeHierarchy    = this.nodeGraph.get(renderContext);
            final Map<String, GridPosition> worldPositionMapping = this.nodeWorldPositions.get(renderContext);
            LOGGER.log(Level.FINE, "Rendering dynamic grid with cached data: " + rankNodeHierarchy.size() + " nodes");
            for (final Integer slotNumber : VALID_SLOTS) {
                this.bindSlotToDynamicContent(renderContext, player, rankTree, slotNumber, rankNodeHierarchy, worldPositionMapping, previewMode);
            }
            LOGGER.log(Level.FINE, "Dynamic rank grid rendered successfully");
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to render dynamic rank grid", exception);
            this.renderFallbackGrid(renderContext, player, rankTree);
        }
    }

    private void bindSlotToDynamicContent(final @NotNull RenderContext renderContext, final @NotNull Player player, final @NotNull RRankTree rankTree, final int slotNumber, final @NotNull Map<String, RankNode> rankNodeHierarchy, final @NotNull Map<String, GridPosition> worldPositionMapping, final boolean previewMode) {
        renderContext.slot(slotNumber)
                .renderWith(() -> this.createDynamicSlotContent(slotNumber, renderContext, player, rankTree, rankNodeHierarchy, worldPositionMapping, previewMode))
                .updateOnStateChange(this.offsetX, this.offsetY, this.lastRefreshAt)
                .onClick(clickContext -> this.handleDynamicSlotClick(clickContext, slotNumber, renderContext, rankNodeHierarchy, worldPositionMapping));
    }

    private @NotNull ItemStack createDynamicSlotContent(final int slotNumber, final @NotNull RenderContext renderContext, final @NotNull Player player, final @NotNull RRankTree rankTree, final @NotNull Map<String, RankNode> rankNodeHierarchy, final @NotNull Map<String, GridPosition> worldPositionMapping, final boolean previewMode) {
        try {
            final int          offsetX          = this.offsetX.get(renderContext);
            final int          offsetY          = this.offsetY.get(renderContext);
            final GridPosition slotGridPosition = this.findGridPositionForSlot(slotNumber);
            if (slotGridPosition == null) {
                return this.createBackgroundPane(player);
            }
            final GridPosition worldPosition = new GridPosition(slotGridPosition.x() - offsetX, slotGridPosition.y() - offsetY);
            final String rankIdAtPosition = this.findRankIdAtWorldPosition(worldPosition, worldPositionMapping);
            if (rankIdAtPosition != null) {
                final RankNode rankNode = rankNodeHierarchy.get(rankIdAtPosition);
                if (rankNode != null) {
                    return this.createRankDisplayItem(player, rankNode, renderContext, previewMode);
                }
            }
            final ItemStack connectionItem = this.createConnectionLineItem(slotGridPosition, offsetX, offsetY, worldPositionMapping, rankNodeHierarchy, player, renderContext, previewMode);
            if (connectionItem != null) {
                return connectionItem;
            }
            return this.createBackgroundPane(player);
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to create dynamic slot content for slot " + slotNumber, exception);
            return this.createBackgroundPane(player);
        }
    }

    private @Nullable GridPosition findGridPositionForSlot(final int slotNumber) {
        for (final Map.Entry<GridPosition, Integer> entry : SLOT_MAPPING.entrySet()) {
            if (entry.getValue().equals(slotNumber)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private @Nullable String findRankIdAtWorldPosition(final @NotNull GridPosition worldPosition, final @NotNull Map<String, GridPosition> worldPositionMapping) {
        for (final Map.Entry<String, GridPosition> entry : worldPositionMapping.entrySet()) {
            if (entry.getValue().equals(worldPosition)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private @NotNull ItemStack createBackgroundPane(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(BACKGROUND_MATERIAL)
                .setName(this.i18n("background.name", player).build().component())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @Nullable ItemStack createConnectionLineItem(final @NotNull GridPosition slotPosition, final int offsetX, final int offsetY, final @NotNull Map<String, GridPosition> worldPositionMapping, final @NotNull Map<String, RankNode> rankNodeHierarchy, final @NotNull Player player, final @NotNull RenderContext renderContext, final boolean previewMode) {
        try {
            final GridPosition worldPosition = new GridPosition(slotPosition.x() - offsetX, slotPosition.y() - offsetY);
            for (final Map.Entry<String, RankNode> entry : rankNodeHierarchy.entrySet()) {
                final RankNode     parentNode          = entry.getValue();
                final GridPosition parentWorldPosition = worldPositionMapping.get(entry.getKey());
                if (parentWorldPosition == null) continue;
                for (final RankNode childNode : parentNode.getChildren()) {
                    final GridPosition childWorldPosition = worldPositionMapping.get(childNode.getRank().getIdentifier());
                    if (childWorldPosition != null && this.isConnectionPosition(worldPosition, parentWorldPosition, childWorldPosition, parentNode, worldPositionMapping)) {
                        return this.createConnectionLineDisplayItem(parentNode, childNode, player, renderContext, previewMode);
                    }
                }
            }
            return null;
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to create connection line item", exception);
            return null;
        }
    }

    private boolean isConnectionPosition(final @NotNull GridPosition position, final @NotNull GridPosition parentPosition, final @NotNull GridPosition childPosition, final @NotNull RankNode parentNode, final @NotNull Map<String, GridPosition> worldPositionMapping) {
        if (parentNode.getChildren().size() == 1) {
            return this.isDirectConnectionPosition(position, parentPosition, childPosition);
        }
        return this.isBalancedBranchingConnectionPosition(position, parentPosition, childPosition, parentNode, worldPositionMapping);
    }

    private boolean isDirectConnectionPosition(final @NotNull GridPosition position, final @NotNull GridPosition parentPosition, final @NotNull GridPosition childPosition) {
        if (parentPosition.x() == childPosition.x() && parentPosition.x() == position.x()) {
            final int minY = Math.min(parentPosition.y(), childPosition.y());
            final int maxY = Math.max(parentPosition.y(), childPosition.y());
            return position.y() > minY && position.y() < maxY;
        }
        if (parentPosition.y() == childPosition.y() && parentPosition.y() == position.y()) {
            final int minX = Math.min(parentPosition.x(), childPosition.x());
            final int maxX = Math.max(parentPosition.x(), childPosition.x());
            return position.x() > minX && position.x() < maxX;
        }
        final int deltaX = Math.abs(parentPosition.x() - childPosition.x());
        final int deltaY = Math.abs(parentPosition.y() - childPosition.y());
        if (deltaX == GRID_STEP && deltaY == GRID_STEP) {
            final int stepX = Integer.signum(childPosition.x() - parentPosition.x());
            final int stepY = Integer.signum(childPosition.y() - parentPosition.y());
            for (int step = 1; step < GRID_STEP; step++) {
                final int connectionX = parentPosition.x() + (stepX * step);
                final int connectionY = parentPosition.y() + (stepY * step);
                if (position.x() == connectionX && position.y() == connectionY) {
                    return true;
                }
            }
        }
        return false;
    }

    private @NotNull Map<String, RankNode> buildRankNodeHierarchy(final @NotNull RRankTree rankTree) {
        final Map<String, RankNode> nodeHierarchy = new HashMap<>();
        try {
            for (final RRank rank : rankTree.getRanks()) {
                nodeHierarchy.put(rank.getIdentifier(), new RankNode(rank));
            }
            for (final RRank rank : rankTree.getRanks()) {
                final RankNode currentNode = nodeHierarchy.get(rank.getIdentifier());
                if (currentNode != null) {
                    for (final String nextRankId : rank.getNextRanks()) {
                        final RankNode childNode = nodeHierarchy.get(nextRankId);
                        if (childNode != null) {
                            currentNode.addChild(childNode);
                            childNode.addParent(currentNode);
                        }
                    }
                }
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Error building rank node hierarchy", exception);
        }
        return nodeHierarchy;
    }

    private @NotNull Map<String, GridPosition> calculateRankWorldPositions(final @NotNull Map<String, RankNode> rankNodeHierarchy) {
        final Map<String, GridPosition> worldPositions  = new HashMap<>();
        final Set<String>               positionedRanks = new HashSet<>();
        try {
            final List<RankNode> initialRanks = this.findInitialRanks(rankNodeHierarchy);
            if (initialRanks.isEmpty()) {
                LOGGER.log(Level.WARNING, "No initial ranks found in hierarchy");
                return worldPositions;
            }
            LOGGER.log(Level.FINE, "Found " + initialRanks.size() + " initial ranks");
            if (initialRanks.size() == 1) {
                this.positionSingleInitialRank(initialRanks.get(0), worldPositions, positionedRanks, rankNodeHierarchy);
            } else {
                this.positionMultipleInitialRanks(initialRanks, worldPositions, positionedRanks, rankNodeHierarchy);
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Error calculating rank world positions", exception);
        }
        return worldPositions;
    }

    private @NotNull List<RankNode> findInitialRanks(final @NotNull Map<String, RankNode> rankNodeHierarchy) {
        return rankNodeHierarchy.values().stream()
                .filter(node -> node.getParents().isEmpty() || node.getRank().isInitialRank())
                .sorted(Comparator.comparingInt(node -> node.getRank().getTier()))
                .toList();
    }

    private void positionSingleInitialRank(final @NotNull RankNode initialRank, final @NotNull Map<String, GridPosition> worldPositions, final @NotNull Set<String> positionedRanks, final @NotNull Map<String, RankNode> rankNodeHierarchy) {
        worldPositions.put(initialRank.getRank().getIdentifier(), ROOT_ANCHOR);
        positionedRanks.add(initialRank.getRank().getIdentifier());
        final Integer slotNumber = SLOT_MAPPING.get(ROOT_ANCHOR);
        LOGGER.log(Level.FINE, "Positioned single initial rank '" + initialRank.getRank().getIdentifier() + "' at center: " + ROOT_ANCHOR + " (slot " + slotNumber + ")");
        this.positionRankChildren(initialRank, ROOT_ANCHOR, worldPositions, positionedRanks, rankNodeHierarchy);
    }

    private void positionMultipleInitialRanks(final @NotNull List<RankNode> initialRanks, final @NotNull Map<String, GridPosition> worldPositions, final @NotNull Set<String> positionedRanks, final @NotNull Map<String, RankNode> rankNodeHierarchy) {
        final int startX = ROOT_ANCHOR.x() - ((initialRanks.size() - 1) * GRID_STEP / 2);
        for (int i = 0; i < initialRanks.size(); i++) {
            final RankNode initialRank = initialRanks.get(i);
            final GridPosition position = new GridPosition(startX + (i * GRID_STEP), ROOT_ANCHOR.y());
            worldPositions.put(initialRank.getRank().getIdentifier(), position);
            positionedRanks.add(initialRank.getRank().getIdentifier());
            final Integer slotNumber = SLOT_MAPPING.get(position);
            LOGGER.log(Level.FINE, "Positioned initial rank '" + initialRank.getRank().getIdentifier() + "' at: " + position + " (slot " + slotNumber + ")");
            this.positionRankChildren(initialRank, position, worldPositions, positionedRanks, rankNodeHierarchy);
        }
    }

    private void positionRankChildren(final @NotNull RankNode parentNode, final @NotNull GridPosition parentPosition, final @NotNull Map<String, GridPosition> worldPositions, final @NotNull Set<String> positionedRanks, final @NotNull Map<String, RankNode> allNodes) {
        if (parentNode.getChildren().isEmpty()) {
            return;
        }
        final List<RankNode> sortedChildren = new ArrayList<>(parentNode.getChildren());
        sortedChildren.sort(Comparator.comparingInt(node -> node.getRank().getTier()));
        if (sortedChildren.size() == 1) {
            this.positionSingleChild(sortedChildren.get(0), parentPosition, worldPositions, positionedRanks, allNodes);
        } else {
            this.positionMultipleChildren(sortedChildren, parentPosition, worldPositions, positionedRanks, allNodes);
        }
    }

    private void positionSingleChild(final @NotNull RankNode childNode, final @NotNull GridPosition parentPosition, final @NotNull Map<String, GridPosition> worldPositions, final @NotNull Set<String> positionedRanks, final @NotNull Map<String, RankNode> allNodes) {
        if (!positionedRanks.contains(childNode.getRank().getIdentifier())) {
            final GridPosition childPosition = new GridPosition(parentPosition.x(), parentPosition.y() + GRID_STEP);
            worldPositions.put(childNode.getRank().getIdentifier(), childPosition);
            positionedRanks.add(childNode.getRank().getIdentifier());
            final Integer slotNumber = SLOT_MAPPING.get(childPosition);
            LOGGER.log(Level.FINE, "Positioned single child '" + childNode.getRank().getIdentifier() + "' below parent at: " + childPosition + " (slot " + slotNumber + ")");
            this.positionRankChildren(childNode, childPosition, worldPositions, positionedRanks, allNodes);
        }
    }

    private void positionMultipleChildren(final @NotNull List<RankNode> children, final @NotNull GridPosition parentPosition, final @NotNull Map<String, GridPosition> worldPositions, final @NotNull Set<String> positionedRanks, final @NotNull Map<String, RankNode> allNodes) {
        final int childY     = parentPosition.y() + GRID_STEP;
        final int totalWidth = (children.size() - 1) * GRID_STEP;
        final int startX     = parentPosition.x() - (totalWidth / 2);
        for (int i = 0; i < children.size(); i++) {
            final RankNode child = children.get(i);
            if (!positionedRanks.contains(child.getRank().getIdentifier())) {
                final GridPosition childPosition = new GridPosition(startX + (i * GRID_STEP), childY);
                worldPositions.put(child.getRank().getIdentifier(), childPosition);
                positionedRanks.add(child.getRank().getIdentifier());
                final Integer slotNumber = SLOT_MAPPING.get(childPosition);
                LOGGER.log(Level.FINE, "Positioned child '" + child.getRank().getIdentifier() + "' at balanced position: " + childPosition + " (slot " + slotNumber + ")");
                this.positionRankChildren(child, childPosition, worldPositions, positionedRanks, allNodes);
            }
        }
    }

    private @NotNull ItemStack createRankDisplayItem(final @NotNull Player player, final @NotNull RankNode rankNode, final @NotNull RenderContext renderContext, final boolean previewMode) {
        try {
            final RRank                   rank           = rankNode.getRank();
            final Map<String, ERankStatus> cachedStatuses = this.nodeStatuses.get(renderContext);
            final ERankStatus status = cachedStatuses.getOrDefault(rank.getIdentifier(), ERankStatus.LOCKED);
            final Material iconMaterial = this.extractRankIconMaterial(rank);
            final List<Component> lore = this.buildRankDisplayLore(player, rank, status, previewMode);
            final Component displayName = this.extractRankDisplayName(player, rank);
            ItemStack baseItem = UnifiedBuilderFactory.item(iconMaterial)
                    .setName(displayName)
                    .setLore(lore)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
            if (status == ERankStatus.OWNED && !previewMode) {
                baseItem = UnifiedBuilderFactory.item(baseItem).addItemFlags(ItemFlag.HIDE_ATTRIBUTES).setGlowing(true).build();
            }
            return baseItem;
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to create rank display item", exception);
            return UnifiedBuilderFactory.item(Material.STONE)
                    .setName(this.i18n("error.render_rank", player).build().component())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        }
    }

    private @NotNull Material extractRankIconMaterial(final @NotNull RRank rank) {
        try {
            return Material.valueOf(rank.getIcon().getMaterial());
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Invalid material for rank " + rank.getIdentifier() + ": " + rank.getIcon().getMaterial(), exception);
            return Material.STONE;
        }
    }

    private @NotNull Component extractRankDisplayName(final @NotNull Player player, final @NotNull RRank rank) {
        try {
            return TranslationService.create(TranslationKey.of(rank.getDisplayNameKey()), player).build().component();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to extract localized rank display name", exception);
            return this.i18n("rank.fallback_name", player).with("rank_id", rank.getIdentifier()).build().component();
        }
    }

    private @NotNull List<Component> buildRankDisplayLore(final @NotNull Player player, final @NotNull RRank rank, final @NotNull ERankStatus status, final boolean previewMode) {
        final List<Component> lore = new ArrayList<>();
        try {
            lore.addAll(this.i18n(rank.getDescriptionKey(), player).build().splitLines());
            lore.add(Component.empty());
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to extract localized rank description", exception);
            lore.add(this.i18n("rank.fallback_description", player).with("rank_id", rank.getIdentifier()).build().component());
            lore.add(Component.empty());
        }
        lore.add(this.createStatusComponent(status, player));
        lore.add(Component.empty());
        lore.add(this.i18n("rank.tier", player).with("tier", rank.getTier()).build().component());
        lore.add(this.i18n("rank.weight", player).with("weight", rank.getWeight()).build().component());
        if (!previewMode) {
            lore.add(Component.empty());
            this.addClickInstructions(lore, status, player);
        }
        if (previewMode) {
            lore.add(Component.empty());
            lore.addAll(this.i18n("preview_mode.lore", player).build().splitLines());
        }
        return lore;
    }

    private @NotNull Component createStatusComponent(final @NotNull ERankStatus status, final @NotNull Player player) {
        return switch (status) {
            case OWNED -> this.i18n("status.owned", player).build().component();
            case AVAILABLE -> this.i18n("status.available", player).build().component();
            case IN_PROGRESS -> this.i18n("status.pending", player).build().component();
            case LOCKED -> this.i18n("status.locked", player).build().component();
        };
    }

    private void addClickInstructions(final @NotNull List<Component> lore, final @NotNull ERankStatus status, final @NotNull Player player) {
        switch (status) {
            case AVAILABLE -> {
                lore.add(this.i18n("click.left_start", player).build().component());
                lore.add(this.i18n("click.right_requirements", player).build().component());
            }
            case IN_PROGRESS -> {
                lore.add(this.i18n("click.left_redeem", player).build().component());
                lore.add(this.i18n("click.right_progress", player).build().component());
            }
        }
    }

    private void renderFallbackGrid(final @NotNull RenderContext renderContext, final @NotNull Player player, final @NotNull RRankTree rankTree) {
        try {
            final List<RRank> ranks = new ArrayList<>(rankTree.getRanks());
            ranks.sort(Comparator.comparingInt(RRank::getTier));
            final List<Integer> centerSlots = List.of(22, 21, 23, 13, 31);
            for (int i = 0; i < Math.min(ranks.size(), centerSlots.size()); i++) {
                final RRank rank = ranks.get(i);
                final ItemStack item = UnifiedBuilderFactory.item(Material.STONE)
                        .setName(this.i18n("fallback.rank", player).with("rank_id", rank.getIdentifier()).build().component())
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build();
                renderContext.slot(centerSlots.get(i)).renderWith(() -> item);
            }
            for (final Integer slot : VALID_SLOTS) {
                if (!centerSlots.subList(0, Math.min(ranks.size(), centerSlots.size())).contains(slot)) {
                    renderContext.slot(slot).renderWith(() -> this.createBackgroundPane(player));
                }
            }
            LOGGER.log(Level.INFO, "Rendered fallback grid with " + Math.min(ranks.size(), centerSlots.size()) + " ranks");
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Fallback grid rendering failed", exception);
        }
    }

    private void renderErrorState(final @NotNull RenderContext renderContext, final @NotNull Player player) {
        try {
            final ItemStack errorItem = UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("error.no_rank_tree", player).build().component())
                    .setLore(this.i18n("error.no_rank_tree_lore", player).build().splitLines())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
            renderContext.slot(22).renderWith(() -> errorItem);
            for (final Integer slot : VALID_SLOTS) {
                if (slot != 22) {
                    renderContext.slot(slot).renderWith(() -> this.createBackgroundPane(player));
                }
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to render error state", exception);
        }
    }

    private void renderCriticalErrorState(final @NotNull RenderContext renderContext, final @NotNull Player player) {
        try {
            final ItemStack errorItem = UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("error.critical", player).build().component())
                    .setLore(this.i18n("error.critical.lore", player).build().splitLines())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
            renderContext.slot(22).renderWith(() -> errorItem);
            for (final Integer slot : VALID_SLOTS) {
                if (slot != 22) {
                    renderContext.slot(slot).renderWith(() -> this.createBackgroundPane(player));
                }
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Critical error state rendering failed", exception);
        }
    }

    private @Nullable RPlayerRank getPlayerRankForTree(final @NotNull RDQ plugin, final @NotNull RDQPlayer rdqPlayer, final @NotNull RRankTree rankTree) {
        try {
            final List<RPlayerRank> playerRanks = plugin.getPlayerRankRepository().findListByAttributes(Map.of("player.uniqueId", rdqPlayer.getUniqueId()));
            return playerRanks.stream()
                    .filter(rank -> Objects.equals(rank.getRankTree(), rankTree))
                    .findFirst()
                    .orElse(null);
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to get player rank for tree", exception);
            return null;
        }
    }

    private static @NotNull Map<GridPosition, Integer> createSlotMapping() {
        final Map<GridPosition, Integer> mapping = new HashMap<>();
        mapping.put(new GridPosition(0, 0), 1);
        mapping.put(new GridPosition(1, 0), 2);
        mapping.put(new GridPosition(2, 0), 3);
        mapping.put(new GridPosition(3, 0), 5);
        mapping.put(new GridPosition(4, 0), 6);
        mapping.put(new GridPosition(5, 0), 7);
        mapping.put(new GridPosition(0, 1), 10);
        mapping.put(new GridPosition(1, 1), 11);
        mapping.put(new GridPosition(2, 1), 12);
        mapping.put(new GridPosition(3, 1), 13);
        mapping.put(new GridPosition(4, 1), 14);
        mapping.put(new GridPosition(5, 1), 15);
        mapping.put(new GridPosition(6, 1), 16);
        mapping.put(new GridPosition(0, 2), 19);
        mapping.put(new GridPosition(1, 2), 20);
        mapping.put(new GridPosition(2, 2), 21);
        mapping.put(new GridPosition(3, 2), 22);
        mapping.put(new GridPosition(4, 2), 23);
        mapping.put(new GridPosition(5, 2), 24);
        mapping.put(new GridPosition(6, 2), 25);
        mapping.put(new GridPosition(0, 3), 28);
        mapping.put(new GridPosition(1, 3), 29);
        mapping.put(new GridPosition(2, 3), 30);
        mapping.put(new GridPosition(3, 3), 31);
        mapping.put(new GridPosition(4, 3), 32);
        mapping.put(new GridPosition(5, 3), 33);
        mapping.put(new GridPosition(6, 3), 34);
        mapping.put(new GridPosition(0, 4), 37);
        mapping.put(new GridPosition(1, 4), 38);
        mapping.put(new GridPosition(2, 4), 39);
        mapping.put(new GridPosition(3, 4), 40);
        mapping.put(new GridPosition(4, 4), 41);
        mapping.put(new GridPosition(5, 4), 42);
        mapping.put(new GridPosition(6, 4), 43);
        return mapping;
    }

    private static @NotNull List<Integer> createValidSlots() {
        final List<Integer> slots = new ArrayList<>();
        slots.addAll(List.of(1, 2, 3, 5, 6, 7));
        slots.addAll(List.of(10, 11, 12, 13, 14, 15, 16));
        slots.addAll(List.of(19, 20, 21, 22, 23, 24, 25));
        slots.addAll(List.of(28, 29, 30, 31, 32, 33, 34));
        slots.addAll(List.of(37, 38, 39, 40, 41, 42, 43));
        return slots;
    }

    private boolean isBalancedBranchingConnectionPosition(final @NotNull GridPosition position, final @NotNull GridPosition parentPosition, final @NotNull GridPosition childPosition, final @NotNull RankNode parentNode, final @NotNull Map<String, GridPosition> worldPositionMapping) {
        final List<GridPosition> childPositions = this.extractChildPositions(parentNode, worldPositionMapping);
        if (childPositions.isEmpty()) {
            return false;
        }
        childPositions.sort(Comparator.comparingInt(p -> p.x()));
        final int branchingDistance = GRID_STEP / 2;
        final GridPosition branchingPoint = new GridPosition(parentPosition.x(), parentPosition.y() + branchingDistance);
        final GridPosition leftmostChild  = childPositions.get(0);
        final GridPosition rightmostChild = childPositions.get(childPositions.size() - 1);
        if (position.x() == parentPosition.x() && position.y() > parentPosition.y() && position.y() < branchingPoint.y()) {
            return true;
        }
        if (position.y() == branchingPoint.y()) {
            final int minX = Math.min(leftmostChild.x(), rightmostChild.x());
            final int maxX = Math.max(leftmostChild.x(), rightmostChild.x());
            final int centerX   = (minX + maxX) / 2;
            final int halfWidth = (maxX - minX) / 2;
            if (position.x() >= centerX - halfWidth && position.x() <= centerX + halfWidth) {
                return true;
            }
        }
        for (final GridPosition childPos : childPositions) {
            if (position.x() == childPos.x() && position.y() > branchingPoint.y() && position.y() < childPos.y()) {
                return true;
            }
        }
        return false;
    }

    private @NotNull List<GridPosition> extractChildPositions(final @NotNull RankNode parentNode, final @NotNull Map<String, GridPosition> worldPositionMapping) {
        final List<GridPosition> childPositions = new ArrayList<>();
        for (final RankNode childNode : parentNode.getChildren()) {
            final GridPosition childPosition = worldPositionMapping.get(childNode.getRank().getIdentifier());
            if (childPosition != null) {
                childPositions.add(childPosition);
            }
        }
        return childPositions;
    }

    private @NotNull ItemStack createConnectionLineDisplayItem(final @NotNull RankNode parentNode, final @NotNull RankNode childNode, final @NotNull Player player, final @NotNull RenderContext renderContext, final boolean previewMode) {
        try {
            final Map<String, ERankStatus> cachedStatuses = this.nodeStatuses.get(renderContext);
            final ERankStatus childStatus = cachedStatuses.getOrDefault(childNode.getRank().getIdentifier(), ERankStatus.LOCKED);
            final Material connectionMaterial = this.getConnectionMaterialForStatus(childStatus);
            final Component connectionName = this.getConnectionNameForStatus(childStatus, player);
            final List<Component> lore = new ArrayList<>();
            lore.add(this.i18n("connection.from", player).with("rank_name", parentNode.getRank().getIdentifier()).build().component());
            lore.add(this.i18n("connection.to", player).with("rank_name", childNode.getRank().getIdentifier()).build().component());
            if (previewMode) {
                lore.addAll(this.i18n("preview_mode.lore", player).build().splitLines());
            }
            return UnifiedBuilderFactory.item(connectionMaterial)
                    .setName(connectionName)
                    .setLore(lore)
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to create connection line display item", exception);
            return UnifiedBuilderFactory.item(CONNECTION_MATERIAL)
                    .setName(this.i18n("connection.fallback", player).build().component())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        }
    }

    private @NotNull Material getConnectionMaterialForStatus(final @NotNull ERankStatus status) {
        return switch (status) {
            case OWNED -> OWNED_MATERIAL;
            case AVAILABLE -> AVAILABLE_MATERIAL;
            case IN_PROGRESS -> PENDING_MATERIAL;
            default -> LOCKED_MATERIAL;
        };
    }

    private @NotNull Component getConnectionNameForStatus(final @NotNull ERankStatus status, final @NotNull Player player) {
        return switch (status) {
            case OWNED -> this.i18n("connection.owned", player).build().component();
            case AVAILABLE -> this.i18n("connection.available", player).build().component();
            case IN_PROGRESS -> this.i18n("connection.pending", player).build().component();
            default -> this.i18n("connection.locked", player).build().component();
        };
    }

    private void handleDynamicSlotClick(final @NotNull SlotClickContext clickContext, final int slotNumber, final @NotNull RenderContext renderContext, final @NotNull Map<String, RankNode> rankNodeHierarchy, final @NotNull Map<String, GridPosition> worldPositionMapping) {
        try {
            final int          offsetX          = this.offsetX.get(renderContext);
            final int          offsetY          = this.offsetY.get(renderContext);
            final GridPosition slotGridPosition = this.findGridPositionForSlot(slotNumber);
            if (slotGridPosition == null) return;
            final GridPosition worldPosition = new GridPosition(slotGridPosition.x() - offsetX, slotGridPosition.y() - offsetY);
            final String rankIdAtPosition = this.findRankIdAtWorldPosition(worldPosition, worldPositionMapping);
            if (rankIdAtPosition != null) {
                final RankNode rankNode = rankNodeHierarchy.get(rankIdAtPosition);
                if (rankNode != null) {
                    this.handleRankNodeClick(clickContext, rankNode, renderContext);
                }
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to handle dynamic slot click", exception);
        }
    }

    private void handleRankNodeClick(final @NotNull SlotClickContext clickContext, final @NotNull RankNode rankNode, final @NotNull RenderContext renderContext) {
        final Player                   player             = clickContext.getPlayer();
        final boolean                  currentPreviewMode = this.isPreviewMode.get(clickContext);
        final Map<String, ERankStatus> cachedStatuses     = this.nodeStatuses.get(renderContext);
        final ERankStatus rankStatus = cachedStatuses.getOrDefault(rankNode.getRank().getIdentifier(), ERankStatus.LOCKED);
        final ClickType clickType = clickContext.getClickOrigin().getClick();
        LOGGER.log(Level.FINE, "Handling rank click: " + rankNode.getRank().getIdentifier() + " - Preview: " + currentPreviewMode + " - Status: " + rankStatus + " - Click: " + clickType);
        if (currentPreviewMode) {
            this.handlePreviewModeRankClick(player, rankNode.getRank());
            return;
        }
        switch (rankStatus) {
            case OWNED -> this.handleOwnedRankClick(player, rankNode.getRank());
            case AVAILABLE -> this.handleAvailableRankClick(clickContext, rankNode, clickType);
            case IN_PROGRESS -> this.handlePendingRankClick(clickContext, rankNode, clickType);
            case LOCKED -> this.handleLockedRankClick(player, rankNode.getRank());
        }
    }

    private void handlePreviewModeRankClick(final @NotNull Player player, final @NotNull RRank rank) {
        try {
            this.i18n("messages.preview_rank_click", player)
                    .with("rank_name", rank.getIdentifier())
                    .withPrefix()
                    .send();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to send preview mode rank click message", exception);
        }
    }

    private void handleOwnedRankClick(final @NotNull Player player, final @NotNull RRank rank) {
        try {
            this.i18n("messages.rank_owned", player)
                    .with("rank_name", rank.getIdentifier())
                    .withPrefix()
                    .send();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to send owned rank click message", exception);
        }
    }

    private void handleAvailableRankClick(final @NotNull SlotClickContext clickContext, final @NotNull RankNode rankNode, final @NotNull ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            this.startRankProgression(clickContext, rankNode);
        } else if (clickType == ClickType.RIGHT) {
            this.openRankRequirementsView(clickContext, rankNode);
        }
    }

    private void handlePendingRankClick(final @NotNull SlotClickContext clickContext, final @NotNull RankNode rankNode, final @NotNull ClickType clickType) {
        if (clickType == ClickType.LEFT) {
            this.attemptRankRedemption(clickContext, rankNode);
        } else if (clickType == ClickType.RIGHT) {
            this.openRankRequirementsView(clickContext, rankNode);
        }
    }

    private void handleLockedRankClick(final @NotNull Player player, final @NotNull RRank rank) {
        try {
            this.i18n("messages.rank_locked", player)
                    .with("rank_name", rank.getIdentifier())
                    .withPrefix()
                    .send();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to send locked rank click message", exception);
        }
    }

    private void startRankProgression(final @NotNull SlotClickContext clickContext, final @NotNull RankNode rankNode) {
        try {
            final Player player = clickContext.getPlayer();
            final RDQPlayer rdqPlayer = this.currentPlayer.get(clickContext);
            final RRank                        rank                = rankNode.getRank();
            final Set<RRankUpgradeRequirement> upgradeRequirements = rank.getUpgradeRequirements();
            if (upgradeRequirements.isEmpty()) {
                this.sendNoRequirementsMessage(player, rank);
                this.redeemRank(clickContext, rankNode);
                return;
            }
            if (this.progressManager == null) {
                this.progressManager = new RankRequirementProgressManager(this.rdq.get(clickContext));
            }
            final boolean allRequirementsCompleted = progressManager.areAllRequirementsCompleted(player, rdqPlayer, rank);
            if (allRequirementsCompleted) {
                LOGGER.log(Level.INFO, "All requirements already completed for rank " + rank.getIdentifier() + ", directly redeeming for player " + player.getName());
                this.redeemRank(clickContext, rankNode);
                return;
            }
            progressManager.initializeRankProgressTracking(rdqPlayer, rank);
            this.sendProgressStartedMessage(player, rank, upgradeRequirements.size());
            this.refreshCachedData(clickContext);
            LOGGER.log(Level.INFO, "Started rank progression for player " + player.getName() + " on rank " + rank.getIdentifier());
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to start rank progression for " + rankNode.getRank().getIdentifier(), exception);
            this.sendProgressStartErrorMessage(clickContext.getPlayer(), rankNode.getRank());
        }
    }

    private void attemptRankRedemption(final @NotNull SlotClickContext clickContext, final @NotNull RankNode rankNode) {
        try {
            final Player player = clickContext.getPlayer();
            final RDQPlayer rdqPlayer = this.currentPlayer.get(clickContext);
            final RRank rank = rankNode.getRank();
            if (this.progressManager == null) {
                this.progressManager = new RankRequirementProgressManager(this.rdq.get(clickContext));
            }
            final boolean allRequirementsCompleted = progressManager.areAllRequirementsCompleted(player, rdqPlayer, rank);
            if (allRequirementsCompleted) {
                this.redeemRank(clickContext, rankNode);
            } else {
                this.sendRequirementsNotCompletedMessage(player, rank);
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to attempt rank redemption for " + rankNode.getRank().getIdentifier(), exception);
            this.i18n("error.general", clickContext.getPlayer()).send();
        }
    }

    private void redeemRank(final @NotNull SlotClickContext clickContext, final @NotNull RankNode rankNode) {
        final Player player = clickContext.getPlayer();
        try {
            final RDQ plugin = this.rdq.get(clickContext);
            final RDQPlayer rdqPlayer = this.currentPlayer.get(clickContext);
            final RRankTree rankTree = this.selectedRankTree.get(clickContext);
            final RRank newRank = rankNode.getRank();
            LOGGER.log(Level.INFO, "Starting rank redemption for player " + player.getName() + " to rank " + newRank.getIdentifier());
            this.handleRankRedemptionForTree(plugin, rdqPlayer, rankTree, newRank);
            this.handleLuckPermsRankAssignment(plugin, player, newRank);
            this.refreshCachedData(clickContext);
            this.sendRankRedeemedMessage(player, newRank);
            LOGGER.log(Level.INFO, "Successfully redeemed rank " + newRank.getIdentifier() + " for player " + player.getName());
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to redeem rank " + rankNode.getRank().getIdentifier() + " for player " + player.getName(), exception);
            this.sendRankRedemptionErrorMessage(player, rankNode.getRank());
        }
    }

    private void handleRankRedemptionForTree(final @NotNull RDQ plugin, final @NotNull RDQPlayer rdqPlayer, final @NotNull RRankTree rankTree, final @NotNull RRank newRank) {
        try {
            RPlayerRank existingRankInTree = this.getPlayerRankForTree(plugin, rdqPlayer, rankTree);
            if (existingRankInTree != null) {
                RPlayerRank freshRank = plugin.getPlayerRankRepository().findById(existingRankInTree.getId());
                if (freshRank != null) {
                    freshRank.setCurrentRank(newRank);
                    freshRank.setActive(true);
                    plugin.getPlayerRankRepository().update(freshRank);
                    LOGGER.info("Updated existing rank for player " + rdqPlayer.getPlayerName() + " to " + newRank.getIdentifier() + " in tree " + rankTree.getIdentifier());
                }
            } else {
                final RPlayerRank newPlayerRank = new RPlayerRank(rdqPlayer, newRank, rankTree);
                rdqPlayer.addPlayerRank(newPlayerRank);
                plugin.getPlayerRankRepository().create(newPlayerRank);
                LOGGER.info("Created new rank assignment for player " + rdqPlayer.getPlayerName() + " with rank " + newRank.getIdentifier() + " in tree " + rankTree.getIdentifier());
            }
            RDQPlayer freshPlayer = plugin.getPlayerRepository().findById(rdqPlayer.getId());
            if (freshPlayer != null) {
                plugin.getPlayerRepository().update(freshPlayer);
            }
            this.markProgressEntriesCompleted(plugin, rdqPlayer, newRank);
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to handle rank redemption database operations", exception);
            throw new RuntimeException("Database rank redemption failed", exception);
        }
    }

    private void handleLuckPermsRankAssignment(final @NotNull RDQ plugin, final @NotNull Player player, final @NotNull RRank newRank) {
        try {
            if (plugin.getLuckPermsService() != null) {
                final String luckPermsGroup = newRank.getAssignedLuckPermsGroup();
                if (luckPermsGroup != null && !luckPermsGroup.isEmpty()) {
                    this.removePlayerFromPreviousRankGroups(plugin, player, newRank);
                    LOGGER.info("Assigned LuckPerms group '" + luckPermsGroup + "' to player " + player.getName());
                } else {
                    LOGGER.warning("No LuckPerms group defined for rank " + newRank.getIdentifier());
                }
            } else {
                LOGGER.warning("LuckPerms service not available for rank assignment");
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to handle LuckPerms rank assignment", exception);
        }
    }

    private void removePlayerFromPreviousRankGroups(final @NotNull RDQ plugin, final @NotNull Player player, final @NotNull RRank newRank) {
        try {
            final RRankTree rankTree = newRank.getRankTree();
            if (rankTree != null) {
                for (final RRank rank : rankTree.getRanks()) {
                    if (!rank.equals(newRank)) {
                        final String groupToRemove = rank.getAssignedLuckPermsGroup();
                        if (groupToRemove != null && !groupToRemove.isEmpty()) {
                            //TODO plugin.getLuckPermsService().removePlayerFromGroup(player.getUniqueId(), groupToRemove);
                        }
                    }
                }
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to remove player from previous rank groups", exception);
        }
    }

    private void markProgressEntriesCompleted(final @NotNull RDQ plugin, final @NotNull RDQPlayer rdqPlayer, final @NotNull RRank rank) {
        try {
            final Set<RRankUpgradeRequirement> upgradeRequirements = rank.getUpgradeRequirements();
            for (final RRankUpgradeRequirement requirement : upgradeRequirements) {
                final List<RPlayerRankUpgradeProgress> progressList = plugin.getPlayerRankUpgradeProgressRepository()
                        .findListByAttributes(Map.of(
                                "player.uniqueId", rdqPlayer.getUniqueId(),
                                "upgradeRequirement.id", requirement.getId()
                        ));
                for (final RPlayerRankUpgradeProgress progress : progressList) {
                    if (!progress.isCompleted()) {
                        final RPlayerRankUpgradeProgress freshProgress = plugin.getPlayerRankUpgradeProgressRepository().findById(progress.getId());
                        if (freshProgress != null) {
                            freshProgress.setProgress(1.0);
                            plugin.getPlayerRankUpgradeProgressRepository().update(freshProgress);
                        }
                    }
                }
            }
            LOGGER.log(Level.FINE, "Marked progress entries as completed for rank " + rank.getIdentifier());
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to mark progress entries as completed", exception);
        }
    }

    private void openRankRequirementsView(final @NotNull SlotClickContext clickContext, final @NotNull RankNode rankNode) {
        try {
            final RDQ plugin = this.rdq.get(clickContext);
            final RDQPlayer rdqPlayer = this.currentPlayer.get(clickContext);
            final Player player = clickContext.getPlayer();
            final boolean previewMode = this.isPreviewMode.get(clickContext);
            LOGGER.log(Level.INFO, "Opening requirements view for rank " + rankNode.getRank().getIdentifier() + " for player " + player.getName());
            this.lastRefreshAt.set(0L, clickContext);
            clickContext.openForPlayer(
                    RankPathRankRequirementOverview.class,
                    Map.of(
                            "plugin", plugin,
                            "player", rdqPlayer,
                            "targetRank", rankNode.getRank(),
                            "rankTree", selectedRankTree.get(clickContext),
                            "previewMode", previewMode,
                            "initialData", clickContext.getInitialData(),
                            "cachedRequirementProgress", new HashMap<>(),
                            "lastProgressRefresh", 0L
                    )
            );
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to open requirements view for rank " + rankNode.getRank().getIdentifier(), exception);
            try {
                this.i18n("error.requirements_view_failed", clickContext.getPlayer())
                        .with("rank_name", rankNode.getRank().getIdentifier())
                        .withPrefix()
                        .send();
            } catch (final Exception fallbackException) {
                LOGGER.log(Level.WARNING, "Failed to send requirements view error message", fallbackException);
                this.i18n("error.general", clickContext.getPlayer()).withPrefix().send();
            }
        }
    }

    private void refreshCachedData(final @NotNull SlotClickContext clickContext) {
        try {
            final boolean previewMode = this.isPreviewMode.get(clickContext);
            if (!previewMode) {
                LOGGER.log(Level.FINE, "Refreshing cached data after rank interaction");
                if (this.progressManager != null) {
                    this.progressManager.cleaRDQPlayerCache(clickContext.getPlayer());
                }
                this.initializeAndCacheData(clickContext, previewMode);
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to refresh cached data", exception);
        }
    }

    private void sendNoRequirementsMessage(final @NotNull Player player, final @NotNull RRank rank) {
        try {
            this.i18n("messages.no_requirements", player)
                    .with("rank_name", rank.getIdentifier())
                    .withPrefix()
                    .send();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to send no requirements message", exception);
        }
    }

    private void sendProgressStartedMessage(final @NotNull Player player, final @NotNull RRank rank, final int requirementCount) {
        try {
            this.i18n("messages.rank_started", player)
                    .with("rank_name", rank.getIdentifier())
                    .with("requirement_count", requirementCount)
                    .withPrefix()
                    .send();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to send progress started message", exception);
        }
    }

    private void sendProgressStartErrorMessage(final @NotNull Player player, final @NotNull RRank rank) {
        try {
            this.i18n("messages.rank_start_error", player)
                    .with("rank_name", rank.getIdentifier())
                    .withPrefix()
                    .send();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to send progress start error message", exception);
        }
    }

    private void sendRequirementsNotCompletedMessage(final @NotNull Player player, final @NotNull RRank rank) {
        try {
            this.i18n("messages.requirements_not_completed", player)
                    .with("rank_name", rank.getIdentifier())
                    .withPrefix()
                    .send();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to send requirements not completed message", exception);
        }
    }

    private void sendRankRedemptionErrorMessage(final @NotNull Player player, final @NotNull RRank rank) {
        try {
            this.i18n("messages.rank_redemption_error", player)
                    .with("rank_name", rank.getIdentifier())
                    .withPrefix()
                    .send();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to send rank redemption error message", exception);
        }
    }

    private void sendRankRedeemedMessage(final @NotNull Player player, final @NotNull RRank rank) {
        try {
            this.i18n("messages.rank_redeemed", player)
                    .with("rank_name", rank.getIdentifier())
                    .withPrefix()
                    .send();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to send rank redeemed message", exception);
        }
    }
}*/
