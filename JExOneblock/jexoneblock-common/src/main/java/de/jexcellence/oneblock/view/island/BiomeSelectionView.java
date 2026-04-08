package de.jexcellence.oneblock.view.island;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.biome.EBiomeCategory;
import de.jexcellence.oneblock.database.entity.biome.EBiomeCategory.BiomeInfo;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BiomeSelectionView extends BaseView {

    private final State<JExOneblock> plugin = initialState("plugin");
    private final State<OneblockIsland> island = initialState("island");
    private final State<EBiomeCategory> selectedCategory = initialState("selectedCategory");

    private static final EBiomeCategory DEFAULT_CATEGORY = EBiomeCategory.TEMPERATE;

    @Override
    protected String getKey() {
        return "biome_selection_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "CCCCCCCCC",
            "B1234567B",
            "B8901234B",
            "B5678901B",
            "B<RSA_>XB"
        };
    }

    @Override
    protected int getSize() {
        return 5;
    }

    @Override
    protected Map<String, Object> getTitlePlaceholders(@NotNull OpenContext open) {
        var islandData = island.get(open);
        var category = selectedCategory.get(open);
        if (category == null) {
            category = DEFAULT_CATEGORY;
        }
        return Map.of(
            "category", category.getDisplayName(),
            "current_biome", getCurrentBiomeName(islandData),
            "available_count", getAvailableBiomeCount(islandData, category),
            "total_count", category.getBiomesForCategory().size()
        );
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        var islandData = island.get(render);

        if (!hasBiomePermission(player, islandData)) {
            i18n("biome.no_permission", player).includePrefix().build().sendMessage();
            player.closeInventory();
            return;
        }

        var category = selectedCategory.get(render);
        if (category == null) {
            category = DEFAULT_CATEGORY;
        }

        renderCategoryTabs(render, player, category);
        renderBiomes(render, player, islandData, category);
        renderNavigationButtons(render, player, islandData, category);
        renderBorder(render);
    }

    private void renderCategoryTabs(@NotNull RenderContext render, @NotNull Player player, @NotNull EBiomeCategory currentCategory) {
        var allCategories = EBiomeCategory.values();
        var slotIndex = 0;

        for (var category : allCategories) {
            if (slotIndex >= 9) break;

            var isSelected = category == currentCategory;
            final var finalCategory = category;

            Component nameComponent = isSelected
                ? i18n("biome.category.selected", player)
                    .withPlaceholder("category", category.getDisplayName())
                    .build().component()
                : i18n("biome.category.name", player)
                    .withPlaceholder("category", category.getDisplayName())
                    .build().component();

            render.slot(slotIndex, UnifiedBuilderFactory
                .item(category.getIcon())
                .setName(nameComponent)
                .setLore(i18n("biome.category.lore", player)
                    .withPlaceholder("description", category.getDescription())
                    .withPlaceholder("category", category.getDisplayName().toLowerCase())
                    .build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .setGlowing(isSelected)
                .build()
            ).onClick(ctx -> {
                if (finalCategory != currentCategory) {
                    ctx.openForPlayer(BiomeSelectionView.class, Map.of(
                        "plugin", plugin.get(ctx),
                        "island", island.get(ctx),
                        "selectedCategory", finalCategory
                    ));
                }
            });

            slotIndex++;
        }
    }

    private void renderBiomes(@NotNull RenderContext render, @NotNull Player player,
                              @NotNull OneblockIsland island, @NotNull EBiomeCategory category) {
        var biomes = category.getBiomesForCategory();
        var biomeSlots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

        for (int i = 0; i < Math.min(biomes.size(), biomeSlots.length); i++) {
            var biome = biomes.get(i);
            renderBiomeSlot(render, player, island, biome, biomeSlots[i]);
        }

        for (int i = biomes.size(); i < biomeSlots.length; i++) {
            render.slot(biomeSlots[i], UnifiedBuilderFactory
                .item(Material.BLACK_STAINED_GLASS_PANE)
                .setName(Component.text(""))
                .build()
            );
        }
    }

    private void renderBiomeSlot(@NotNull RenderContext render, @NotNull Player player,
                                 @NotNull OneblockIsland island, @NotNull BiomeInfo biome, int slot) {
        var isUnlocked = isBiomeUnlocked(island, biome);
        var isCurrent = isCurrentBiome(island, biome);
        var canAfford = canAffordBiome(island, biome);

        var material = isUnlocked ? biome.material() : Material.BARRIER;

        Component nameComponent;
        if (isCurrent) {
            nameComponent = i18n("biome.item.current", player)
                .withPlaceholder("biome", biome.displayName())
                .build().component();
        } else if (isUnlocked) {
            nameComponent = i18n("biome.item.unlocked", player)
                .withPlaceholder("biome", biome.displayName())
                .build().component();
        } else {
            nameComponent = i18n("biome.item.locked", player)
                .withPlaceholder("biome", biome.displayName())
                .build().component();
        }

        render.slot(slot, UnifiedBuilderFactory
            .item(material)
            .setName(nameComponent)
            .setLore(buildBiomeLore(player, island, biome, isUnlocked, isCurrent, canAfford))
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .setGlowing(isCurrent)
            .build()
        ).onClick(ctx -> {
            if (isCurrent) {
                showCurrentBiomeInfo(player, biome);
            } else if (isUnlocked) {
                confirmBiomeChange(render, player, island, biome);
            } else {
                showBiomeRequirements(player, biome);
            }
        });
    }

    private List<Component> buildBiomeLore(@NotNull Player player, @NotNull OneblockIsland island,
                                           @NotNull BiomeInfo biome, boolean isUnlocked,
                                           boolean isCurrent, boolean canAfford) {
        var lore = new ArrayList<Component>();

        lore.addAll(i18n("biome.item.lore.info", player)
            .withPlaceholder("description", biome.description())
            .withPlaceholder("level", biome.requiredLevel())
            .withPlaceholder("cost", formatNumber(biome.requiredCoins()))
            .build().children());

        lore.add(Component.text(""));

        if (isCurrent) {
            lore.addAll(i18n("biome.item.lore.current", player).build().children());
        } else if (isUnlocked) {
            lore.addAll(i18n("biome.item.lore.available", player).build().children());
        } else if (canAfford) {
            lore.addAll(i18n("biome.item.lore.locked_level", player)
                .withPlaceholder("level", biome.requiredLevel())
                .build().children());
        } else {
            lore.addAll(i18n("biome.item.lore.locked_coins", player)
                .withPlaceholder("needed", formatNumber(biome.requiredCoins() - island.getIslandCoins()))
                .build().children());
        }

        return lore;
    }

    private void renderNavigationButtons(@NotNull RenderContext render, @NotNull Player player,
                                         @NotNull OneblockIsland island, @NotNull EBiomeCategory currentCategory) {
        var categories = EBiomeCategory.values();
        var currentIndex = currentCategory.ordinal();

        // Previous Category (<) - slot 36
        render.slot(36, UnifiedBuilderFactory
            .item(currentIndex > 0 ? Material.ARROW : Material.GRAY_DYE)
            .setName(i18n("biome.nav.previous", player).build().component())
            .build()
        ).onClick(ctx -> {
            if (currentIndex > 0) {
                ctx.openForPlayer(BiomeSelectionView.class, Map.of(
                    "plugin", plugin.get(ctx),
                    "island", this.island.get(ctx),
                    "selectedCategory", categories[currentIndex - 1]
                ));
            }
        });

        // Random Biome (R) - slot 37
        render.slot(37, UnifiedBuilderFactory
            .item(Material.ENDER_PEARL)
            .setName(i18n("biome.nav.random.name", player).build().component())
            .setLore(i18n("biome.nav.random.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> selectRandomBiome(player, island));

        // Current Biome Info (S) - slot 38
        render.slot(38, UnifiedBuilderFactory
            .item(Material.COMPASS)
            .setName(i18n("biome.nav.current.name", player)
                .withPlaceholder("biome", getCurrentBiomeName(island))
                .build().component())
            .setLore(i18n("biome.nav.current.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> showCurrentBiomeDetails(player, island));

        // Back (A) - slot 39
        render.slot(39, UnifiedBuilderFactory
            .item(Material.BARRIER)
            .setName(i18n("common.back", player).build().component())
            .build()
        ).onClick(ctx -> player.closeInventory());

        // Next Category (>) - slot 43
        render.slot(43, UnifiedBuilderFactory
            .item(currentIndex < categories.length - 1 ? Material.ARROW : Material.GRAY_DYE)
            .setName(i18n("biome.nav.next", player).build().component())
            .build()
        ).onClick(ctx -> {
            if (currentIndex < categories.length - 1) {
                ctx.openForPlayer(BiomeSelectionView.class, Map.of(
                    "plugin", plugin.get(ctx),
                    "island", this.island.get(ctx),
                    "selectedCategory", categories[currentIndex + 1]
                ));
            }
        });
    }

    private void renderBorder(@NotNull RenderContext render) {
        int[] borderSlots = {9, 17, 18, 26, 27, 35, 40, 41, 42};
        for (int slot : borderSlots) {
            render.slot(slot, UnifiedBuilderFactory
                .item(Material.GRAY_STAINED_GLASS_PANE)
                .setName(Component.text(""))
                .build()
            );
        }
    }

    // Helper methods

    private boolean hasBiomePermission(@NotNull Player player, @NotNull OneblockIsland island) {
        return island.getOwnerUuid().equals(player.getUniqueId());
    }

    private boolean isBiomeUnlocked(@NotNull OneblockIsland island, @NotNull BiomeInfo biome) {
        return island.getLevel() >= biome.requiredLevel() && island.getIslandCoins() >= biome.requiredCoins();
    }

    private boolean isCurrentBiome(@NotNull OneblockIsland island, @NotNull BiomeInfo biome) {
        var world = org.bukkit.Bukkit.getWorld(island.getWorldName());
        if (world != null) {
            var centerLoc = new org.bukkit.Location(world, island.getCenterX(), island.getSpawnY(), island.getCenterZ());
            var currentBiome = world.getBiome(centerLoc);
            return currentBiome.getKey().getKey().equalsIgnoreCase(biome.biome().getKey().getKey());
        }
        return false;
    }

    private boolean canAffordBiome(@NotNull OneblockIsland island, @NotNull BiomeInfo biome) {
        return island.getIslandCoins() >= biome.requiredCoins();
    }

    private String getCurrentBiomeName(@NotNull OneblockIsland island) {
        var world = org.bukkit.Bukkit.getWorld(island.getWorldName());
        if (world != null) {
            var centerLoc = new org.bukkit.Location(world, island.getCenterX(), island.getSpawnY(), island.getCenterZ());
            var currentBiome = world.getBiome(centerLoc);
            return currentBiome.getKey().getKey().replace("_", " ").toLowerCase();
        }
        return "Unknown";
    }

    private int getAvailableBiomeCount(@NotNull OneblockIsland island, @NotNull EBiomeCategory category) {
        return (int) category.getBiomesForCategory().stream()
            .filter(biome -> isBiomeUnlocked(island, biome))
            .count();
    }

    private void confirmBiomeChange(@NotNull RenderContext render, @NotNull Player player,
                                    @NotNull OneblockIsland island, @NotNull BiomeInfo biome) {
        i18n("biome.confirm.header", player).includePrefix().build().sendMessage();
        i18n("biome.confirm.biome", player)
            .withPlaceholder("biome", biome.displayName())
            .build().sendMessage();
        i18n("biome.confirm.cost", player)
            .withPlaceholder("cost", formatNumber(biome.requiredCoins()))
            .build().sendMessage();
        i18n("biome.confirm.remaining", player)
            .withPlaceholder("remaining", formatNumber(island.getIslandCoins() - biome.requiredCoins()))
            .build().sendMessage();
        i18n("biome.confirm.warning", player).build().sendMessage();

        performBiomeChange(render, player, island, biome);
    }

    private void performBiomeChange(@NotNull RenderContext render, @NotNull Player player,
                                    @NotNull OneblockIsland island, @NotNull BiomeInfo biome) {
        var biomeService = plugin.get(render).getBiomeService();

        try {
            biomeService.changeBiome(island, biome.biome(), player);
            island.setIslandCoins(island.getIslandCoins() - biome.requiredCoins());
            i18n("biome.change.success", player)
                .withPlaceholder("biome", biome.displayName())
                .includePrefix()
                .build().sendMessage();
        } catch (Exception e) {
            i18n("biome.change.failed", player)
                .withPlaceholder("biome", biome.displayName())
                .withPlaceholder("error", e.getMessage())
                .includePrefix()
                .build().sendMessage();
        }
    }

    private void showBiomeRequirements(@NotNull Player player, @NotNull BiomeInfo biome) {
        i18n("biome.requirements.header", player)
            .withPlaceholder("biome", biome.displayName())
            .includePrefix()
            .build().sendMessage();
        i18n("biome.requirements.level", player)
            .withPlaceholder("level", biome.requiredLevel())
            .build().sendMessage();
        i18n("biome.requirements.cost", player)
            .withPlaceholder("cost", formatNumber(biome.requiredCoins()))
            .build().sendMessage();
        i18n("biome.requirements.description", player)
            .withPlaceholder("description", biome.description())
            .build().sendMessage();
    }

    private void showCurrentBiomeInfo(@NotNull Player player, @NotNull BiomeInfo biome) {
        i18n("biome.info.header", player).includePrefix().build().sendMessage();
        i18n("biome.info.biome", player)
            .withPlaceholder("biome", biome.displayName())
            .build().sendMessage();
        i18n("biome.info.description", player)
            .withPlaceholder("description", biome.description())
            .build().sendMessage();
        i18n("biome.info.effects", player)
            .withPlaceholder("effects", getBiomeEffects(biome.biome()))
            .build().sendMessage();
    }

    private void showCurrentBiomeDetails(@NotNull Player player, @NotNull OneblockIsland island) {
        var currentBiome = getCurrentBiomeName(island);
        i18n("biome.details.header", player).includePrefix().build().sendMessage();
        i18n("biome.details.biome", player)
            .withPlaceholder("biome", currentBiome)
            .build().sendMessage();
        i18n("biome.details.effects", player)
            .withPlaceholder("effects", getBiomeEffects(currentBiome))
            .build().sendMessage();
    }

    private void selectRandomBiome(@NotNull Player player, @NotNull OneblockIsland island) {
        var cost = getRandomBiomeCost();
        if (island.getIslandCoins() < cost) {
            i18n("biome.random.insufficient", player)
                .withPlaceholder("needed", formatNumber(cost))
                .withPlaceholder("have", formatNumber(island.getIslandCoins()))
                .includePrefix()
                .build().sendMessage();
            return;
        }

        var unlockedBiomes = EBiomeCategory.getAllBiomes().stream()
            .filter(biome -> isBiomeUnlocked(island, biome))
            .filter(biome -> !isCurrentBiome(island, biome))
            .toList();

        if (unlockedBiomes.isEmpty()) {
            i18n("biome.random.none_available", player).includePrefix().build().sendMessage();
            return;
        }

        var randomBiome = unlockedBiomes.get((int) (Math.random() * unlockedBiomes.size()));
        i18n("biome.random.selected", player)
            .withPlaceholder("biome", randomBiome.displayName())
            .includePrefix()
            .build().sendMessage();
    }

    private long getRandomBiomeCost() {
        return 5000;
    }

    private String getBiomeEffects(@NotNull Biome biome) {
        return getBiomeEffects(biome.getKey().getKey());
    }

    private String getBiomeEffects(@NotNull String biomeName) {
        String normalizedName = biomeName.toLowerCase().replace(" ", "_");
        return switch (normalizedName) {
            case "desert" -> "Increased sand drops, no rain";
            case "jungle" -> "Increased wood drops, dense vegetation";
            case "ocean" -> "Increased fish spawns, water everywhere";
            case "nether_wastes" -> "Fire resistance, nether mobs";
            case "the_end" -> "Void protection, end mobs";
            default -> "Standard biome effects";
        };
    }

    private String formatNumber(long number) {
        if (number >= 1_000_000_000) return String.format("%.1fB", number / 1_000_000_000.0);
        if (number >= 1_000_000) return String.format("%.1fM", number / 1_000_000.0);
        if (number >= 1_000) return String.format("%.1fK", number / 1_000.0);
        return String.valueOf(number);
    }
}
