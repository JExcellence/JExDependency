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
 * Paginated town level-up requirement browser with click-to-progress support.
 *
 * <p>Clicking a requirement attempts to bank partial progress for consumable requirements
 * (currency/items) and persists that progress on the target {@link RTown}.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownLevelUpRequirementsView extends APaginatedView<TownLevelUpSupport.ResolvedTownRequirement> {

    private final State<RDT> rdt = initialState("plugin");
    private final State<UUID> townUuid = initialState("town_uuid");

    /**
     * Creates the town level-up requirement browser.
     */
    public TownLevelUpRequirementsView() {
        super(TownLevelUpView.class);
    }

    /**
     * Returns the i18n key namespace for this view.
     *
     * @return translation key
     */
    @Override
    protected @NotNull String getKey() {
        return "town_level_up_requirements_ui";
    }

    /**
     * Returns inventory layout for summary and paginated requirement entries.
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
     * Loads configured requirements for the next available level.
     *
     * @param context view context
     * @return future requirement list
     */
    @Override
    protected @NotNull CompletableFuture<List<TownLevelUpSupport.ResolvedTownRequirement>> getAsyncPaginationSource(
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
                TownLevelUpSupport.getConfiguredRequirements(
                        plugin,
                        context.getPlayer(),
                        town,
                        targetLevel
                )
        );
    }

    /**
     * Renders one requirement entry.
     *
     * @param context context
     * @param builder item builder
     * @param index entry index
     * @param entry requirement entry
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull TownLevelUpSupport.ResolvedTownRequirement entry
    ) {
        final Player player = context.getPlayer();
        final RDT plugin = this.resolvePlugin(context);
        final RTown town = this.resolveTown(context, player);
        if (plugin == null || town == null) {
            builder.withItem(this.createUnavailableItem(player));
            return;
        }

        builder.withItem(this.createRequirementItem(plugin, player, town, entry))
                .updateOnClick()
                .onClick(click -> this.handleRequirementClick(click, entry));
    }

    /**
     * Renders summary controls for this paginated requirements view.
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

        final List<TownLevelUpSupport.ResolvedTownRequirement> requirements =
                TownLevelUpSupport.getConfiguredRequirements(plugin, player, town, targetLevel);
        render.layoutSlot('s', this.createSummaryItem(player, plugin, town, targetLevel, requirements));

        if (requirements.isEmpty()) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player, targetLevel));
        }
    }

    private @NotNull ItemStack createRequirementItem(
            final @NotNull RDT plugin,
            final @NotNull Player player,
            final @NotNull RTown town,
            final @NotNull TownLevelUpSupport.ResolvedTownRequirement requirement
    ) {
        final boolean met = TownLevelUpSupport.isRequirementMet(plugin, player, town, requirement);
        final int progress = TownLevelUpSupport.getProgressPercentage(plugin, player, town, requirement);
        final String progressBar = this.buildProgressBar(player, progress);
        final String status = this.resolveStatusPlaceholder(player, requirement, met);
        final List<Component> lore = new ArrayList<>(this.i18n("entry.lore", player)
                .withPlaceholders(Map.of(
                        "requirement_key", requirement.key(),
                        "requirement_type", this.resolveTypeLabel(requirement),
                        "status", status,
                        "progress", progress + "%",
                        "progress_bar", progressBar,
                        "summary", requirement.summary()
                ))
                .build()
                .children());

        lore.add(Component.empty());
        lore.add(this.i18n("entry.details", player).build().component());
        for (final String detailLine : TownLevelUpSupport.buildRequirementDetailLines(plugin, player, town, requirement)) {
            lore.add(Component.text(detailLine, NamedTextColor.GRAY));
        }

        return UnifiedBuilderFactory.item(TownLevelUpSupport.resolveRequirementMaterial(requirement))
                .setName(this.i18n("entry.name", player)
                        .withPlaceholder("requirement_name", TownLevelUpSupport.resolveRequirementDisplayName(requirement))
                        .build()
                        .component())
                .setLore(lore)
                .setGlowing(met)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final @NotNull RDT plugin,
            final @NotNull RTown town,
            final int targetLevel,
            final @NotNull List<TownLevelUpSupport.ResolvedTownRequirement> requirements
    ) {
        final long metCount = requirements.stream()
                .filter(requirement -> TownLevelUpSupport.isRequirementMet(plugin, player, town, requirement))
                .count();
        final TownLevelUpSupport.RequirementAvailability availability =
                TownLevelUpSupport.resolveRequirementAvailability(plugin, player, town, requirements);

        return UnifiedBuilderFactory.item(Material.BOOKSHELF)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "current_level", town.getTownLevel(),
                                "target_level", targetLevel,
                                "requirement_count", requirements.size(),
                                "met_count", metCount,
                                "state", this.resolveAvailabilityLabel(player, availability)
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
        return UnifiedBuilderFactory.item(Material.LIME_STAINED_GLASS_PANE)
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

    private void handleRequirementClick(
            final @NotNull SlotClickContext click,
            final @NotNull TownLevelUpSupport.ResolvedTownRequirement requirement
    ) {
        click.setCancelled(true);

        final Player player = click.getPlayer();
        final RDT plugin = this.resolvePlugin(click);
        if (plugin == null || plugin.getTownRepository() == null) {
            this.i18n("feedback.system_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        final RTown town = this.resolveTown(click, player);
        if (town == null) {
            this.i18n("feedback.town_unavailable", player).includePrefix().build().sendMessage();
            return;
        }

        if (!this.verifyViewerAccess(click, player)) {
            return;
        }

        final TownLevelUpSupport.ProgressUpdateResult result =
                TownLevelUpSupport.attemptRequirementProgress(plugin, player, town, requirement);
        if (result.changed()) {
            plugin.getTownRepository().update(town);
        }

        this.sendProgressFeedback(click, requirement, result);
        this.reopen(click, plugin, town.getIdentifier());
    }

    private void sendProgressFeedback(
            final @NotNull SlotClickContext click,
            final @NotNull TownLevelUpSupport.ResolvedTownRequirement requirement,
            final @NotNull TownLevelUpSupport.ProgressUpdateResult result
    ) {
        final String key = switch (result.status()) {
            case UNSUPPORTED -> "feedback.unsupported";
            case UNAVAILABLE -> "feedback.unavailable";
            case NO_PROGRESS -> "feedback.no_progress";
            case PROGRESSED -> "feedback.progress_saved";
            case COMPLETE -> "feedback.complete";
        };

        this.i18n(key, click.getPlayer())
                .includePrefix()
                .withPlaceholders(Map.of(
                        "requirement", TownLevelUpSupport.resolveRequirementDisplayName(requirement),
                        "progress_bar", this.buildProgressBar(click.getPlayer(), result.progressPercentage()),
                        "progress", result.progressPercentage() + "%"
                ))
                .build()
                .sendMessage();
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

    private void reopen(
            final @NotNull Context context,
            final @NotNull RDT plugin,
            final @NotNull UUID townIdentifier
    ) {
        context.openForPlayer(
                TownLevelUpRequirementsView.class,
                Map.of(
                        "plugin", plugin,
                        "town_uuid", townIdentifier
                )
        );
    }

    private @NotNull String resolveAvailabilityLabel(
            final @NotNull Player player,
            final @NotNull TownLevelUpSupport.RequirementAvailability availability
    ) {
        final String key = switch (availability) {
            case READY -> "summary.state.ready";
            case PENDING -> "summary.state.pending";
            case UNAVAILABLE -> "summary.state.unavailable";
        };
        return this.i18n(key, player).build().getI18nVersionWrapper().asPlaceholder();
    }

    private @NotNull String resolveStatusPlaceholder(
            final @NotNull Player player,
            final @NotNull TownLevelUpSupport.ResolvedTownRequirement requirement,
            final boolean met
    ) {
        final String key;
        if (!requirement.operational() || requirement.requirement() == null) {
            key = "entry.status.unavailable";
        } else if (met) {
            key = "entry.status.met";
        } else {
            key = "entry.status.pending";
        }
        return this.i18n(key, player).build().getI18nVersionWrapper().asPlaceholder();
    }

    private @NotNull String buildProgressBar(final @NotNull Player player, final int percentage) {
        return TownLevelUpSupport.buildProgressBar(
                percentage,
                this.resolvePlaceholder("progress_bar.empty", player),
                this.resolvePlaceholder("progress_bar.partial", player),
                this.resolvePlaceholder("progress_bar.filled", player)
        );
    }

    private @NotNull String resolvePlaceholder(final @NotNull String key, final @NotNull Player player) {
        return this.i18n(key, player).build().getI18nVersionWrapper().asPlaceholder();
    }

    private @NotNull String resolveTypeLabel(final @NotNull TownLevelUpSupport.ResolvedTownRequirement requirement) {
        final String rawType = requirement.requirement() == null
                ? String.valueOf(requirement.definition().getOrDefault("type", "unknown"))
                : requirement.requirement().getTypeId();
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
