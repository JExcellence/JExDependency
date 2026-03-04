/*
 * StorageStoreView.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rdr.view;

import java.util.List;
import java.util.Map;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.configs.ConfigSection;
import com.raindropcentral.rdr.database.entity.RDRPlayer;
import com.raindropcentral.rdr.database.entity.RStorage;
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
import org.jetbrains.annotations.Nullable;

/**
 * Store view used to purchase additional persistent storages.
 *
 * <p>The purchase flow evaluates and consumes the configured RPlatform requirements for each storage
 * purchase.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageStoreView extends BaseView {

    private static final int STORAGE_SIZE = 54;

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates the storage purchase view.
     */
    public StorageStoreView() {
        super(StorageOverviewView.class);
    }

    /**
     * Returns the translation namespace used by this view.
     *
     * @return storage store translation key prefix
     */
    @Override
    protected String getKey() {
        return "storage_store_ui";
    }

    /**
     * Returns the inventory layout for the storage purchase view.
     *
     * @return compact three-row layout with summary, cost, and purchase controls
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "   c p   ",
            "         "
        };
    }

    /**
     * Disables automatic filler item placement for this compact store view.
     *
     * @return {@code false} so only explicit controls render
     */
    @Override
    protected boolean shouldAutoFill() {
        return false;
    }

    /**
     * Renders the summary, cost, and purchase controls for the current player.
     *
     * @param render render context for slot registration
     * @param player player viewing the store
     */
    @Override
    public void onFirstRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final RDR plugin = this.rdr.get(render);
        final ConfigSection config = plugin.getDefaultConfig();
        final RDRPlayer rdrPlayer = this.findPlayer(render);
        final int ownedStorages = rdrPlayer == null ? 0 : rdrPlayer.getStorages().size();
        final int maxStorages = config.getMaxStorages();
        final int purchaseNumber = StorageStoreSupport.getNextPurchaseNumber(
            ownedStorages,
            config.getStartingStorages()
        );
        final List<StorageStorePricingSupport.ResolvedStoreRequirement> requirements =
            StorageStorePricingSupport.getConfiguredStoreRequirements(plugin, config, player, purchaseNumber);
        final String costSummary = this.resolveCostSummary(player, requirements);
        final boolean limitReached = StorageStoreSupport.hasReachedStorageLimit(ownedStorages, maxStorages);
        final StorageStorePricingSupport.RequirementAvailability requirementAvailability =
            StorageStorePricingSupport.resolveAvailability(player, requirements, rdrPlayer);

        render.layoutSlot('s')
            .renderWith(() -> this.createSummaryItem(player, ownedStorages, maxStorages));

        render.layoutSlot('c')
            .withItem(this.createCostItem(player, requirements.size(), requirementAvailability))
            .onClick(clickContext -> this.openRequirementBrowser(clickContext, plugin, purchaseNumber));

        render.layoutSlot('p')
            .withItem(this.createPurchaseItem(
                player,
                costSummary,
                requirements.size(),
                ownedStorages,
                maxStorages,
                rdrPlayer != null,
                requirementAvailability,
                limitReached
            ))
            .onClick(clickContext -> this.handlePurchaseClick(clickContext, plugin));
    }

    /**
     * Cancels item interaction so GUI items cannot be moved.
     *
     * @param click slot click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void handlePurchaseClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RDR plugin
    ) {
        final ConfigSection config = plugin.getDefaultConfig();
        final Player player = clickContext.getPlayer();
        final RDRPlayer rdrPlayer = this.findPlayer(clickContext);
        final int ownedStorages = rdrPlayer == null ? 0 : rdrPlayer.getStorages().size();
        final int maxStorages = config.getMaxStorages();
        final int purchaseNumber = StorageStoreSupport.getNextPurchaseNumber(
            ownedStorages,
            config.getStartingStorages()
        );

        if (plugin.getPlayerRepository() == null || rdrPlayer == null) {
            this.i18n("feedback.profile_missing", player)
                .build()
                .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        if (StorageStoreSupport.hasReachedStorageLimit(ownedStorages, maxStorages)) {
            this.i18n("feedback.limit_reached", player)
                .withPlaceholders(Map.of(
                    "owned_storages", ownedStorages,
                    "max_storages", maxStorages
                ))
                .build()
                .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        final StorageStorePricingSupport.PurchaseResult purchaseResult = StorageStorePricingSupport.purchaseStorage(
            clickContext,
            plugin,
            config,
            purchaseNumber,
            rdrPlayer
        );
        if (!purchaseResult.success()) {
            this.i18n(purchaseResult.failureKey(), player)
                .withPlaceholders(Map.of(
                    "requirement", purchaseResult.failedRequirement(),
                    "requirements", purchaseResult.requirementSummary(),
                    "owned_storages", ownedStorages,
                    "max_storages", maxStorages
                ))
                .build()
                .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        final String storageKey = StorageStoreSupport.buildNextStorageKey(rdrPlayer);
        new RStorage(rdrPlayer, storageKey, STORAGE_SIZE);
        plugin.getPlayerRepository().update(rdrPlayer);
        final String requirementSummary = purchaseResult.requirementSummary().isBlank()
            ? this.i18n("cost.none", player).build().getI18nVersionWrapper().asPlaceholder()
            : purchaseResult.requirementSummary();

        this.i18n("feedback.purchased", player)
            .withPlaceholders(Map.of(
                "storage_key", storageKey,
                "requirements", requirementSummary,
                "owned_storages", rdrPlayer.getStorages().size(),
                "max_storages", maxStorages
            ))
            .build()
            .sendMessage();
        this.openFreshView(clickContext);
    }

    private void openFreshView(final @NotNull Context context) {
        context.openForPlayer(
            StorageStoreView.class,
            Map.of("plugin", this.rdr.get(context))
        );
    }

    private void openRequirementBrowser(
        final @NotNull Context context,
        final @NotNull RDR plugin,
        final int purchaseNumber
    ) {
        context.openForPlayer(
            StorageStoreRequirementsView.class,
            Map.of(
                "plugin", plugin,
                "purchase_number", purchaseNumber
            )
        );
    }

    private @Nullable RDRPlayer findPlayer(final @NotNull Context context) {
        final RDR plugin = this.rdr.get(context);
        return plugin.getPlayerRepository() == null
            ? null
            : plugin.getPlayerRepository().findByPlayer(context.getPlayer().getUniqueId());
    }

    private @NotNull String resolveCostSummary(
        final @NotNull Player player,
        final @NotNull List<StorageStorePricingSupport.ResolvedStoreRequirement> requirements
    ) {
        if (requirements.isEmpty()) {
            return this.i18n("cost.none", player)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
        }

        return StorageStorePricingSupport.formatRequirementSummary(requirements);
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final int ownedStorages,
        final int maxStorages
    ) {
        return UnifiedBuilderFactory.item(Material.BARREL)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "owned_storages", ownedStorages,
                    "max_storages", maxStorages
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createCostItem(
        final @NotNull Player player,
        final int requirementCount,
        final @NotNull StorageStorePricingSupport.RequirementAvailability availability
    ) {
        final String availabilityPlaceholder = switch (availability) {
            case READY -> this.i18n("cost.availability.available", player)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
            case PENDING -> this.i18n("cost.availability.pending", player)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
            case UNAVAILABLE -> this.i18n("cost.availability.unavailable", player)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
        };

        final Material material = switch (availability) {
            case READY -> Material.GOLD_INGOT;
            case PENDING -> Material.CLOCK;
            case UNAVAILABLE -> Material.REDSTONE;
        };

        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n("cost.name", player).build().component())
            .setLore(this.i18n("cost.lore", player)
                .withPlaceholders(Map.of(
                    "requirement_count", requirementCount,
                    "requirements", requirementCount,
                    "availability", availabilityPlaceholder
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createPurchaseItem(
        final @NotNull Player player,
        final @NotNull String costSummary,
        final int requirementCount,
        final int ownedStorages,
        final int maxStorages,
        final boolean profileLoaded,
        final @NotNull StorageStorePricingSupport.RequirementAvailability requirementAvailability,
        final boolean limitReached
    ) {
        final Material material;
        final String key;
        if (!profileLoaded) {
            material = Material.BARRIER;
            key = "purchase.profile_missing";
        } else if (limitReached) {
            material = Material.BARRIER;
            key = "purchase.limit_reached";
        } else if (requirementAvailability == StorageStorePricingSupport.RequirementAvailability.UNAVAILABLE) {
            material = Material.REDSTONE_BLOCK;
            key = "purchase.no_requirements";
        } else if (requirementAvailability == StorageStorePricingSupport.RequirementAvailability.PENDING) {
            material = Material.CLOCK;
            key = "purchase.pending";
        } else {
            material = Material.EMERALD;
            key = "purchase.available";
        }

        return UnifiedBuilderFactory.item(material)
            .setName(this.i18n(key + ".name", player).build().component())
            .setLore(this.i18n(key + ".lore", player)
                .withPlaceholders(Map.of(
                    "requirements", costSummary,
                    "requirement_count", requirementCount,
                    "owned_storages", ownedStorages,
                    "max_storages", maxStorages
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}