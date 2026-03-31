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

package com.raindropcentral.rdq.view.ranks;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankReward;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rdq.view.ranks.util.RewardCardRenderer;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Detailed view showing all rewards for a rank in a grid layout.
 */
public class RankRewardsDetailView extends BaseView {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");

    // State
    private final State<RDQ> rdq = initialState("plugin");
    private final State<RDQPlayer> currentPlayer = initialState("player");
    private final State<RRank> targetRank = initialState("targetRank");
    private final State<RRankTree> selectedRankTree = initialState("rankTree");
    private final State<Boolean> previewMode = initialState("previewMode");

    // Layout constants
    private static final int RANK_INFO_SLOT = 4;
    private static final int[] REWARD_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    /**
     * Executes RankRewardsDetailView.
     */
    public RankRewardsDetailView() {
        super(RankRequirementsJourneyView.class);
    }

    @Override
    protected String getKey() {
        return "rank_rewards_detail_ui";
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        final RRank rank = this.targetRank.get(openContext);
        return Map.of("rank_name", rank != null ? rank.getIdentifier() : "Unknown");
    }

    /**
     * Override back button handling to properly pass all required data back to RankRequirementsJourneyView.
     */
    @Override
    protected void handleBackButtonClick(final @NotNull SlotClickContext clickContext) {
        try {
            final Map<String, Object> data = new HashMap<>();
            data.put("plugin", this.rdq.get(clickContext));
            data.put("player", this.currentPlayer.get(clickContext));
            data.put("targetRank", this.targetRank.get(clickContext));
            data.put("rankTree", this.selectedRankTree.get(clickContext));
            data.put("previewMode", this.previewMode.get(clickContext));
            
            clickContext.openForPlayer(RankRequirementsJourneyView.class, data);
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to navigate back to RankRequirementsJourneyView", e);
            clickContext.closeForPlayer();
        }
    }

    /**
     * Executes onFirstRender.
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        try {
            final RRank rank = this.targetRank.get(render);

            if (rank == null) {
                renderErrorState(render, player);
                return;
            }

            // Render rank info at top
            renderRankInfo(render, player, rank);

            // Render all rewards in grid
            renderAllRewards(render, player, rank);

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to render rewards detail view", e);
            renderErrorState(render, player);
        }
    }

    private void renderRankInfo(final @NotNull RenderContext render, final @NotNull Player player, final @NotNull RRank rank) {
        render.slot(RANK_INFO_SLOT)
                .renderWith(() -> {
                    try {
                        final Material icon = Material.valueOf(rank.getIcon().getMaterial().toUpperCase());
                        final int rewardCount = rank.getRewardsOrdered().size();
                        
                        return UnifiedBuilderFactory.item(icon)
                                .setName(this.i18n("rank_info.name", player)
                                        .withPlaceholder("rank_name", rank.getIdentifier())
                                        .build().component())
                                .setLore(this.i18n("rank_info.lore", player)
                                        .withPlaceholder("count", String.valueOf(rewardCount))
                                        .build().children())
                                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                                .build();
                    } catch (final Exception e) {
                        return UnifiedBuilderFactory.item(Material.DIAMOND)
                                .setName(Component.text(rank.getIdentifier()))
                                .build();
                    }
                });
    }

    private void renderAllRewards(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @NotNull RRank rank
    ) {
        final List<RRankReward> rewards = rank.getRewardsOrdered();

        if (rewards.isEmpty()) {
            // Show "no rewards" message in center
            render.slot(22)
                    .renderWith(() -> RewardCardRenderer.createNoRewardsCard(player));
            return;
        }

        // Render each reward in the grid
        for (int i = 0; i < Math.min(REWARD_SLOTS.length, rewards.size()); i++) {
            final RRankReward reward = rewards.get(i);
            final int slot = REWARD_SLOTS[i];

            render.slot(slot)
                    .renderWith(() -> RewardCardRenderer.createRewardCard(reward, player))
                    .onClick(clickContext -> {
                        // Show detailed info about the reward
                        this.i18n(reward.getReward().getReward().getDescriptionKey(), player)
                                .withPlaceholder("reward_type", reward.getReward().getTypeId())
                                .build().sendMessage();
                    });
        }
    }

    private void renderErrorState(final @NotNull RenderContext render, final @NotNull Player player) {
        render.slot(22)
                .renderWith(() -> UnifiedBuilderFactory.item(Material.BARRIER)
                        .setName(Component.text("Error loading rewards"))
                        .build());
    }
}
