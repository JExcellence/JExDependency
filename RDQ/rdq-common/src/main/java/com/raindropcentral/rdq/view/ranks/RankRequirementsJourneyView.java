package com.raindropcentral.rdq.view.ranks;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRank;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq.manager.RankRequirementProgressManager;
import com.raindropcentral.rdq.manager.RankRequirementProgressManager.RequirementProgressData;
import com.raindropcentral.rdq.manager.RankRequirementProgressManager.RequirementStatus;
import com.raindropcentral.rdq.view.ranks.util.RequirementCardRenderer;
import com.raindropcentral.rdq.view.ranks.util.RequirementProgressRenderer;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Journey-style view for displaying rank requirements with visual progress indicators.
 * Features a slot-based progress bar, requirement cards with task previews, and one-click completion.
 *
 * @author ItsRainingHP
 * @version 1.0.0
 */
public class RankRequirementsJourneyView extends BaseView {

    private static final Logger LOGGER = CentralLogger.getLogger("RDQ");

    // State
    private final State<RDQ> rdq = initialState("plugin");
    private final State<RDQPlayer> currentPlayer = initialState("player");
    private final State<RRank> targetRank = initialState("targetRank");
    private final State<RRankTree> selectedRankTree = initialState("rankTree");
    private final State<Boolean> previewMode = initialState("previewMode");
    
    private final MutableState<Boolean> allCompleted = mutableState(false);
    private final MutableState<Boolean> pulseState = mutableState(false);
    
    // Pulsing animation task
    private BukkitRunnable pulseTask;

    // Layout constants
    private static final int BACK_SLOT = 0;
    private static final int RANK_INFO_SLOT = 4;
    private static final int CLAIM_BUTTON_SLOT = 8;
    private static final int OVERALL_PROGRESS_SLOT = 22;
    
    // Requirement slots (row 3)
    private static final int[] REQUIREMENT_SLOTS = {28, 30, 32, 34};
    // Connection slots between requirements
    private static final int[] CONNECTION_SLOTS = {29, 31, 33};
    // Overflow slots (row 4) for 5+ requirements
    private static final int[] OVERFLOW_SLOTS = {37, 39, 41, 43};
    private static final int[] OVERFLOW_CONNECTION_SLOTS = {38, 40, 42};

    private RankRequirementProgressManager progressManager;
    private RequirementProgressRenderer progressRenderer;
    private RequirementCardRenderer cardRenderer;

    public RankRequirementsJourneyView() {
        super(RankPathOverview.class);
    }

    @Override
    protected String getKey() {
        return "rank_requirements_journey_ui";
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        final RRank rank = this.targetRank.get(openContext);
        return Map.of("rank_name", rank != null ? rank.getIdentifier() : "Unknown");
    }

    /**
     * Called when returning to this view from a child view (e.g., detail view).
     * Refreshes the progress cache and updates the UI state.
     */
    @Override
    public void onResume(final @NotNull Context origin, final @NotNull Context target) {
        super.onResume(origin, target);
        
        try {
            final Player player = target.getPlayer();
            final RDQPlayer rdqPlayer = this.currentPlayer.get(target);
            final RRank rank = this.targetRank.get(target);
            
            if (progressManager != null && rank != null && rdqPlayer != null) {
                // Clear cache and refresh progress
                progressManager.refreshRankProgress(player, rdqPlayer, rank);
                
                // Update allCompleted state
                final boolean canClaim = progressManager.areAllRequirementsCompleted(player, rdqPlayer, rank);
                this.allCompleted.set(canClaim, target);
                
                LOGGER.log(Level.FINE, "Resumed journey view, allCompleted=" + canClaim);
            }
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to refresh progress on resume", e);
        }
    }

