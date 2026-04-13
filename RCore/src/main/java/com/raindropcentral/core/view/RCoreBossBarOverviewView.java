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
import com.raindropcentral.core.service.RCoreBossBarService;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.jextranslate.i18n.I18n;
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
 * Root settings view listing every registered boss-bar provider available on the server.
 *
 * @author Codex
 * @since 2.1.0
 * @version 2.1.0
 */
public final class RCoreBossBarOverviewView extends APaginatedView<RCoreBossBarService.ProviderDefinition> {

    private final State<RCoreImpl> plugin = initialState("plugin");

    /**
     * Creates the root boss-bar overview view.
     */
    public RCoreBossBarOverviewView() {
        super();
    }

    @Override
    protected @NotNull String getKey() {
        return "rc_boss_bar_ui";
    }

    @Override
    protected @NotNull CompletableFuture<List<RCoreBossBarService.ProviderDefinition>> getAsyncPaginationSource(
        final @NotNull Context context
    ) {
        final RCoreImpl pluginRuntime = this.plugin.get(context);
        final List<RCoreBossBarService.ProviderDefinition> providers = pluginRuntime == null
            || pluginRuntime.getBossBarService() == null
            ? List.of()
            : pluginRuntime.getBossBarService().getRegisteredProviders();
        return CompletableFuture.completedFuture(providers);
    }

    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull RCoreBossBarService.ProviderDefinition entry
    ) {
        final Player player = context.getPlayer();
        final RCoreBossBarService bossBarService = this.plugin.get(context).getBossBarService();
        final RCoreBossBarService.PreferenceSnapshot snapshot = bossBarService.resolvePreferences(
            player.getUniqueId(),
            entry.key()
        );

        builder.withItem(
            UnifiedBuilderFactory.item(entry.iconMaterial())
                .setName(this.translate(entry.nameTranslationKey(), player))
                .setLore(List.of(
                    this.translate(entry.descriptionTranslationKey(), player),
                    this.i18n(snapshot.enabled() ? "entry.enabled" : "entry.disabled", player).build().component(),
                    this.i18n("entry.options", player)
                        .withPlaceholder("option_count", entry.options().size())
                        .build()
                        .component(),
                    this.i18n("entry.open", player).build().component()
                ))
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build()
        ).onClick(clickContext -> this.openProviderSettings(clickContext, entry.key()));
    }

    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final List<RCoreBossBarService.ProviderDefinition> providers = this.plugin.get(render) == null
            || this.plugin.get(render).getBossBarService() == null
            ? List.of()
            : this.plugin.get(render).getBossBarService().getRegisteredProviders();

        render.slot(40).renderWith(() -> this.createSummaryItem(player, providers.size()));
        if (providers.isEmpty()) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
    }

    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void openProviderSettings(final @NotNull SlotClickContext clickContext, final @NotNull String providerKey) {
        clickContext.setCancelled(true);
        clickContext.openForPlayer(RCoreBossBarProviderView.class, Map.of(
            "plugin", this.plugin.get(clickContext),
            "providerKey", providerKey
        ));
    }

    private @NotNull ItemStack createSummaryItem(final @NotNull Player player, final int providerCount) {
        return UnifiedBuilderFactory.item(Material.NETHER_STAR)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholder("provider_count", providerCount)
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

    private @NotNull net.kyori.adventure.text.Component translate(
        final @NotNull String translationKey,
        final @NotNull Player player
    ) {
        return new I18n.Builder(translationKey, player).build().component();
    }
}
