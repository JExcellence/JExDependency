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

package com.raindropcentral.rda.view;

import com.raindropcentral.rda.PrestigeAdjustmentPreview;
import com.raindropcentral.rda.RDA;
import com.raindropcentral.rda.SkillConfig;
import com.raindropcentral.rda.SkillProfileSnapshot;
import com.raindropcentral.rda.SkillProgressionService;
import com.raindropcentral.rda.SkillType;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import com.raindropcentral.rplatform.view.ConfirmationView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated skill detail menu showing configured rates and progression status.
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
public final class RaSkillView extends APaginatedView<SkillConfig.RateDefinition> {

    private final State<RDA> rda = initialState("plugin");
    private final State<SkillType> skillType = initialState("skillType");

    /**
     * Creates the skill detail menu.
     */
    public RaSkillView() {
        super(RaSkillsView.class);
    }

    @Override
    protected String getKey() {
        return "ra_skill_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "    I    ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            "  <pP>   "
        };
    }

    @Override
    protected Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext open) {
        return this.getSkillType(open).getPlaceholders(open.getPlayer());
    }

    @Override
    protected CompletableFuture<List<SkillConfig.RateDefinition>> getAsyncPaginationSource(final @NotNull Context context) {
        final RDA plugin = this.rda.get(context);
        final SkillProgressionService progressionService =
            plugin == null ? null : plugin.getSkillProgressionService(this.getSkillType(context));
        if (progressionService == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        return CompletableFuture.completedFuture(progressionService.getRates());
    }

    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull SkillConfig.RateDefinition entry
    ) {
        final Player player = context.getPlayer();
        final String triggerName = PlainTextComponentSerializer.plainText().serialize(
            new I18n.Builder(entry.triggerType().getTranslationKey(), player).build().component()
        );
        builder.withItem(
            UnifiedBuilderFactory.item(entry.icon())
                .setName(this.i18n("rate.name", player)
                    .withPlaceholders(this.mergePlaceholders(player, context, Map.of("rate_name", entry.label())))
                    .build()
                    .component())
                .setLore(this.i18n("rate.lore", player)
                    .withPlaceholders(this.mergePlaceholders(player, context, Map.of(
                        "rate_name", entry.label(),
                        "rate_description", entry.resolveDescription(player).isBlank() ? "-" : entry.resolveDescription(player),
                        "rate_trigger", triggerName,
                        "rate_xp", entry.xp()
                    )))
                    .build()
                    .children())
                .build()
        );
    }

    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RDA plugin = this.rda.get(render);
        final SkillType resolvedSkillType = this.getSkillType(render);
        final SkillProfileSnapshot snapshot = this.getSnapshot(plugin, player, resolvedSkillType);
        final Material icon = this.getDisplayIcon(plugin, resolvedSkillType);
        final SkillConfig skillConfig = plugin == null ? null : plugin.getSkillConfig(resolvedSkillType);
        final PrestigeAdjustmentPreview prestigePreview = plugin == null
            ? null
            : plugin.previewPrestigeAdjustment(player, resolvedSkillType);

        render.layoutSlot(
            'I',
            UnifiedBuilderFactory.item(Material.EXPERIENCE_BOTTLE)
                .setName(this.i18n("info.name", player)
                    .withPlaceholders(resolvedSkillType.getPlaceholders(player))
                    .build()
                    .component())
                .setLore(this.i18n("info.lore", player)
                    .withPlaceholders(this.mergePlaceholders(player, render, Map.of(
                        "level", snapshot.displayLevelText(),
                        "prestige", snapshot.prestige(),
                        "prestige_symbols", snapshot.prestigeSymbols(),
                        "current_xp", snapshot.currentLevelXp(),
                        "xp_to_next", snapshot.xpToNextLevel(),
                        "xp_bonus_percent", snapshot.currentXpBonusPercent(),
                        "total_power", snapshot.totalPower(),
                        "soft_max_level", snapshot.softMaxLevel(),
                        "passive_count", skillConfig == null ? 0 : skillConfig.getPassiveAbilities().size(),
                        "active_name", skillConfig == null || skillConfig.getActiveAbility() == null
                            ? "-"
                            : skillConfig.getActiveAbility().name()
                    )))
                    .build()
                    .children())
                .build()
        );

        render.layoutSlot(
            'P',
            UnifiedBuilderFactory.item(icon)
                .setName(this.i18n("prestige.name", player)
                    .withPlaceholders(resolvedSkillType.getPlaceholders(player))
                    .build()
                    .component())
                .setLore(this.i18n("prestige.lore", player)
                    .withPlaceholders(this.mergePlaceholders(player, render, Map.of(
                        "prestige", snapshot.prestige(),
                        "max_prestiges", snapshot.maxPrestiges(),
                        "xp_bonus_percent", snapshot.currentXpBonusPercent()
                            + this.getPerPrestigeBonus(plugin, resolvedSkillType),
                        "reset_points", prestigePreview == null ? 0 : prestigePreview.getTotalResetPoints(),
                        "available_points", prestigePreview == null ? 0 : prestigePreview.unspentPointsAfterPrestige()
                    )))
                    .build()
                    .children())
                .build()
        ).displayIf(snapshot::prestigeAvailable).onClick(clickContext -> {
            if (plugin == null) {
                return;
            }

            final SkillProgressionService progressionService = plugin.getSkillProgressionService(resolvedSkillType);
            if (progressionService == null) {
                return;
            }

            new ConfirmationView.Builder()
                .withKey("ra_skill_prestige_confirmation")
                .withInitialData(this.mergePlaceholders(player, clickContext, Map.of(
                    "level", snapshot.displayLevelText(),
                    "prestige", snapshot.prestige() + 1,
                    "max_prestiges", snapshot.maxPrestiges(),
                    "xp_bonus_percent", snapshot.currentXpBonusPercent()
                        + this.getPerPrestigeBonus(plugin, resolvedSkillType),
                    "reset_points", prestigePreview == null ? 0 : prestigePreview.getTotalResetPoints(),
                    "available_points", prestigePreview == null ? 0 : prestigePreview.unspentPointsAfterPrestige()
                )))
                .withCallback(confirmed -> {
                    if (confirmed && progressionService.prestige(player) && plugin.getViewFrame() != null) {
                        plugin.getViewFrame().open(RaSkillView.class, player, Map.of(
                            "plugin", plugin,
                            "skillType", resolvedSkillType
                        ));
                    }
                })
                .withParentView(RaSkillView.class)
                .openFor(clickContext, player);
        });
    }

    private @NotNull SkillType getSkillType(final @NotNull Context context) {
        final SkillType resolved = this.skillType.get(context);
        return resolved == null ? SkillType.MINING : resolved;
    }

    private @NotNull Material getDisplayIcon(final RDA plugin, final @NotNull SkillType skillType) {
        final SkillConfig skillConfig = plugin == null ? null : plugin.getSkillConfig(skillType);
        return skillConfig == null ? skillType.getFallbackIcon() : skillConfig.getDisplayIcon();
    }

    private int getPerPrestigeBonus(final RDA plugin, final @NotNull SkillType skillType) {
        final SkillConfig skillConfig = plugin == null ? null : plugin.getSkillConfig(skillType);
        return skillConfig == null ? 0 : skillConfig.getPrestigeXpBonusPerPrestigePercent();
    }

    private @NotNull SkillProfileSnapshot getSnapshot(
        final RDA plugin,
        final @NotNull Player player,
        final @NotNull SkillType skillType
    ) {
        return plugin == null ? SkillProfileSnapshot.empty(skillType) : plugin.getSkillSnapshot(skillType, player);
    }

    private @NotNull Map<String, Object> mergePlaceholders(
        final @NotNull Player player,
        final @NotNull Context context,
        final @NotNull Map<String, Object> extraPlaceholders
    ) {
        final LinkedHashMap<String, Object> placeholders =
            new LinkedHashMap<>(this.getSkillType(context).getPlaceholders(player));
        placeholders.putAll(extraPlaceholders);
        return placeholders;
    }
}
