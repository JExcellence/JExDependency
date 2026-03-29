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

package com.raindropcentral.core.view;

import com.raindropcentral.core.RCoreImpl;
import com.raindropcentral.core.service.central.DropletClaimService;
import com.raindropcentral.core.service.central.cookie.DropletCookieDefinition;
import com.raindropcentral.core.service.central.cookie.DropletCookieEffectType;
import com.raindropcentral.rplatform.skill.SkillBridge;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated skill picker for tagged droplet cookies.
 */
public final class DropletSkillSelectionView extends APaginatedView<SkillBridge.SkillDescriptor> {

    private final State<RCoreImpl> plugin = initialState("plugin");
    private final State<List<SkillBridge.SkillDescriptor>> skills = initialState("skills");
    private final State<ItemStack> cookie = initialState("cookie");
    private final State<DropletCookieDefinition> definition = initialState("definition");

    /**
     * Creates the skill selection view.
     */
    public DropletSkillSelectionView() {
        super();
    }

    @Override
    protected @NotNull String getKey() {
        return "rc_claim_droplet_skills_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
                "         ",
                " OOOOOOO ",
                " OOOOOOO ",
                " OOOOOOO ",
                "    S    ",
                "   <p>   "
        };
    }

    @Override
    protected @NotNull CompletableFuture<List<SkillBridge.SkillDescriptor>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final List<SkillBridge.SkillDescriptor> availableSkills = this.skills.get(context);
        return CompletableFuture.completedFuture(availableSkills == null ? List.of() : List.copyOf(availableSkills));
    }

    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull SkillBridge.SkillDescriptor entry
    ) {
        final Player player = context.getPlayer();
        final DropletClaimService claimService = this.plugin.get(context).getDropletClaimService();
        final double currentLevel = claimService.getCurrentSkillLevel(player, entry);
        final DropletCookieDefinition resolvedDefinition = this.definition.get(context);
        final String loreKey = resolvedDefinition != null && resolvedDefinition.effectType() == DropletCookieEffectType.SKILL_LEVEL
                ? "entry.level_lore"
                : "entry.boost_lore";
        final int ratePercent = resolvedDefinition == null ? 0 : resolvedDefinition.ratePercent();
        final long durationMinutes = resolvedDefinition == null ? 0L : resolvedDefinition.durationMinutes();

        builder.withItem(
                        UnifiedBuilderFactory.item(this.resolveIcon(entry))
                                .setName(this.i18n("entry.name", player)
                                        .withPlaceholders(Map.of("skill_name", entry.displayName()))
                                        .build()
                                        .component())
                                .setLore(this.i18n(loreKey, player)
                                        .withPlaceholders(Map.of(
                                                "skill_name", entry.displayName(),
                                                "plugin_name", entry.pluginName(),
                                                "current_level", currentLevel,
                                                "rate_percent", ratePercent,
                                                "duration_minutes", durationMinutes
                                        ))
                                        .build()
                                        .children())
                                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                                .build()
                )
                .onClick(clickContext -> this.handleSkillClick(clickContext, entry));
    }

    @Override
    protected void onPaginatedRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final List<SkillBridge.SkillDescriptor> entries = this.skills.get(render);
        final List<SkillBridge.SkillDescriptor> source = entries == null ? List.of() : entries;
        render.slot(40).renderWith(() -> this.createSummaryItem(player, source.size()));
        if (source.isEmpty()) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
    }

    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void handleSkillClick(
            final @NotNull SlotClickContext clickContext,
            final @NotNull SkillBridge.SkillDescriptor entry
    ) {
        clickContext.setCancelled(true);
        final ItemStack cookieItem = this.cookie.get(clickContext);
        final DropletCookieDefinition resolvedDefinition = this.definition.get(clickContext);
        if (cookieItem == null) {
            clickContext.closeForPlayer();
            return;
        }
        if (resolvedDefinition == null) {
            clickContext.closeForPlayer();
            return;
        }

        this.plugin.get(clickContext).getDropletClaimService()
                .handleSkillSelection(clickContext.getPlayer(), entry, cookieItem, resolvedDefinition);
    }

    private @NotNull Material resolveIcon(final @NotNull SkillBridge.SkillDescriptor descriptor) {
        return switch (descriptor.integrationId().toLowerCase()) {
            case "auraskills" -> Material.AMETHYST_SHARD;
            case "mcmmo" -> Material.IRON_PICKAXE;
            default -> Material.BOOK;
        };
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final int skillCount
    ) {
        return UnifiedBuilderFactory.item(Material.EXPERIENCE_BOTTLE)
                .setName(this.i18n("summary.name", player)
                        .withPlaceholders(Map.of("skill_count", skillCount))
                        .build()
                        .component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of("skill_count", skillCount))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createEmptyItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("empty.name", player).build().component())
                .setLore(this.i18n("empty.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }
}
