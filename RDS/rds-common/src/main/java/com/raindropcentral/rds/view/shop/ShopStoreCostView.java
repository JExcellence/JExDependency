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

package com.raindropcentral.rds.view.shop;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.configs.ConfigSection;
import com.raindropcentral.rds.database.entity.RDSPlayer;
import com.raindropcentral.rds.items.ShopBlock;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated browser and purchase screen for the next RDS shop-store requirement tier.
 *
 * <p>Players can inspect each requirement, bank partial item or currency progress with right-click,
 * and purchase a new shop block from the summary item when the current tier is ready.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopStoreCostView extends APaginatedView<ShopStorePricingSupport.ResolvedStoreRequirement> {

    private final State<RDS> rds = initialState("plugin");

    /**
     * Creates the paginated shop-store requirement browser.
     */
    public ShopStoreCostView() {
        super(ShopStoreView.class);
    }

    @Override
    protected String getKey() {
        return "shop_store_cost_ui";
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        final RDS plugin = this.rds.get(openContext);
        final int ownedShops = this.getOrCreatePlayer(openContext).getShops();
        final int purchaseNumber = ShopStoreSupport.getNextPurchaseNumber(ownedShops);
        final ConfigSection config = plugin.getDefaultConfig();
        final int maxShopsValue = plugin.getMaximumShops(openContext.getPlayer(), config);
        final String maxShops = maxShopsValue > 0 ? Integer.toString(maxShopsValue) : "No limit";
        return Map.of(
            "purchase_number", purchaseNumber,
            "owned_shops", ownedShops,
            "max_shops", maxShops
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
    protected CompletableFuture<List<ShopStorePricingSupport.ResolvedStoreRequirement>> getAsyncPaginationSource(
        final @NotNull Context context
    ) {
        final RDS plugin = this.rds.get(context);
        final ConfigSection config = plugin.getDefaultConfig();
        final int ownedShops = this.getOrCreatePlayer(context).getShops();
        return CompletableFuture.completedFuture(
            ShopStorePricingSupport.getConfiguredStoreRequirements(
                plugin,
                config,
                context.getPlayer(),
                ShopStoreSupport.getNextPurchaseNumber(ownedShops)
            )
        );
    }

    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull ShopStorePricingSupport.ResolvedStoreRequirement entry
    ) {
        builder.withItem(this.createRequirementItem(this.rds.get(context), context.getPlayer(), entry))
            .updateOnClick()
            .onClick(clickContext -> this.handleRequirementClick(clickContext, entry));
    }

    @Override
    protected void onPaginatedRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final RDS plugin = this.rds.get(render);
        final ConfigSection config = plugin.getDefaultConfig();
        final RDSPlayer playerData = this.getOrCreatePlayer(render);
        final int ownedShops = playerData.getShops();
        final int purchaseNumber = ShopStoreSupport.getNextPurchaseNumber(ownedShops);
        final List<ShopStorePricingSupport.ResolvedStoreRequirement> requirements =
            ShopStorePricingSupport.getConfiguredStoreRequirements(
                plugin,
                config,
                player,
                purchaseNumber
            );

        render.layoutSlot('s')
            .renderWith(() -> this.createSummaryItem(player, plugin, config, playerData, requirements))
            .onClick(clickContext -> this.handlePurchaseClick(clickContext, playerData, config, requirements));

        if (requirements.isEmpty()) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
    }

    /**
     * Cancels direct inventory clicks while this view is open.
     *
     * @param click slot click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void handlePurchaseClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RDSPlayer playerData,
        final @NotNull ConfigSection config,
        final @NotNull List<ShopStorePricingSupport.ResolvedStoreRequirement> requirements
    ) {
        final Player player = clickContext.getPlayer();
        final RDS plugin = this.rds.get(clickContext);
        final int ownedShops = playerData.getShops();
        final int maxShops = plugin.getMaximumShops(player, config);
        final String maxShopsDisplay = maxShops > 0 ? Integer.toString(maxShops) : "No limit";
        final int purchaseNumber = ShopStoreSupport.getNextPurchaseNumber(ownedShops);

        if (ShopStoreSupport.hasReachedShopLimit(ownedShops, maxShops)) {
            this.i18n("feedback.limit_reached", player)
                .withPlaceholders(Map.of(
                    "owned_shops", ownedShops,
                    "max_shops", maxShopsDisplay
                ))
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final ShopStorePricingSupport.PurchaseResult purchaseResult = ShopStorePricingSupport.purchaseShop(
            clickContext,
            plugin,
            config,
            purchaseNumber,
            playerData
        );

        if (!purchaseResult.success()) {
            this.i18n(purchaseResult.failureKey(), player)
                .withPlaceholders(Map.of(
                    "requirement", purchaseResult.failedRequirement(),
                    "requirements", purchaseResult.requirementSummary(),
                    "owned_shops", ownedShops,
                    "max_shops", maxShopsDisplay
                ))
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        player.getInventory().addItem(ShopBlock.getShopBlock(plugin, player))
            .forEach((slot, item) -> player.getWorld().dropItem(
                player.getLocation().clone().add(0, 0.5, 0),
                item
            ));
        playerData.addShop(1);
        plugin.getPlayerRepository().update(playerData);
        final int updatedOwnedShops = playerData.getShops();

        final String requirementSummary = purchaseResult.requirementSummary().isBlank()
            ? this.i18n("summary.none", player).build().getI18nVersionWrapper().asPlaceholder()
            : purchaseResult.requirementSummary();

        this.i18n("feedback.purchased", player)
            .withPlaceholders(Map.of(
                "requirements", requirementSummary,
                "owned_shops", updatedOwnedShops,
                "max_shops", maxShopsDisplay,
                "requirement_count", requirements.size()
            ))
            .includePrefix()
            .build()
            .sendMessage();

        this.reopenCostView(clickContext);
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @NotNull RDS plugin,
        final @NotNull ConfigSection config,
        final @NotNull RDSPlayer playerData,
        final @NotNull List<ShopStorePricingSupport.ResolvedStoreRequirement> requirements
    ) {
        final int ownedShops = playerData.getShops();
        final int purchaseNumber = ShopStoreSupport.getNextPurchaseNumber(ownedShops);
        final long metCount = requirements.stream()
            .filter(requirement -> ShopStoreRequirementBrowserSupport.isRequirementMet(plugin, player, requirement))
            .count();
        final ShopStorePricingSupport.RequirementAvailability availability =
            ShopStorePricingSupport.resolveAvailability(player, requirements, playerData);
        final String availabilityPlaceholder = this.resolveAvailabilityPlaceholder(player, availability);
        final int maxShopsValue = plugin.getMaximumShops(player, config);
        final String maxShops = maxShopsValue > 0 ? Integer.toString(maxShopsValue) : "No limit";
        final Material material = switch (availability) {
            case READY -> Material.EMERALD;
            case PENDING -> Material.CLOCK;
            case UNAVAILABLE -> Material.BARRIER;
        };

        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "purchase_number", purchaseNumber,
                    "owned_shops", ownedShops,
                    "max_shops", maxShops,
                    "requirement_count", requirements.size(),
                    "met_count", metCount,
                    "availability", availabilityPlaceholder
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createRequirementItem(
        final @NotNull RDS plugin,
        final @NotNull Player player,
        final @NotNull ShopStorePricingSupport.ResolvedStoreRequirement requirement
    ) {
        final boolean met = ShopStoreRequirementBrowserSupport.isRequirementMet(plugin, player, requirement);
        final int progress = ShopStoreRequirementBrowserSupport.getProgressPercentage(plugin, player, requirement);
        final String progressBar = this.buildProgressBar(player, progress);
        final String status = this.resolveStatusPlaceholder(player, requirement, met);
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
        for (final String detailLine : ShopStoreRequirementBrowserSupport.buildDetailLines(plugin, player, requirement)) {
            lore.add(Component.text(detailLine, NamedTextColor.GRAY));
        }

        return UnifiedBuilderFactory.item(ShopStoreRequirementBrowserSupport.resolveMaterial(requirement))
            .setName(this.i18n("entry.name", player)
                .withPlaceholder("requirement_name", ShopStoreRequirementBrowserSupport.resolveDisplayName(requirement))
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

    private void handleRequirementClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull ShopStorePricingSupport.ResolvedStoreRequirement requirement
    ) {
        if (!clickContext.isRightClick()) {
            this.reopenCostView(clickContext);
            return;
        }

        final RDS plugin = this.rds.get(clickContext);
        final ShopStorePricingSupport.ProgressUpdateResult result =
            ShopStorePricingSupport.bankRequirementProgress(
                plugin,
                clickContext.getPlayer(),
                this.findPlayer(clickContext),
                requirement
            );

        this.sendProgressFeedback(clickContext, requirement, result);
        this.reopenCostView(clickContext);
    }

    private void sendProgressFeedback(
        final @NotNull SlotClickContext clickContext,
        final @NotNull ShopStorePricingSupport.ResolvedStoreRequirement requirement,
        final @NotNull ShopStorePricingSupport.ProgressUpdateResult result
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
                "requirement", ShopStoreRequirementBrowserSupport.resolveDisplayName(requirement),
                "progress_bar", this.buildProgressBar(clickContext.getPlayer(), result.progressPercentage()),
                "progress", result.progressPercentage() + "%"
            ))
            .includePrefix()
            .build()
            .sendMessage();
    }

    private @Nullable RDSPlayer findPlayer(final @NotNull Context context) {
        final RDS plugin = this.rds.get(context);
        return plugin.getPlayerRepository() == null
            ? null
            : plugin.getPlayerRepository().findByPlayer(context.getPlayer().getUniqueId());
    }

    private @NotNull String buildProgressBar(final @NotNull Player player, final int percentage) {
        return ShopStorePricingSupport.buildProgressBar(
            percentage,
            this.resolvePlaceholder("progress_bar.empty", player),
            this.resolvePlaceholder("progress_bar.partial", player),
            this.resolvePlaceholder("progress_bar.filled", player)
        );
    }

    private @NotNull String resolvePlaceholder(final @NotNull String key, final @NotNull Player player) {
        return this.i18n(key, player).build().getI18nVersionWrapper().asPlaceholder();
    }

    private @NotNull RDSPlayer getOrCreatePlayer(final @NotNull Context context) {
        final Player player = context.getPlayer();
        final RDS plugin = this.rds.get(context);
        final RDSPlayer existingPlayer = plugin.getPlayerRepository().findByPlayer(player.getUniqueId());
        if (existingPlayer != null) {
            return existingPlayer;
        }

        final RDSPlayer newPlayer = new RDSPlayer(player.getUniqueId());
        plugin.getPlayerRepository().create(newPlayer);
        return newPlayer;
    }

    private void reopenCostView(final @NotNull Context context) {
        context.openForPlayer(
            ShopStoreCostView.class,
            Map.of("plugin", this.rds.get(context))
        );
    }

    private @NotNull String resolveAvailabilityPlaceholder(
        final @NotNull Player player,
        final @NotNull ShopStorePricingSupport.RequirementAvailability availability
    ) {
        final String key = switch (availability) {
            case READY -> "summary.availability.ready";
            case PENDING -> "summary.availability.pending";
            case UNAVAILABLE -> "summary.availability.unavailable";
        };
        return this.i18n(key, player).build().getI18nVersionWrapper().asPlaceholder();
    }

    private @NotNull String resolveStatusPlaceholder(
        final @NotNull Player player,
        final @NotNull ShopStorePricingSupport.ResolvedStoreRequirement requirement,
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
