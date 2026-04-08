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

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.service.TownCreationProgressSnapshot;
import com.raindropcentral.rdt.view.main.TownHubView;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dedicated pre-town creation hub backed by Nexus level-one requirements and rewards.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownCreationProgressView extends BaseView {

    private final State<RDT> plugin = initialState("plugin");

    /**
     * Creates the town-creation hub.
     */
    public TownCreationProgressView() {
        super(TownHubView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "town_creation_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "   q w   ",
            "    c    ",
            "         ",
            "         ",
            "r        "
        };
    }

    /**
     * Continues from the naming step into the final confirmation view.
     *
     * @param origin previous context
     * @param target current context
     */
    @Override
    public void onResume(final @NotNull Context origin, final @NotNull Context target) {
        final Map<String, Object> data = this.extractData(target) != null
            ? this.extractData(target)
            : this.extractData(origin);
        if (data != null && data.get("draftTownName") instanceof String draftTownName) {
            target.openForPlayer(
                CreateTownConfirmView.class,
                Map.of(
                    "plugin", this.plugin.get(target),
                    "draftTownName", draftTownName
                )
            );
            return;
        }
        target.update();
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final TownCreationProgressSnapshot snapshot = TownCreationViewSupport.resolveSnapshot(render);
        if (snapshot == null) {
            render.slot(22).renderWith(() -> this.createMissingItem(player));
            return;
        }

        render.layoutSlot('s', this.createSummaryItem(player, snapshot));
        render.layoutSlot('q', this.createRequirementsItem(player, snapshot))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownCreationRequirementsView.class,
                this.createNavigationData(clickContext)
            ));
        render.layoutSlot('w', this.createRewardsItem(player, snapshot))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownCreationRewardsView.class,
                this.createNavigationData(clickContext)
            ));
        render.layoutSlot('c', this.createCreateItem(player, snapshot))
            .onClick(clickContext -> this.handleCreateClick(clickContext, snapshot));
        render.layoutSlot('r', this.createReturnItem(player))
            .onClick(SlotClickContext::back);
    }

    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void handleCreateClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull TownCreationProgressSnapshot snapshot
    ) {
        if (!snapshot.available() || snapshot.alreadyInTown()) {
            return;
        }
        if (!snapshot.readyToCreate()) {
            clickContext.openForPlayer(TownCreationRequirementsView.class, this.createNavigationData(clickContext));
            return;
        }
        clickContext.openForPlayer(CreateTownNameAnvilView.class, this.createNavigationData(clickContext));
    }

    private @NotNull Map<String, Object> createNavigationData(final @NotNull Context context) {
        final Map<String, Object> copiedData = TownCreationViewSupport.copyInitialData(context);
        if (copiedData == null) {
            return new LinkedHashMap<>(Map.of("plugin", this.plugin.get(context)));
        }
        return new LinkedHashMap<>(TownCreationViewSupport.stripTransientData(copiedData));
    }

    private Map<String, Object> extractData(final @NotNull Context context) {
        return TownCreationViewSupport.copyInitialData(context);
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @NotNull TownCreationProgressSnapshot snapshot
    ) {
        return UnifiedBuilderFactory.item(Material.BEACON)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "progress_percent", Math.round(snapshot.progress() * 100.0D),
                    "requirement_count", snapshot.requirements().size(),
                    "reward_count", snapshot.rewards().size(),
                    "ready_state", snapshot.readyToCreate() ? "Ready" : "In Progress"
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createRequirementsItem(
        final @NotNull Player player,
        final @NotNull TownCreationProgressSnapshot snapshot
    ) {
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
            .setName(this.i18n("requirements.name", player).build().component())
            .setLore(this.i18n("requirements.lore", player)
                .withPlaceholders(Map.of(
                    "requirement_count", snapshot.requirements().size(),
                    "progress_percent", Math.round(snapshot.progress() * 100.0D)
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createRewardsItem(
        final @NotNull Player player,
        final @NotNull TownCreationProgressSnapshot snapshot
    ) {
        return UnifiedBuilderFactory.item(Material.CHEST)
            .setName(this.i18n("rewards.name", player).build().component())
            .setLore(this.i18n("rewards.lore", player)
                .withPlaceholder("reward_count", snapshot.rewards().size())
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createCreateItem(
        final @NotNull Player player,
        final @NotNull TownCreationProgressSnapshot snapshot
    ) {
        final Material material = !snapshot.available()
            ? Material.BARRIER
            : snapshot.readyToCreate()
                ? Material.LIME_DYE
                : Material.EXPERIENCE_BOTTLE;
        final String loreKey = !snapshot.available()
            ? snapshot.alreadyInTown()
                ? "create.locked.lore"
                : "create.unavailable.lore"
            : snapshot.readyToCreate()
                ? "create.ready.lore"
                : "create.progress.lore";
        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("create.name", player).build().component())
            .setLore(this.i18n(loreKey, player)
                .withPlaceholder("progress_percent", Math.round(snapshot.progress() * 100.0D))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .setGlowing(snapshot.readyToCreate())
            .build();
    }

    private @NotNull ItemStack createReturnItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.ARROW)
            .setName(this.i18n("return.name", player).build().component())
            .setLore(this.i18n("return.lore", player).build().children())
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