    /**
     * Override back button handling to properly pass all required data to RankPathOverview.
     */
    @Override
    protected void handleBackButtonClick(final @NotNull SlotClickContext clickContext) {
        try {
            final Map<String, Object> data = new HashMap<>();
            data.put("plugin", this.rdq.get(clickContext));
            data.put("player", this.currentPlayer.get(clickContext));
            data.put("rankTree", this.selectedRankTree.get(clickContext));
            data.put("previewMode", this.previewMode.get(clickContext));
            
            clickContext.openForPlayer(RankPathOverview.class, data);
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to navigate back to RankPathOverview", e);
            clickContext.closeForPlayer();
        }
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        try {
            final RDQ plugin = this.rdq.get(render);
            final RRank rank = this.targetRank.get(render);
            final RDQPlayer rdqPlayer = this.currentPlayer.get(render);
            final boolean isPreviewMode = this.previewMode.get(render);

            if (rank == null) {
                renderErrorState(render, player);
                return;
            }

            // Initialize managers
            this.progressManager = new RankRequirementProgressManager(plugin);
            this.progressRenderer = new RequirementProgressRenderer();
            this.cardRenderer = new RequirementCardRenderer();

            // Initialize progress tracking
            this.progressManager.initializeRankProgressTracking(rdqPlayer, rank);

            // Render all components
            // Note: Back button is handled automatically by the framework at bottom-left
            renderRankInfo(render, player, rank);
            renderProgressBar(render, player, rank, rdqPlayer);
            renderOverallProgress(render, player, rank, rdqPlayer);
            renderRequirementCards(render, player, rank, rdqPlayer, isPreviewMode);
            renderClaimOrLockedButton(render, player, rank, rdqPlayer, isPreviewMode);
            
            // Start pulsing animation for ready requirements
            startPulseAnimation(render, plugin);

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to render journey view", e);
            renderErrorState(render, player);
        }
    }

    private void renderRankInfo(final @NotNull RenderContext render, final @NotNull Player player, final @NotNull RRank rank) {
        render.slot(RANK_INFO_SLOT)
                .renderWith(() -> {
                    try {
                        final Material icon = Material.valueOf(rank.getIcon().getMaterial().toUpperCase());
                        return UnifiedBuilderFactory.item(icon)
                                .setName(this.i18n("rank_info.name", player)
                                        .withPlaceholder("rank_name", rank.getIdentifier())
                                        .build().component())
                                .setLore(this.i18n("rank_info.lore", player).build().children())
                                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                                .build();
                    } catch (final Exception e) {
                        return UnifiedBuilderFactory.item(Material.DIAMOND)
                                .setName(Component.text(rank.getIdentifier()))
                                .build();
                    }
                });
    }

    private void renderProgressBar(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @NotNull RRank rank,
            final @NotNull RDQPlayer rdqPlayer
    ) {
        final double progress = progressManager.getRankOverallProgress(player, rdqPlayer, rank);
        final List<RRankUpgradeRequirement> requirements = new ArrayList<>(rank.getUpgradeRequirements());
        
        int completed = 0;
        for (final RRankUpgradeRequirement req : requirements) {
            final RequirementProgressData data = progressManager.getRequirementProgress(player, rdqPlayer, req);
            if (data.getStatus() == RequirementStatus.COMPLETED) {
                completed++;
            }
        }

        progressRenderer.renderProgressBar(render, player, progress, completed, requirements.size());
    }

    private void renderOverallProgress(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @NotNull RRank rank,
            final @NotNull RDQPlayer rdqPlayer
    ) {
        render.slot(OVERALL_PROGRESS_SLOT)
                .renderWith(() -> {
                    final double progress = progressManager.getRankOverallProgress(player, rdqPlayer, rank);
                    final int percentage = (int) Math.round(progress * 100);
                    final List<RRankUpgradeRequirement> requirements = new ArrayList<>(rank.getUpgradeRequirements());
                    
                    int completed = 0;
                    for (final RRankUpgradeRequirement req : requirements) {
                        final RequirementProgressData data = progressManager.getRequirementProgress(player, rdqPlayer, req);
                        if (data.getStatus() == RequirementStatus.COMPLETED) {
                            completed++;
                        }
                    }

                    return UnifiedBuilderFactory.item(Material.NETHER_STAR)
                            .setName(this.i18n("overall_progress.name", player).build().component())
                            .setLore(this.i18n("overall_progress.lore", player)
                                    .withPlaceholder("percentage", percentage)
                                    .withPlaceholder("completed", completed)
                                    .withPlaceholder("total", requirements.size())
                                    .build().children())
                            .setGlowing(percentage >= 100)
                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                            .build();
                })
                .updateOnStateChange(this.allCompleted);
    }

