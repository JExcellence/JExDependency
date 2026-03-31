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
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.repository.RRDTPlayer;
import com.raindropcentral.rdt.database.repository.RRTown;
import com.raindropcentral.rdt.utils.TownPermissions;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated browser for rewards granted when a town levels up.
 *
 * <p>This view is informational and lists all configured rewards for the next target level.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownLevelUpRewardsView extends APaginatedView<TownLevelUpSupport.ResolvedTownReward> {

    private final State<RDT> rdt = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");

    /**
     * Creates the town level-up rewards browser.
     */
    public TownLevelUpRewardsView() {
        super(TownLevelUpView.class);
    }

    /**
     * Returns the i18n key namespace for this view.
     *
     * @return translation key
     */
    @Override
    protected @NotNull String getKey() {
        return "town_level_up_rewards_ui";
    }

    /**
     * Returns inventory layout for summary and paginated reward entries.
     *
     * @return layout rows
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
                "    s    ",
                " OOOOOOO ",
                " OOOOOOO ",
                " OOOOOOO ",
                " OOOOOOO ",
                "   <p>   "
        };
    }

    /**
     * Resolves placeholders for translated inventory title.
     *
     * @param openContext open context
     * @return title placeholders
     */
    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        final RTown town = this.resolveTown(openContext, openContext.getPlayer());
        final RDT plugin = this.resolvePlugin(openContext);
        final Integer targetLevel = plugin == null || town == null
                ? null
                : TownLevelUpSupport.resolveNextLevel(plugin, town);

        return Map.of(
                "town_name", town == null ? "Unknown" : town.getTownName(),
                "target_level", targetLevel == null ? "-" : targetLevel
        );
    }

    /**
     * Cancels default inventory movement behavior.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    /**
     * Verifies access before initial rendering.
     *
     * @param render render context
     * @param player viewer
     */
    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        if (!this.verifyViewerAccess(render, player)) {
            player.closeInventory();
            return;
        }
        super.onFirstRender(render, player);
    }

    /**
     * Loads configured rewards for the next available level.
     *
     * @param context view context
     * @return future reward list
     */
    @Override
    protected @NotNull CompletableFuture<List<TownLevelUpSupport.ResolvedTownReward>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final RDT plugin = this.resolvePlugin(context);
        final RTown town = this.resolveTown(context, context.getPlayer());
        if (plugin == null || town == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        final Integer targetLevel = TownLevelUpSupport.resolveNextLevel(plugin, town);
        if (targetLevel == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        return CompletableFuture.completedFuture(
                TownLevelUpSupport.getConfiguredRewards(plugin, targetLevel)
        );
    }

    /**
     * Renders one reward entry.
     *
     * @param context context
     * @param builder item builder
     * @param index entry index
     * @param entry reward entry
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull TownLevelUpSupport.ResolvedTownReward entry
    ) {
        final Player player = context.getPlayer();
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null) {
            builder.withItem(this.createUnavailableItem(player));
            return;
        }

        builder.withItem(this.createRewardItem(plugin, player, entry));
    }

    /**
     * Renders summary controls for this paginated rewards view.
     *
     * @param render render context
     * @param player viewer
     */
    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RDT plugin = this.resolvePlugin(render);
        final RTown town = this.resolveTown(render, player);
        if (plugin == null || town == null) {
            render.layoutSlot('s', this.createUnavailableItem(player));
            return;
        }

        final Integer targetLevel = TownLevelUpSupport.resolveNextLevel(plugin, town);
        if (targetLevel == null) {
            render.layoutSlot('s', this.createMaxLevelItem(player, town));
            render.slot(22).renderWith(() -> this.createMaxLevelItem(player, town));
            return;
        }

        final List<TownLevelUpSupport.ResolvedTownReward> rewards =
                TownLevelUpSupport.getConfiguredRewards(plugin, targetLevel);
        render.layoutSlot('s', this.createSummaryItem(player, town, targetLevel, rewards));

        if (rewards.isEmpty()) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player, targetLevel));
        }
    }

    private @NotNull ItemStack createRewardItem(
            final @NotNull RDT plugin,
            final @NotNull Player player,
            final @NotNull TownLevelUpSupport.ResolvedTownReward reward
    ) {
        final List<Component> lore = new ArrayList<>(this.i18n("entry.lore", player)
                .withPlaceholders(Map.of(
                        "reward_key", reward.key(),
                        "reward_type", this.resolveTypeLabel(reward),
                        "status", this.resolveStatusPlaceholder(player, reward),
                        "summary", reward.summary()
                ))
                .build()
                .children());
        lore.add(Component.empty());
        lore.add(this.i18n("entry.details", player).build().component());
        for (final String detailLine : TownLevelUpSupport.buildRewardDetailLines(plugin, reward)) {
            lore.add(Component.text(detailLine, NamedTextColor.GRAY));
        }

        return UnifiedBuilderFactory.item(TownLevelUpSupport.resolveRewardMaterial(reward))
                .setName(this.i18n("entry.name", player)
                        .withPlaceholder("reward_name", TownLevelUpSupport.resolveRewardDisplayName(reward))
                        .build()
                        .component())
                .setLore(lore)
                .setGlowing(reward.operational() && reward.reward() != null)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final @NotNull RTown town,
            final int targetLevel,
            final @NotNull List<TownLevelUpSupport.ResolvedTownReward> rewards
    ) {
        return UnifiedBuilderFactory.item(Material.CHEST)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "current_level", town.getTownLevel(),
                                "target_level", targetLevel,
                                "reward_count", rewards.size()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createUnavailableItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("entry.unavailable.name", player).build().component())
                .setLore(this.i18n("entry.unavailable.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createEmptyItem(final @NotNull Player player, final int targetLevel) {
        return UnifiedBuilderFactory.item(Material.GRAY_STAINED_GLASS_PANE)
                .setName(this.i18n("empty.name", player).build().component())
                .setLore(this.i18n("empty.lore", player)
                        .withPlaceholder("target_level", targetLevel)
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createMaxLevelItem(
            final @NotNull Player player,
            final @NotNull RTown town
    ) {
        return UnifiedBuilderFactory.item(Material.LIME_STAINED_GLASS_PANE)
                .setName(this.i18n("max_level.name", player).build().component())
                .setLore(this.i18n("max_level.lore", player)
                        .withPlaceholder("current_level", town.getTownLevel())
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private boolean verifyViewerAccess(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RTown town = this.resolveTown(context, player);
        if (town == null) {
            this.i18n("feedback.town_unavailable", player).includePrefix().build().sendMessage();
            return false;
        }

        final RDTPlayer townPlayer = this.resolveTownPlayer(context, player);
        if (!town.hasTownPermission(townPlayer, TownPermissions.TOWN_LEVEL_UP)) {
            this.i18n("feedback.no_permission", player)
                    .includePrefix()
                    .withPlaceholder("permission", TownPermissions.TOWN_LEVEL_UP.getPermissionKey())
                    .build()
                    .sendMessage();
            return false;
        }

        return true;
    }

    private @NotNull String resolveStatusPlaceholder(
            final @NotNull Player player,
            final @NotNull TownLevelUpSupport.ResolvedTownReward reward
    ) {
        final String key = reward.operational() && reward.reward() != null
                ? "entry.status.available"
                : "entry.status.unavailable";
        return this.i18n(key, player).build().getI18nVersionWrapper().asPlaceholder();
    }

    private @NotNull String resolveTypeLabel(final @NotNull TownLevelUpSupport.ResolvedTownReward reward) {
        final String rawType = reward.reward() == null
                ? String.valueOf(reward.definition().getOrDefault("type", "unknown"))
                : reward.reward().getTypeId();
        final String normalized = rawType.replace('-', ' ').replace('_', ' ').toLowerCase(Locale.ROOT);
        final String[] words = normalized.split("\\s+");
        final StringBuilder builder = new StringBuilder();
        for (final String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.length() == 0 ? rawType : builder.toString();
    }

    private @Nullable RTown resolveTown(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null) {
            return null;
        }

        final RRTown townRepository = plugin.getTownRepository();
        if (townRepository == null) {
            return null;
        }

        final UUID resolvedTownUuid = this.resolveTownUuid(context, player, plugin);
        if (resolvedTownUuid == null) {
            return null;
        }
        return townRepository.findByTownUUID(resolvedTownUuid);
    }

    private @Nullable UUID resolveTownUuid(
            final @NotNull Context context,
            final @NotNull Player player,
            final @NotNull RDT plugin
    ) {
        try {
            final UUID explicitTownUuid = this.townUuid.get(context);
            if (explicitTownUuid != null) {
                return explicitTownUuid;
            }
        } catch (final Exception ignored) {
        }

        final RRDTPlayer playerRepository = plugin.getPlayerRepository();
        if (playerRepository == null) {
            return null;
        }

        final RDTPlayer rdtPlayer = playerRepository.findByPlayer(player.getUniqueId());
        return rdtPlayer == null ? null : rdtPlayer.getTownUUID();
    }

    private @Nullable RDTPlayer resolveTownPlayer(
            final @NotNull Context context,
            final @NotNull Player player
    ) {
        final RDT plugin = this.resolvePlugin(context);
        if (plugin == null || plugin.getPlayerRepository() == null) {
            return null;
        }
        return plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
    }

    private @Nullable RDT resolvePlugin(final @NotNull Context context) {
        try {
            return this.rdt.get(context);
        } catch (final Exception ignored) {
            return null;
        }
    }
}
