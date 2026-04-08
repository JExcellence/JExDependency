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

package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import com.raindropcentral.rdq.database.entity.bounty.BountyReward;
import com.raindropcentral.rplatform.utility.heads.view.Cancel;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import com.raindropcentral.rplatform.view.ConfirmationView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Compact view for displaying bounty details on a specific player.
 *
 * @author JExcellence
 * @version 1.1.0
 */
public class BountyPlayerInfoView extends BaseView {

    private final State<RDQ> rdq = initialState("plugin");
    private final MutableState<Optional<OfflinePlayer>> target = initialState("target");
    private final MutableState<List<BountyReward>> rewards = initialState("rewards");
    private final State<Optional<Bounty>> bounty = initialState("bounty");
    private final State<Map<UUID, Map<Integer, ItemStack>>> insertedItems = initialState("insertedItems");

    /**
     * Executes BountyPlayerInfoView.
     */
    public BountyPlayerInfoView() {
        super(BountyOverviewView.class);
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "  P   R  ",
            "       D "
        };
    }

    @Override
    protected int getSize() {
        return 2;
    }

    @Override
    protected Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext open) {
        var bountyOpt = this.bounty.get(open);
        if (bountyOpt.isEmpty()) {
            return Map.of("target_name", "Unknown");
        }
        var targetPlayer = Bukkit.getOfflinePlayer(bountyOpt.get().getTargetUniqueId());
        var targetName = targetPlayer.getName() != null ? targetPlayer.getName() : "Unknown";
        return Map.of("target_name", targetName);
    }

    @Override
    protected String getKey() {
        return "bounty_player_info_ui";
    }

    /**
     * Executes onFirstRender.
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        renderTargetHead(render, player);
        renderRewardsButton(render, player);
        renderDeleteButton(render, player);
    }

    private void renderTargetHead(final @NotNull RenderContext render, final @NotNull Player player) {
        var targetOpt = this.target.get(render);
        if (targetOpt.isEmpty()) return;

        var targetPlayer = targetOpt.get();
        var targetName = targetPlayer.getName() != null ? targetPlayer.getName() : "Unknown";

        render.layoutSlot('P', UnifiedBuilderFactory
            .unifiedHead(targetPlayer)
            .setDisplayName((Component) this.i18n("target.name", player)
                .withPlaceholder("target_name", targetName)
                .build().component())
            .setLore(List.of(
                Component.empty(),
                this.i18n("target.coming_soon", player).build().component()
            ))
            .build()
        );
    }

    private void renderRewardsButton(final @NotNull RenderContext render, final @NotNull Player player) {
        var bountyOpt = this.bounty.get(render);
        int rewardCount = bountyOpt.map(b -> b.getRewards().size()).orElse(0);

        render.layoutSlot('R', UnifiedBuilderFactory
            .item(Material.CHEST)
            .setName(this.i18n("rewards.name", player).build().component())
            .setLore(this.i18n("rewards.lore", player)
                .withPlaceholder("reward_count", String.valueOf(rewardCount))
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            var b = this.bounty.get(ctx);
            if (b.isEmpty()) return;

            ctx.openForPlayer(BountyRewardView.class, Map.of(
                "plugin", rdq.get(ctx),
                "target", target.get(ctx),
                "rewards", b.get().getRewards(),
                "bounty", b,
                "insertedItems", new HashMap<>()
            ));
        });
    }

    private void renderDeleteButton(final @NotNull RenderContext render, final @NotNull Player player) {
        render.layoutSlot('D', UnifiedBuilderFactory
            .item(new Cancel().getHead(player))
            .setName(this.i18n("delete.name", player).build().component())
            .setLore(this.i18n("delete.lore", player).build().children())
            .build()
        ).displayIf(ctx -> ctx.getPlayer().isOp())
        .onClick(ctx -> {
            var b = this.bounty.get(ctx);
            if (b.isEmpty()) return;

            var plugin = rdq.get(ctx);
            var targetPlayer = this.target.get(render);
            
            if (plugin == null || targetPlayer == null) {
                player.sendMessage("§cError: Missing required data");
                return;
            }

            Map<String, Object> initialData = new HashMap<>();
            initialData.put("plugin", plugin);
            initialData.put("target", targetPlayer);
            initialData.put("rewards", new ArrayList<>());
            initialData.put("bounty", b.get());
            initialData.put("insertedItems", new HashMap<>());

            new ConfirmationView.Builder()
                .withKey("bounty_player_info_ui")
                .withMessageKey("bounty_player_info_ui.confirm.message")
                .withInitialData(initialData)
                .withCallback(confirmed -> {
                    if (confirmed) {
                        plugin.getBountyFactory().deleteBounty(b.get());
                        player.closeInventory();
                    }
                })
                .withParentView(BountyPlayerInfoView.class)
                .openFor(ctx, player);
        });
    }

    /**
     * Executes onResume.
     */
    @Override
    public void onResume(final @NotNull Context origin, final @NotNull Context target) {
        @SuppressWarnings("unchecked")
        Map<String, Object> initialData = (Map<String, Object>) target.getInitialData();

        if (initialData == null || !Boolean.TRUE.equals(initialData.get("confirmed"))) {
            return;
        }

        // Bounty is stored as the actual object, not Optional
        var bounty = (Bounty) initialData.get("bounty");
        if (bounty == null) return;

        @SuppressWarnings("unchecked")
        var targetPlayer = (Optional<OfflinePlayer>) initialData.get("target");
        var rdqPlugin = (RDQ) initialData.get("plugin");
        var commissionerUniqueId = bounty.getCommissionerUniqueId();

        CompletableFuture.supplyAsync(
            () -> rdqPlugin.getPlayerRepository().findByAttributes(Map.of("uniqueId", commissionerUniqueId)).orElse(null),
            rdqPlugin.getExecutor()
        ).thenCompose(rdqPlayer -> {
            if (rdqPlayer == null) return CompletableFuture.completedFuture(null);

            return rdqPlugin.getPlayerRepository().updateAsync(rdqPlayer).thenCompose(p -> 
                rdqPlugin.getBountyRepository().deleteAsync(bounty.getId()).thenAccept(v -> {
                    if (targetPlayer != null && targetPlayer.isPresent()) {
                        rdqPlugin.getVisualIndicatorManager().removeIndicators(targetPlayer.get().getUniqueId());
                    }

                    var targetName = targetPlayer != null && targetPlayer.isPresent() && targetPlayer.get().getName() != null
                        ? targetPlayer.get().getName() : "Unknown";

                    i18n("deleted_bounty_successfully", origin.getPlayer())
                        .includePrefix()
                        .withPlaceholders(Map.of(
                            "bounty_id", bounty.getId(),
                            "target_name", targetName
                        ))
                        .build().sendMessage();
                    
                    // Open the view AFTER deletion completes
                    target.openForPlayer(BountyMainView.class, Map.of(
                        "plugin", rdqPlugin,
                        "target", targetPlayer,
                        "rewards", rewards,
                        "bounty", this.bounty.get(target),
                        "insertedItems", insertedItems
                    ));
                })
            );
        });
    }
}