    private void renderRequirementCards(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @NotNull RRank rank,
            final @NotNull RDQPlayer rdqPlayer,
            final boolean isPreviewMode
    ) {
        final List<RRankUpgradeRequirement> requirements = new ArrayList<>(rank.getUpgradeRequirements());
        requirements.sort(Comparator.comparingInt(RRankUpgradeRequirement::getDisplayOrder));

        // Render main row requirements (up to 4)
        for (int i = 0; i < Math.min(4, requirements.size()); i++) {
            final RRankUpgradeRequirement req = requirements.get(i);
            final int slot = REQUIREMENT_SLOTS[i];
            
            renderRequirementCard(render, player, rdqPlayer, req, slot, isPreviewMode);
            
            // Render connection to next requirement
            if (i < Math.min(3, requirements.size() - 1)) {
                renderConnection(render, player, rdqPlayer, req, requirements.get(i + 1), CONNECTION_SLOTS[i]);
            }
        }

        // Render overflow row if needed (5-8 requirements)
        if (requirements.size() > 4) {
            for (int i = 4; i < Math.min(8, requirements.size()); i++) {
                final RRankUpgradeRequirement req = requirements.get(i);
                final int slot = OVERFLOW_SLOTS[i - 4];
                
                renderRequirementCard(render, player, rdqPlayer, req, slot, isPreviewMode);
                
                if (i < Math.min(7, requirements.size() - 1) && i - 4 < OVERFLOW_CONNECTION_SLOTS.length) {
                    renderConnection(render, player, rdqPlayer, req, requirements.get(i + 1), OVERFLOW_CONNECTION_SLOTS[i - 4]);
                }
            }
        }
    }

    private void renderRequirementCard(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @NotNull RDQPlayer rdqPlayer,
            final @NotNull RRankUpgradeRequirement requirement,
            final int slot,
            final boolean isPreviewMode
    ) {
        render.slot(slot)
                .renderWith(() -> {
                    final RequirementProgressData progress = progressManager.getRequirementProgress(player, rdqPlayer, requirement);
                    final ItemStack card = cardRenderer.createRequirementCard(player, requirement, progress);
                    
                    // Apply pulsing glow effect for READY_TO_COMPLETE requirements
                    if (progress.getStatus() == RequirementStatus.READY_TO_COMPLETE) {
                        final boolean shouldGlow = this.pulseState.get(render);
                        if (!shouldGlow) {
                            // Remove glow during pulse-off phase
                            final var meta = card.getItemMeta();
                            if (meta != null) {
                                meta.setEnchantmentGlintOverride(false);
                                card.setItemMeta(meta);
                            }
                        }
                    }
                    
                    return card;
                })
                .updateOnStateChange(this.allCompleted)
                .updateOnStateChange(this.pulseState)
                .onClick(ctx -> handleRequirementClick(ctx, requirement, isPreviewMode));
    }

    private void renderConnection(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @NotNull RDQPlayer rdqPlayer,
            final @NotNull RRankUpgradeRequirement current,
            final @NotNull RRankUpgradeRequirement next,
            final int slot
    ) {
        render.slot(slot)
                .renderWith(() -> {
                    final RequirementProgressData currentProgress = progressManager.getRequirementProgress(player, rdqPlayer, current);
                    final Material material = currentProgress.getStatus() == RequirementStatus.COMPLETED
                            ? Material.LIME_STAINED_GLASS_PANE
                            : Material.GRAY_STAINED_GLASS_PANE;
                    
                    return UnifiedBuilderFactory.item(material)
                            .setName(Component.text(" "))
                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                            .build();
                })
                .updateOnStateChange(this.allCompleted);
    }

    /**
     * Renders the claim button (slot 8, top-right).
     * Shows a barrier when requirements are not complete (locked state).
     * Shows an emerald block claim button when all requirements are completed.
     */
    private void renderClaimOrLockedButton(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @NotNull RRank rank,
            final @NotNull RDQPlayer rdqPlayer,
            final boolean isPreviewMode
    ) {
        // Calculate completion status OUTSIDE of renderWith to avoid infinite loop
        final boolean canClaim = progressManager.areAllRequirementsCompleted(player, rdqPlayer, rank);
        this.allCompleted.set(canClaim, render);

        render.slot(CLAIM_BUTTON_SLOT)
                .renderWith(() -> {
                    // Use the cached state value instead of recalculating
                    final boolean claimable = this.allCompleted.get(render);

                    if (claimable && !isPreviewMode) {
                        // All requirements completed - show claim button
                        return UnifiedBuilderFactory.item(Material.EMERALD_BLOCK)
                                .setName(this.i18n("claim_button.name", player).build().component())
                                .setLore(this.i18n("claim_button.lore", player).build().children())
                                .setGlowing(true)
                                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                                .build();
                    } else {
                        // Requirements not complete - show barrier (locked indicator)
                        return UnifiedBuilderFactory.item(Material.BARRIER)
                                .setName(this.i18n("claim_button.locked.name", player).build().component())
                                .setLore(this.i18n("claim_button.locked.lore", player).build().children())
                                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                                .build();
                    }
                })
                .updateOnStateChange(this.allCompleted)
                .onClick(ctx -> handleClaimClick(ctx, rank, rdqPlayer, isPreviewMode));
    }

