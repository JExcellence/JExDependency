/*
package com.raindropcentral.rdq2.view.perks;

import com.raindropcentral.rdq2.RDQ;
import com.raindropcentral.rdq2.database.entity.perk.RPerk;
import com.raindropcentral.rdq2.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq2.manager.perk.PerkManager;
import com.raindropcentral.rdq2.perk.runtime.LoadedPerk;
import com.raindropcentral.rdq2.perk.runtime.PerkRegistry;
import com.raindropcentral.rdq2.perk.runtime.PerkStateService;
import com.raindropcentral.rdq2.type.EPerkCategory;
import com.raindropcentral.rdq2.type.EPerkState;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

*/
/**
 * Paginated view for displaying and managing player perks.
 * <p>
 * Provides a paginated list of perks with filtering by category,
 * showing state (owned, enabled, active, cooldown) and allowing
 * toggle/enable/disable actions.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 1.0.0
 *//*

public class PerkListViewFrame extends APaginatedView<PerkListViewFrame.PerkDisplayItem> {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkListViewFrame.class.getName());

    // States
    private final State<RDQ> rdq = initialState("plugin");
    private final State<RDQPlayer> player = initialState("player");
    private final MutableState<EPerkCategory> selectedCategory = mutableState(null);

    // Services
    private PerkRegistry perkRegistry;
    private PerkStateService perkStateService;

    public PerkListViewFrame() {
        super(PerkMainView.class);
    }

    @Override
    protected String getKey() {
        return "perk_list_ui";
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        final EPerkCategory category = this.selectedCategory.get(openContext);
        final String categoryName = category != null ? 
                this.i18n("category." + category.name().toLowerCase(), openContext.getPlayer()).build().toString() : 
                this.i18n("category.all", openContext.getPlayer()).build().toString();
        return Map.of("category_name", categoryName);
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
                "CCCCCCCCC",
                "OOOOOOOOO",
                "OOOOOOOOO",
                "OOOOOOOOO",
                "         ",
                "b  <p>   "
        };
    }

    @Override
    protected CompletableFuture<List<PerkDisplayItem>> getAsyncPaginationSource(final @NotNull Context context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final RDQ plugin = this.rdq.get(context);
                final RDQPlayer rdqPlayer = this.player.get(context);
                
                if (rdqPlayer == null) {
                    return List.of();
                }

                this.perkRegistry = plugin.getPerkRegistry();
                this.perkStateService = plugin.getPerkInitializationManager().getPerkStateService();

                final List<LoadedPerk> allPerks = this.perkRegistry.getAll();
                final EPerkCategory category = this.selectedCategory.get(context);

                return allPerks.stream()
                        .filter(perk -> category == null || perk.config().category() == category)
                        .map(perk -> this.createDisplayItem(context, perk, rdqPlayer, context.getPlayer()))
                        .filter(Objects::nonNull)
                        .sorted(this::compareDisplayItems)
                        .collect(Collectors.toList());

            } catch (final Exception e) {
                LOGGER.log(Level.SEVERE, "Error loading perk data", e);
                return List.of();
            }
        });
    }

    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull PerkDisplayItem item
    ) {
        final Player player = context.getPlayer();
        final Material material = item.category.getIconMaterial();

        builder
                .withItem(
                        UnifiedBuilderFactory
                                .item(material)
                                .setName(
                                        this.i18n("perk_entry.name", player)
                                                .with("perk_name", item.perk.getId())
                                                .with("state_color", item.state.getColorCode())
                                                .build()
                                                .component()
                                )
                                .setLore(
                                        this.i18n("perk_entry.lore", player)
                                                .withAll(
                                                        Map.of(
                                                                "category", item.category.getDisplayName(),
                                                                "state", item.state.getColoredDisplayName(),
                                                                "index", index + 1
                                                        )
                                                )
                                                .build()
                                                .splitLines()
                                )
                                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                                .setGlowing(item.state == EPerkState.ACTIVE)
                                .build()
                )
                .onClick(clickContext -> this.handlePerkClick(context, player, item));
    }

    @Override
    protected void onPaginatedRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        this.renderCategoryButtons(render, player);
    }

    private void renderCategoryButtons(@NotNull RenderContext render, @NotNull Player player) {
        final EPerkCategory selected = this.selectedCategory.get(render);

        // All categories button (slot 4)
        render.layoutSlot(
                'C',
                this.createCategoryButton(null, selected == null, player)
        ).onClick(click -> {
            this.selectedCategory.set(null, render);
            render.update();
        });

        // Individual category buttons
        int categoryIndex = 0;
        for (final EPerkCategory category : EPerkCategory.values()) {
            if (categoryIndex >= 9) break; // Limit to top row

            final boolean isSelected = category == selected;
            final char slotChar = 'C';
            
            render.layoutSlot(
                    slotChar,
                    this.createCategoryButton(category, isSelected, player)
            ).onClick(click -> {
                this.selectedCategory.set(category, render);
                render.update();
            });
            
            categoryIndex++;
        }
    }

    private void handlePerkClick(@NotNull Context context, @NotNull Player player, @NotNull PerkDisplayItem item) {
        final RDQPlayer rdqPlayer = this.player.get(context);
        if (rdqPlayer == null) return;

        // Open detail view for all perks
        final RDQ plugin = this.rdq.get(context);
        player.closeInventory();
        plugin.getViewFrame().open(
                PerkDetailView.class,
                player,
                Map.of(
                        "plugin", plugin,
                        "perkId", item.perk.getId(),
                        "player", rdqPlayer
                )
        );
    }

    private @NotNull ItemStack createCategoryButton(@Nullable EPerkCategory category, boolean selected, @NotNull Player player) {
        if (category == null) {
            return UnifiedBuilderFactory.item(Material.COMPASS)
                    .setName(this.i18n("category_button.all.name", player).build().component())
                    .setLore(
                            this.i18n("category_button.all.lore", player)
                                    .with("selected", selected ? "✓" : "")
                                    .build()
                                    .splitLines()
                    )
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .setGlowing(selected)
                    .build();
        }

        return UnifiedBuilderFactory.item(category.getIconMaterial())
                .setName(
                        this.i18n("category_button.name", player)
                                .with("category_name", category.getDisplayName())
                                .build()
                                .component()
                )
                .setLore(
                        this.i18n("category_button.lore", player)
                                .with("category_name", category.getDisplayName())
                                .with("selected", selected ? "✓" : "")
                                .build()
                                .splitLines()
                )
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .setGlowing(selected)
                .build();
    }

    private @Nullable PerkDisplayItem createDisplayItem(
            @NotNull Context context,
            @NotNull LoadedPerk loadedPerk,
            @NotNull RDQPlayer rdqPlayer,
            @NotNull Player player
    ) {
        final EPerkCategory category = loadedPerk.config().category();
        final EPerkState state = this.calculateState(context, loadedPerk, rdqPlayer, player);

        return new PerkDisplayItem(loadedPerk, category, state);
    }

    private @NotNull EPerkState calculateState(
            @NotNull Context context,
            @NotNull LoadedPerk loadedPerk,
            @NotNull RDQPlayer rdqPlayer,
            @NotNull Player player
    ) {
        final RDQ plugin = this.rdq.get(context);
        final PerkManager perkManager = plugin.getPerkInitializationManager().getPerkManager();

        if (!loadedPerk.config().enabled()) {
            return EPerkState.DISABLED;
        }

        // Check ownership
        final RPerk rPerk = plugin.getPerkRepository().findByAttributes(Map.of("identifier", loadedPerk.getId()));
        final boolean isOwned = rdqPlayer != null && rPerk != null &&
                perkManager.getPerkStateService().playerOwnsPerk(rdqPlayer, rPerk);

        if (!isOwned) {
            return EPerkState.LOCKED;
        }

        if (perkManager.isActive(player, loadedPerk.getId())) {
            return EPerkState.ACTIVE;
        }

        if (perkManager.getCooldownService().isOnCooldown(player, loadedPerk.getId())) {
            return EPerkState.COOLDOWN;
        }

        return EPerkState.AVAILABLE;
    }

    private int compareDisplayItems(@NotNull PerkDisplayItem a, @NotNull PerkDisplayItem b) {
        final int stateCompare = Integer.compare(a.state.ordinal(), b.state.ordinal());
        if (stateCompare != 0) return stateCompare;
        return a.perk.getId().compareTo(b.perk.getId());
    }

    */
/**
     * Display item wrapper for perks in the list.
     *//*

    protected static class PerkDisplayItem {
        final LoadedPerk perk;
        final EPerkCategory category;
        final EPerkState state;

        PerkDisplayItem(LoadedPerk perk, EPerkCategory category, EPerkState state) {
            this.perk = perk;
            this.category = category;
            this.state = state;
        }
    }
}

*/
