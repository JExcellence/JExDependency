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

package com.raindropcentral.rdt.view.town;

import com.raindropcentral.rdt.service.NationCreationProgressSnapshot;
import com.raindropcentral.rdt.service.TownLevelRewardSnapshot;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reward preview view for the dedicated nation-creation flow.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownNationRewardsView extends BaseView {

    private static final List<Integer> REWARD_SLOTS = List.of(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    );

    /**
     * Creates the nation reward view.
     */
    public TownNationRewardsView() {
        super();
    }

    @Override
    protected @NotNull String getKey() {
        return "town_nation_rewards_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "         ",
            "         ",
            "         ",
            "   q h   ",
            "r        "
        };
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final NationCreationProgressSnapshot snapshot = TownNationViewSupport.resolveCreationSnapshot(render);
        if (snapshot == null) {
            render.slot(22).renderWith(() -> this.createMissingItem(player));
            return;
        }

        render.layoutSlot('s', this.createSummaryItem(player, snapshot));
        render.layoutSlot('q', this.createRequirementsShortcut(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownNationRequirementsView.class,
                this.createNavigationData(clickContext)
            ));
        render.layoutSlot('h', this.createHubShortcut(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownNationView.class,
                this.createNavigationData(clickContext)
            ));
        render.layoutSlot('r', this.createReturnItem(player))
            .onClick(SlotClickContext::back);

        for (int index = 0; index < REWARD_SLOTS.size(); index++) {
            final int slot = REWARD_SLOTS.get(index);
            if (index >= snapshot.rewards().size()) {
                render.slot(slot).renderWith(() -> this.createEmptyItem(player));
                continue;
            }

            final TownLevelRewardSnapshot reward = snapshot.rewards().get(index);
            render.slot(slot).renderWith(() -> this.createRewardItem(player, reward));
        }
    }

    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private @NotNull Map<String, Object> createNavigationData(final @NotNull Context context) {
        final Map<String, Object> data = new LinkedHashMap<>();
        final Map<String, Object> copiedData = TownNationViewSupport.copyInitialData(context);
        if (copiedData != null) {
            data.putAll(TownNationViewSupport.stripTransientData(copiedData));
        }
        return data;
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @NotNull NationCreationProgressSnapshot snapshot
    ) {
        return UnifiedBuilderFactory.item(Material.CHEST)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholder("reward_count", snapshot.rewards().size())
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createRewardItem(
        final @NotNull Player player,
        final @NotNull TownLevelRewardSnapshot reward
    ) {
        final ItemStack displayItem = reward.displayItem() == null
            ? new ItemStack(Material.CHEST)
            : reward.displayItem();
        return UnifiedBuilderFactory.item(displayItem)
            .setName(this.i18n("entry.name", player)
                .withPlaceholder("reward_name", reward.title())
                .build()
                .component())
            .setLore(this.i18n("entry.lore", player)
                .withPlaceholders(Map.of(
                    "reward_name", reward.title(),
                    "reward_type", reward.typeId(),
                    "description", reward.description()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createRequirementsShortcut(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
            .setName(this.i18n("requirements.name", player).build().component())
            .setLore(this.i18n("requirements.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createHubShortcut(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BEACON)
            .setName(this.i18n("hub.name", player).build().component())
            .setLore(this.i18n("hub.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createReturnItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.ARROW)
            .setName(this.i18n("return.name", player).build().component())
            .setLore(this.i18n("return.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEmptyItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(this.i18n("empty.name", player).build().component())
            .setLore(this.i18n("empty.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMissingItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
