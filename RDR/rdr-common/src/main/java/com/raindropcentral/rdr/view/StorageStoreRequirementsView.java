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

package com.raindropcentral.rdr.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.configs.ConfigSection;
import com.raindropcentral.rdr.database.entity.RDRPlayer;
import com.raindropcentral.rplatform.view.APaginatedView;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;

/**
 * Paginated browser for the configured storage shop requirements of a single purchase tier.
 *
 * <p>This view lets players inspect each configured requirement with its configured icon, current
 * progress, and type-specific details before attempting a purchase from the storage store.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageStoreRequirementsView extends APaginatedView<StorageStorePricingSupport.ResolvedStoreRequirement> {

    private final State<RDR> rdr = initialState("plugin");
    private final State<Integer> purchaseNumber = initialState("purchase_number");

    /**
     * Creates the paginated storage store requirement browser.
     */
    public StorageStoreRequirementsView() {
        super(StorageStoreView.class);
    }

    @Override
    protected String getKey() {
        return "storage_store_requirements_ui";
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        final int currentPurchaseNumber = this.resolvePurchaseNumber(openContext);
        final ConfigSection config = this.rdr.get(openContext).getDefaultConfig();
        return Map.of(
            "purchase_number", currentPurchaseNumber,
            "storage_number", config.getStartingStorages() + currentPurchaseNumber
        );
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            "  < p >  "
        };
    }

    @Override
    protected CompletableFuture<List<StorageStorePricingSupport.ResolvedStoreRequirement>> getAsyncPaginationSource(
        final @NotNull Context context
    ) {
        final RDR plugin = this.rdr.get(context);
        final ConfigSection config = plugin.getDefaultConfig();
        return CompletableFuture.completedFuture(
            StorageStorePricingSupport.getConfiguredStoreRequirements(
                plugin,
                config,
                context.getPlayer(),
                this.resolvePurchaseNumber(context)
            )
        );
    }

    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull StorageStorePricingSupport.ResolvedStoreRequirement entry
    ) {
        builder.withItem(this.createRequirementItem(this.rdr.get(context), context.getPlayer(), entry))
            .updateOnClick()
            .onClick(clickContext -> this.handleRequirementClick(clickContext, entry));
    }

    @Override
    protected void onPaginatedRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final RDR plugin = this.rdr.get(render);
        final ConfigSection config = plugin.getDefaultConfig();
        final int currentPurchaseNumber = this.resolvePurchaseNumber(render);
        final List<StorageStorePricingSupport.ResolvedStoreRequirement> requirements =
            StorageStorePricingSupport.getConfiguredStoreRequirements(
                plugin,
                config,
                player,
                currentPurchaseNumber
            );

        render.layoutSlot(
            's',
            this.createSummaryItem(
                player,
                plugin,
                config,
                currentPurchaseNumber,
                requirements
            )
        );

        if (requirements.isEmpty()) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
    }

    /**
     * Cancels item interaction so GUI entries cannot be moved.
     *
     * @param click slot click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private int resolvePurchaseNumber(final @NotNull Context context) {
        final Integer configuredPurchaseNumber = this.purchaseNumber.get(context);
        return configuredPurchaseNumber == null || configuredPurchaseNumber < 1
            ? 1
            : configuredPurchaseNumber;
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @NotNull RDR plugin,
        final @NotNull ConfigSection config,
        final int currentPurchaseNumber,
        final @NotNull List<StorageStorePricingSupport.ResolvedStoreRequirement> requirements
    ) {
        final long metCount = requirements.stream()
            .filter(requirement -> StorageStoreRequirementBrowserSupport.isRequirementMet(plugin, player, requirement))
            .count();

        return UnifiedBuilderFactory.item(Material.BOOKSHELF)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "purchase_number", currentPurchaseNumber,
                    "storage_number", config.getStartingStorages() + currentPurchaseNumber,
                    "requirement_count", requirements.size(),
                    "met_count", metCount
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createRequirementItem(
        final @NotNull RDR plugin,
        final @NotNull Player player,
        final @NotNull StorageStorePricingSupport.ResolvedStoreRequirement requirement
    ) {
        final boolean met = StorageStoreRequirementBrowserSupport.isRequirementMet(plugin, player, requirement);
        final String status = this.resolveStatusPlaceholder(player, requirement, met);
        final int progress = StorageStoreRequirementBrowserSupport.getProgressPercentage(plugin, player, requirement);
        final String progressBar = this.buildProgressBar(player, progress);
        final List<Component> lore = new ArrayList<>(this.i18n("entry.lore", player)
            .withPlaceholders(Map.of(
                "requirement_key", requirement.key(),
                "requirement_type", this.formatTypeLabel(requirement.section().getType()),
                "status", status,
                "progress_bar", progressBar,
                "progress", progress + "%",
                "summary", requirement.summary()
            ))
            .build()
            .children());

        lore.add(Component.empty());
        lore.add(this.i18n("entry.details", player).build().component());
        for (final String detailLine : StorageStoreRequirementBrowserSupport.buildDetailLines(plugin, player, requirement)) {
            lore.add(Component.text(detailLine, NamedTextColor.GRAY));
        }

        return UnifiedBuilderFactory.item(StorageStoreRequirementBrowserSupport.resolveMaterial(requirement))
            .setName(this.i18n("entry.name", player)
                .withPlaceholder("requirement_name", StorageStoreRequirementBrowserSupport.resolveDisplayName(requirement))
                .build()
                .component())
            .setLore(lore)
            .setGlowing(met)
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEmptyItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.LIME_STAINED_GLASS_PANE)
            .setName(this.i18n("empty.name", player).build().component())
            .setLore(this.i18n("empty.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull String resolveStatusPlaceholder(
        final @NotNull Player player,
        final @NotNull StorageStorePricingSupport.ResolvedStoreRequirement requirement,
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

    private void handleRequirementClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull StorageStorePricingSupport.ResolvedStoreRequirement requirement
    ) {
        if (!clickContext.isRightClick()) {
            this.reopenRequirementBrowser(clickContext);
            return;
        }

        final RDR plugin = this.rdr.get(clickContext);
        final StorageStorePricingSupport.ProgressUpdateResult result =
            StorageStorePricingSupport.bankRequirementProgress(
                plugin,
                clickContext.getPlayer(),
                this.findPlayer(clickContext),
                requirement
            );

        this.sendProgressFeedback(clickContext, requirement, result);
        this.reopenRequirementBrowser(clickContext);
    }

    private void sendProgressFeedback(
        final @NotNull SlotClickContext clickContext,
        final @NotNull StorageStorePricingSupport.ResolvedStoreRequirement requirement,
        final @NotNull StorageStorePricingSupport.ProgressUpdateResult result
    ) {
        final String key = switch (result.status()) {
            case PROFILE_MISSING -> "feedback.profile_missing";
            case UNSUPPORTED -> "feedback.unsupported";
            case UNAVAILABLE -> "feedback.unavailable";
            case NO_PROGRESS -> "feedback.no_progress";
            case PROGRESSED -> "feedback.progress_saved";
            case COMPLETE -> "feedback.complete";
        };

        this.i18n(key, clickContext.getPlayer())
            .withPlaceholders(Map.of(
                "requirement", StorageStoreRequirementBrowserSupport.resolveDisplayName(requirement),
                "progress_bar", this.buildProgressBar(clickContext.getPlayer(), result.progressPercentage()),
                "progress", result.progressPercentage() + "%"
            ))
            .includePrefix()
            .build()
            .sendMessage();
    }

    private @Nullable RDRPlayer findPlayer(final @NotNull Context context) {
        final RDR plugin = this.rdr.get(context);
        return plugin.getPlayerRepository() == null
            ? null
            : plugin.getPlayerRepository().findByPlayer(context.getPlayer().getUniqueId());
    }

    private @NotNull String buildProgressBar(final @NotNull Player player, final int percentage) {
        return StorageStorePricingSupport.buildProgressBar(
            percentage,
            this.resolvePlaceholder("progress_bar.empty", player),
            this.resolvePlaceholder("progress_bar.partial", player),
            this.resolvePlaceholder("progress_bar.filled", player)
        );
    }

    private @NotNull String resolvePlaceholder(final @NotNull String key, final @NotNull Player player) {
        return this.i18n(key, player).build().getI18nVersionWrapper().asPlaceholder();
    }

    private void reopenRequirementBrowser(final @NotNull Context context) {
        context.openForPlayer(
            StorageStoreRequirementsView.class,
            Map.of(
                "plugin", this.rdr.get(context),
                "purchase_number", this.resolvePurchaseNumber(context)
            )
        );
    }

    private @NotNull String formatTypeLabel(final @NotNull String type) {
        final String normalized = type.replace('_', ' ').toLowerCase(Locale.ROOT);
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

        return builder.length() == 0 ? type : builder.toString();
    }
}
