/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.core.view;

import com.raindropcentral.core.RCoreImpl;
import com.raindropcentral.core.service.central.DropletClaimService;
import com.raindropcentral.core.service.central.cookie.DropletCookieDefinition;
import com.raindropcentral.core.service.central.cookie.DropletCookieEffectType;
import com.raindropcentral.rplatform.job.JobBridge;
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
 * Paginated job picker for tagged droplet cookies.
 */
public final class DropletJobSelectionView extends APaginatedView<JobBridge.JobDescriptor> {

    private final State<RCoreImpl> plugin = initialState("plugin");
    private final State<List<JobBridge.JobDescriptor>> jobs = initialState("jobs");
    private final State<ItemStack> cookie = initialState("cookie");
    private final State<DropletCookieDefinition> definition = initialState("definition");

    @Override
    protected @NotNull String getKey() {
        return "rc_claim_droplet_jobs_ui";
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
    protected @NotNull CompletableFuture<List<JobBridge.JobDescriptor>> getAsyncPaginationSource(final @NotNull Context context) {
        final List<JobBridge.JobDescriptor> availableJobs = this.jobs.get(context);
        return CompletableFuture.completedFuture(availableJobs == null ? List.of() : List.copyOf(availableJobs));
    }

    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull JobBridge.JobDescriptor entry
    ) {
        final Player player = context.getPlayer();
        final DropletClaimService claimService = this.plugin.get(context).getDropletClaimService();
        final double currentLevel = claimService.getCurrentJobLevel(player, entry);
        final DropletCookieDefinition resolvedDefinition = this.definition.get(context);
        final String loreKey;
        if (resolvedDefinition != null && resolvedDefinition.effectType() == DropletCookieEffectType.JOB_LEVEL) {
            loreKey = "entry.level_lore";
        } else if (resolvedDefinition != null && resolvedDefinition.effectType() == DropletCookieEffectType.JOB_VAULT_RATE) {
            loreKey = "entry.vault_boost_lore";
        } else {
            loreKey = "entry.xp_boost_lore";
        }
        final int ratePercent = resolvedDefinition == null ? 0 : resolvedDefinition.ratePercent();
        final long durationMinutes = resolvedDefinition == null ? 0L : resolvedDefinition.durationMinutes();

        builder.withItem(
                        UnifiedBuilderFactory.item(this.resolveIcon(entry))
                                .setName(this.i18n("entry.name", player)
                                        .withPlaceholders(Map.of("job_name", entry.displayName()))
                                        .build()
                                        .component())
                                .setLore(this.i18n(loreKey, player)
                                        .withPlaceholders(Map.of(
                                                "job_name", entry.displayName(),
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
                .onClick(clickContext -> this.handleJobClick(clickContext, entry));
    }

    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final List<JobBridge.JobDescriptor> entries = this.jobs.get(render);
        final List<JobBridge.JobDescriptor> source = entries == null ? List.of() : entries;
        render.slot(40).renderWith(() -> this.createSummaryItem(player, source.size()));
        if (source.isEmpty()) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
    }

    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void handleJobClick(
            final @NotNull SlotClickContext clickContext,
            final @NotNull JobBridge.JobDescriptor entry
    ) {
        clickContext.setCancelled(true);
        final ItemStack cookieItem = this.cookie.get(clickContext);
        final DropletCookieDefinition resolvedDefinition = this.definition.get(clickContext);
        if (cookieItem == null || resolvedDefinition == null) {
            clickContext.closeForPlayer();
            return;
        }

        this.plugin.get(clickContext).getDropletClaimService()
                .handleJobSelection(clickContext.getPlayer(), entry, cookieItem, resolvedDefinition);
    }

    private @NotNull Material resolveIcon(final @NotNull JobBridge.JobDescriptor descriptor) {
        return switch (descriptor.integrationId().toLowerCase()) {
            case "jobsreborn" -> Material.GOLD_NUGGET;
            default -> Material.EMERALD;
        };
    }

    private @NotNull ItemStack createSummaryItem(final @NotNull Player player, final int jobCount) {
        return UnifiedBuilderFactory.item(Material.NETHER_STAR)
                .setName(this.i18n("summary.name", player)
                        .withPlaceholders(Map.of("job_count", jobCount))
                        .build()
                        .component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of("job_count", jobCount))
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