    private void handleRequirementClick(
            final @NotNull SlotClickContext ctx,
            final @NotNull RRankUpgradeRequirement requirement,
            final boolean isPreviewMode
    ) {
        final Player player = ctx.getPlayer();
        final RDQPlayer rdqPlayer = this.currentPlayer.get(ctx);

        if (isPreviewMode) {
            this.i18n("messages.preview_mode", player).includePrefix().build().sendMessage();
            return;
        }

        final RequirementProgressData progress = progressManager.getRequirementProgress(player, rdqPlayer, requirement);

        if (ctx.getClickOrigin().getClick().isLeftClick()) {
            // Try to complete the requirement
            if (progress.getStatus() == RequirementStatus.READY_TO_COMPLETE) {
                final var result = progressManager.attemptRequirementCompletion(player, rdqPlayer, requirement);
                if (result.isSuccess()) {
                    this.i18n("messages.requirement_completed", player).includePrefix().build().sendMessage();
                    
                    // Trigger celebration animation
                    final int clickedSlot = ctx.getClickedSlot();
                    playCelebrationAnimation(ctx, clickedSlot);
                    
                    ctx.update();
                    
                    // Check if all requirements are now completed
                    final RRank rank = this.targetRank.get(ctx);
                    if (progressManager.areAllRequirementsCompleted(player, rdqPlayer, rank)) {
                        this.allCompleted.set(true, ctx);
                        this.i18n("messages.all_completed", player).includePrefix().build().sendMessage();
                    }
                }
            } else if (progress.getStatus() != RequirementStatus.COMPLETED) {
                this.i18n("messages.not_ready", player).includePrefix().build().sendMessage();
            }
        } else if (ctx.getClickOrigin().getClick().isRightClick()) {
            // Don't open detail view for completed requirements
            if (progress.getStatus() == RequirementStatus.COMPLETED) {
                this.i18n("messages.already_completed", player).includePrefix().build().sendMessage();
                return;
            }
            // Open detail view for incomplete requirements
            openDetailView(ctx, requirement);
        }
    }

    private void openDetailView(final @NotNull SlotClickContext ctx, final @NotNull RRankUpgradeRequirement requirement) {
        final Map<String, Object> data = new HashMap<>();
        data.put("plugin", this.rdq.get(ctx));
        data.put("player", this.currentPlayer.get(ctx));
        data.put("rankTree", this.selectedRankTree.get(ctx));
        data.put("targetRank", this.targetRank.get(ctx));
        data.put("requirement", requirement);
        data.put("previewMode", this.previewMode.get(ctx));
        
        ctx.openForPlayer(RankRequirementDetailView.class, data);
    }

    private void handleClaimClick(
            final @NotNull SlotClickContext ctx,
            final @NotNull RRank rank,
            final @NotNull RDQPlayer rdqPlayer,
            final boolean isPreviewMode
    ) {
        final Player player = ctx.getPlayer();

        if (isPreviewMode) {
            this.i18n("messages.preview_mode", player).includePrefix().build().sendMessage();
            return;
        }

        if (!progressManager.areAllRequirementsCompleted(player, rdqPlayer, rank)) {
            return;
        }

        try {
            final RDQ plugin = this.rdq.get(ctx);
            final RRankTree rankTree = this.selectedRankTree.get(ctx);

            // Update player rank in database
            updatePlayerRank(plugin, rdqPlayer, rankTree, rank);

            // Send success message
            this.i18n("messages.rank_claimed", player)
                    .withPlaceholder("rank_name", rank.getIdentifier())
                    .includePrefix()
                    .build().sendMessage();

            // Return to rank overview
            ctx.openForPlayer(RankPathOverview.class, Map.of(
                    "plugin",
                    plugin,
                    "player",
                    rdqPlayer,
                    "rankTree",
                    rankTree,
                    "previewMode",
                    false
            ));

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to claim rank", e);
        }
    }

    private void updatePlayerRank(
            final @NotNull RDQ plugin,
            final @NotNull RDQPlayer rdqPlayer,
            final @NotNull RRankTree rankTree,
            final @NotNull RRank newRank
    ) {
        final List<RPlayerRank> playerRanks = plugin.getPlayerRankRepository()
                .findAllByAttributes(Map.of("player.uniqueId", rdqPlayer.getUniqueId()));

        final RPlayerRank existingRank = playerRanks.stream()
                .filter(r -> Objects.equals(r.getRankTree(), rankTree))
                .findFirst()
                .orElse(null);

        if (existingRank != null) {
            existingRank.setCurrentRank(newRank);
            plugin.getPlayerRankRepository().update(existingRank);
        } else {
            final RPlayerRank newPlayerRank = new RPlayerRank(rdqPlayer, newRank, rankTree);
            plugin.getPlayerRankRepository().create(newPlayerRank);
        }
    }

    /**
     * Plays a celebration animation when a requirement is completed.
     * Flashes surrounding slots to lime glass, then returns to normal after 500ms.
     */
    private void playCelebrationAnimation(final @NotNull SlotClickContext ctx, final int completedSlot) {
        final RDQ rdqPlugin = this.rdq.get(ctx);
        final Player player = ctx.getPlayer();
        
        // Calculate surrounding slots (adjacent slots in a 3x3 grid around the completed slot)
        final int[] surroundingOffsets = {-10, -9, -8, -1, 1, 8, 9, 10};
        final List<Integer> validSurroundingSlots = new ArrayList<>();
        
        for (final int offset : surroundingOffsets) {
            final int targetSlot = completedSlot + offset;
            // Ensure slot is within valid inventory range (0-53 for 6-row chest)
            if (targetSlot >= 0 && targetSlot < 54) {
                // Avoid wrapping to different rows for left/right offsets
                final int completedRow = completedSlot / 9;
                final int targetRow = targetSlot / 9;
                if (Math.abs(offset) == 1 && completedRow != targetRow) {
                    continue;
                }
                validSurroundingSlots.add(targetSlot);
            }
        }
        
        // Store original items to restore later
        final Map<Integer, ItemStack> originalItems = new HashMap<>();
        final var inventory = player.getOpenInventory().getTopInventory();
        
        for (final int slot : validSurroundingSlots) {
            final ItemStack original = inventory.getItem(slot);
            if (original != null) {
                originalItems.put(slot, original.clone());
            }
        }
        
        // Flash to lime glass
        final ItemStack celebrationItem = UnifiedBuilderFactory.item(Material.LIME_STAINED_GLASS_PANE)
                .setName(Component.text(" "))
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
        
        for (final int slot : validSurroundingSlots) {
            inventory.setItem(slot, celebrationItem);
        }
        
        // Schedule restoration after 500ms (10 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                // Restore original items or clear the slot
                for (final int slot : validSurroundingSlots) {
                    final ItemStack original = originalItems.get(slot);
                    inventory.setItem(slot, original);
                }
                // Trigger a full update to ensure proper rendering
                ctx.update();
            }
        }.runTaskLater(rdqPlugin.getPlugin(), 10L);
    }

    /**
     * Starts the pulsing animation for ready-to-complete requirements.
     * Toggles glow state every 500ms (10 ticks).
     */
    private void startPulseAnimation(final @NotNull RenderContext render, final @NotNull RDQ plugin) {
        // Cancel any existing pulse task
        stopPulseAnimation();
        
        this.pulseTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // Toggle pulse state
                    final boolean currentState = pulseState.get(render);
                    pulseState.set(!currentState, render);
                } catch (final Exception e) {
                    // View may have been closed, cancel the task
                    this.cancel();
                }
            }
        };
        
        // Run every 10 ticks (500ms) for pulsing effect
        this.pulseTask.runTaskTimer(plugin.getPlugin(), 10L, 10L);
    }
    
    /**
     * Stops the pulsing animation task.
     */
    private void stopPulseAnimation() {
        if (this.pulseTask != null) {
            try {
                this.pulseTask.cancel();
            } catch (final IllegalStateException ignored) {
                // Task was already cancelled
            }
            this.pulseTask = null;
        }
    }

    private void renderErrorState(final @NotNull RenderContext render, final @NotNull Player player) {
        render.slot(22)
                .renderWith(() -> UnifiedBuilderFactory.item(Material.BARRIER)
                        .setName(Component.text("Error loading requirements"))
                        .build());
    }
}
