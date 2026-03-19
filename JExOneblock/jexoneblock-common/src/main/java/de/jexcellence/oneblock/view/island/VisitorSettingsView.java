package de.jexcellence.oneblock.view.island;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockVisitorSettings;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIslandMember;
import de.jexcellence.oneblock.database.entity.oneblock.EOneblockIslandRole;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class VisitorSettingsView extends BaseView {
    
    private final State<JExOneblock> plugin = initialState("plugin");
    private final State<OneblockIsland> island = initialState("island");
    
    private static final List<VisitorPermission> VISITOR_PERMISSIONS = Arrays.asList(
        new VisitorPermission("BREAK_BLOCKS", Material.DIAMOND_PICKAXE, "Break Blocks", "Allow visitors to break blocks"),
        new VisitorPermission("PLACE_BLOCKS", Material.COBBLESTONE, "Place Blocks", "Allow visitors to place blocks"),
        new VisitorPermission("INTERACT_BLOCKS", Material.LEVER, "Interact Blocks", "Allow visitors to interact with blocks"),
        new VisitorPermission("INTERACT_ENTITIES", Material.WHEAT, "Interact Entities", "Allow visitors to interact with entities"),
        new VisitorPermission("OPEN_CONTAINERS", Material.CHEST, "Open Containers", "Allow visitors to open chests and containers"),
        new VisitorPermission("USE_DOORS", Material.OAK_DOOR, "Use Doors", "Allow visitors to open and close doors"),
        new VisitorPermission("USE_BUTTONS", Material.STONE_BUTTON, "Use Buttons", "Allow visitors to press buttons"),
        new VisitorPermission("USE_PRESSURE_PLATES", Material.STONE_PRESSURE_PLATE, "Use Pressure Plates", "Allow visitors to step on pressure plates"),
        new VisitorPermission("PICKUP_ITEMS", Material.APPLE, "Pickup Items", "Allow visitors to pickup dropped items"),
        new VisitorPermission("DROP_ITEMS", Material.DROPPER, "Drop Items", "Allow visitors to drop items"),
        new VisitorPermission("DAMAGE_ENTITIES", Material.IRON_SWORD, "Damage Entities", "Allow visitors to damage entities"),
        new VisitorPermission("BREED_ANIMALS", Material.CARROT, "Breed Animals", "Allow visitors to breed animals"),
        new VisitorPermission("SHEAR_SHEEP", Material.SHEARS, "Shear Sheep", "Allow visitors to shear sheep"),
        new VisitorPermission("MILK_COWS", Material.MILK_BUCKET, "Milk Cows", "Allow visitors to milk cows"),
        new VisitorPermission("FISH", Material.FISHING_ROD, "Fish", "Allow visitors to fish in water"),
        new VisitorPermission("USE_CRAFTING", Material.CRAFTING_TABLE, "Use Crafting", "Allow visitors to use crafting tables"),
        new VisitorPermission("USE_FURNACES", Material.FURNACE, "Use Furnaces", "Allow visitors to use furnaces"),
        new VisitorPermission("USE_ENCHANTING", Material.ENCHANTING_TABLE, "Use Enchanting", "Allow visitors to use enchanting tables"),
        new VisitorPermission("USE_BREWING", Material.BREWING_STAND, "Use Brewing", "Allow visitors to use brewing stands"),
        new VisitorPermission("USE_ANVILS", Material.ANVIL, "Use Anvils", "Allow visitors to use anvils"),
        new VisitorPermission("TELEPORT_HERE", Material.ENDER_PEARL, "Teleport Here", "Allow visitors to teleport to this island"),
        new VisitorPermission("VIEW_ISLAND", Material.SPYGLASS, "View Island", "Allow visitors to view island information")
    );
    
    @Override
    protected String getKey() {
        return "visitor_settings_ui";
    }
    
    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "X1234567X",
            "X8901234X",
            "X5678901X",
            "X<PADB>X"
        };
    }
    
    @Override
    protected int getSize() {
        return 5;
    }
    
    @Override
    protected Map<String, Object> getTitlePlaceholders(@NotNull OpenContext open) {
        var islandData = island.get(open);
        return Map.of(
            "island_name", islandData.getIslandName() != null ? islandData.getIslandName() : "Island",
            "enabled_count", getEnabledPermissionsCount(islandData),
            "total_count", VISITOR_PERMISSIONS.size()
        );
    }
    
    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        var islandData = island.get(render);
        
        if (!hasModeratorPermission(player, islandData)) {
            i18n("visitor.settings.no_permission", player).includePrefix().build().sendMessage();
            player.closeInventory();
            return;
        }
        
        renderPermissionSlots(render, player, islandData);
        renderPresetButtons(render, player, islandData);
        renderNavigationButtons(render, player);
        renderBorder(render);
    }
    
    private void renderPermissionSlots(@NotNull RenderContext render, @NotNull Player player, @NotNull OneblockIsland island) {
        char[] slots = {'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2'};
        
        for (int i = 0; i < Math.min(VISITOR_PERMISSIONS.size(), slots.length); i++) {
            var permission = VISITOR_PERMISSIONS.get(i);
            var isEnabled = isPermissionEnabled(island, permission.key());
            
            renderPermissionSlot(render, player, island, permission, slots[i], isEnabled);
        }
    }
    
    private void renderPermissionSlot(@NotNull RenderContext render, @NotNull Player player,
                                    @NotNull OneblockIsland island, @NotNull VisitorPermission permission,
                                    char slot, boolean isEnabled) {
        var material = isEnabled ? permission.material() : Material.BARRIER;
        var nameColor = isEnabled ? "§a" : "§c";
        var status = isEnabled ? "Enabled" : "Disabled";
        
        render.layoutSlot(slot, UnifiedBuilderFactory
            .item(material)
            .setName(net.kyori.adventure.text.Component.text(nameColor + permission.displayName() + " §7(" + status + ")"))
            .setLore(java.util.List.of(
                net.kyori.adventure.text.Component.text("§7" + permission.description()),
                net.kyori.adventure.text.Component.text("§7Status: " + status),
                net.kyori.adventure.text.Component.text("§7Click to toggle")
            ))
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            var newState = !isEnabled;
            setPermissionEnabled(island, permission.key(), newState);
            
            saveVisitorSettings(island);
            
            var statusKey = newState ? "visitor.settings.enabled" : "visitor.settings.disabled";
            i18n(statusKey, player)
                .withPlaceholder("permission", permission.displayName())
                .includePrefix()
                .build().sendMessage();
            
            ctx.update();
        });
    }
    
    private List<String> buildPermissionLore(@NotNull Player player, @NotNull VisitorPermission permission, boolean isEnabled) {
        var statusColor = isEnabled ? "§a" : "§c";
        var statusText = isEnabled ? "Enabled" : "Disabled";
        
        return i18n("visitor.settings.permission.lore", player)
            .withPlaceholder("description", permission.description())
            .withPlaceholder("status", statusColor + statusText)
            .withPlaceholder("action", isEnabled ? "Click to disable" : "Click to enable")
            .build().children();
    }
    
    private void renderPresetButtons(@NotNull RenderContext render, @NotNull Player player, @NotNull OneblockIsland island) {
        // Preset: Allow All (P)
        render.layoutSlot('P', UnifiedBuilderFactory
            .item(Material.LIME_CONCRETE)
            .setName(i18n("visitor.settings.preset.allow_all.name", player).build().component())
            .setLore(i18n("visitor.settings.preset.allow_all.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            applyPreset(island, "ALLOW_ALL");
            saveVisitorSettings(island);
            i18n("visitor.settings.preset.applied", player)
                .withPlaceholder("preset", "Allow All")
                .includePrefix()
                .build().sendMessage();
            ctx.update();
        });
        
        // Preset: Deny All (A)
        render.layoutSlot('A', UnifiedBuilderFactory
            .item(Material.RED_CONCRETE)
            .setName(i18n("visitor.settings.preset.deny_all.name", player).build().component())
            .setLore(i18n("visitor.settings.preset.deny_all.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            applyPreset(island, "DENY_ALL");
            saveVisitorSettings(island);
            i18n("visitor.settings.preset.applied", player)
                .withPlaceholder("preset", "Deny All")
                .includePrefix()
                .build().sendMessage();
            ctx.update();
        });
        
        // Preset: Basic (D)
        render.layoutSlot('D', UnifiedBuilderFactory
            .item(Material.YELLOW_CONCRETE)
            .setName(i18n("visitor.settings.preset.basic.name", player).build().component())
            .setLore(i18n("visitor.settings.preset.basic.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            applyPreset(island, "BASIC");
            saveVisitorSettings(island);
            i18n("visitor.settings.preset.applied", player)
                .withPlaceholder("preset", "Basic")
                .includePrefix()
                .build().sendMessage();
            ctx.update();
        });
    }
    
    public void renderNavigationButtons(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('<', UnifiedBuilderFactory
            .item(Material.GRAY_DYE)
            .setName(i18n("common.previous", player).build().component())
            .build()
        );
        
        render.layoutSlot('B', UnifiedBuilderFactory
            .item(Material.BARRIER)
            .setName(i18n("common.back", player).build().component())
            .build()
        ).onClick(ctx -> ctx.openForPlayer(IslandMainView.class, ctx.getInitialData()));
    }
    
    private void renderBorder(@NotNull RenderContext render) {
        render.layoutSlot('X', UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(net.kyori.adventure.text.Component.text(""))
            .build()
        );
    }
    
    // Permission management methods
    
    private boolean hasModeratorPermission(@NotNull Player player, @NotNull OneblockIsland island) {
        if (island.getOwnerUuid().equals(player.getUniqueId())) {
            return true;
        }
        
        return false;
    }
    
    private boolean isPermissionEnabled(@NotNull OneblockIsland island, @NotNull String permissionKey) {
        var visitorSettings = island.getVisitorSettings();
        if (visitorSettings == null) {
            return getDefaultPermissionState(permissionKey);
        }
        
        return switch (permissionKey) {
            case "can_visit" -> visitorSettings.isCanVisit();
            case "can_interact_with_blocks" -> visitorSettings.isCanInteractWithBlocks();
            case "can_interact_with_entities" -> visitorSettings.isCanInteractWithEntities();
            case "can_use_items" -> visitorSettings.isCanUseItems();
            case "can_place_blocks" -> visitorSettings.isCanPlaceBlocks();
            case "can_break_blocks" -> visitorSettings.isCanBreakBlocks();
            case "can_open_chests" -> visitorSettings.isCanOpenChests();
            case "can_use_furnaces" -> visitorSettings.isCanUseFurnaces();
            case "can_use_crafting_tables" -> visitorSettings.isCanUseCraftingTables();
            case "can_use_redstone" -> visitorSettings.isCanUseRedstone();
            case "can_use_buttons_and_levers" -> visitorSettings.isCanUseButtonsAndLevers();
            case "can_hurt_animals" -> visitorSettings.isCanHurtAnimals();
            case "can_breed_animals" -> visitorSettings.isCanBreedAnimals();
            case "can_tame_animals" -> visitorSettings.isCanTameAnimals();
            case "can_harvest_crops" -> visitorSettings.isCanHarvestCrops();
            case "can_plant_crops" -> visitorSettings.isCanPlantCrops();
            case "can_use_bone_meal" -> visitorSettings.isCanUseBoneMeal();
            case "can_use_anvils" -> visitorSettings.isCanUseAnvils();
            case "can_use_enchanting_tables" -> visitorSettings.isCanUseEnchantingTables();
            case "can_use_brewing_stands" -> visitorSettings.isCanUseBrewingStands();
            case "can_pickup_items" -> visitorSettings.isCanPickupItems();
            case "can_drop_items" -> visitorSettings.isCanDropItems();
            default -> false;
        };
    }
    
    private void setPermissionEnabled(@NotNull OneblockIsland island, @NotNull String permissionKey, boolean enabled) {
        var visitorSettings = island.getVisitorSettings();
        if (visitorSettings == null) {
            visitorSettings = new OneblockVisitorSettings(island);
            island.setVisitorSettings(visitorSettings);
        }
        
        switch (permissionKey) {
            case "can_visit" -> visitorSettings.setCanVisit(enabled);
            case "can_interact_with_blocks" -> visitorSettings.setCanInteractWithBlocks(enabled);
            case "can_interact_with_entities" -> visitorSettings.setCanInteractWithEntities(enabled);
            case "can_use_items" -> visitorSettings.setCanUseItems(enabled);
            case "can_place_blocks" -> visitorSettings.setCanPlaceBlocks(enabled);
            case "can_break_blocks" -> visitorSettings.setCanBreakBlocks(enabled);
            case "can_open_chests" -> visitorSettings.setCanOpenChests(enabled);
            case "can_use_furnaces" -> visitorSettings.setCanUseFurnaces(enabled);
            case "can_use_crafting_tables" -> visitorSettings.setCanUseCraftingTables(enabled);
            case "can_use_redstone" -> visitorSettings.setCanUseRedstone(enabled);
            case "can_use_buttons_and_levers" -> visitorSettings.setCanUseButtonsAndLevers(enabled);
            case "can_hurt_animals" -> visitorSettings.setCanHurtAnimals(enabled);
            case "can_breed_animals" -> visitorSettings.setCanBreedAnimals(enabled);
            case "can_tame_animals" -> visitorSettings.setCanTameAnimals(enabled);
            case "can_harvest_crops" -> visitorSettings.setCanHarvestCrops(enabled);
            case "can_plant_crops" -> visitorSettings.setCanPlantCrops(enabled);
            case "can_use_bone_meal" -> visitorSettings.setCanUseBoneMeal(enabled);
            case "can_use_anvils" -> visitorSettings.setCanUseAnvils(enabled);
            case "can_use_enchanting_tables" -> visitorSettings.setCanUseEnchantingTables(enabled);
            case "can_use_brewing_stands" -> visitorSettings.setCanUseBrewingStands(enabled);
            case "can_pickup_items" -> visitorSettings.setCanPickupItems(enabled);
            case "can_drop_items" -> visitorSettings.setCanDropItems(enabled);
        }
    }
    
    private boolean getDefaultPermissionState(@NotNull String permissionKey) {
        return switch (permissionKey) {
            case "VIEW_ISLAND", "TELEPORT_HERE", "USE_DOORS", "USE_PRESSURE_PLATES", "PICKUP_ITEMS" -> true;
            default -> false;
        };
    }
    
    private void applyPreset(@NotNull OneblockIsland island, @NotNull String presetType) {
        var visitorSettings = island.getVisitorSettings();
        if (visitorSettings == null) {
            visitorSettings = new OneblockVisitorSettings(island);
            island.setVisitorSettings(visitorSettings);
        }
        
        // Apply preset based on type
        switch (presetType) {
            case "ALLOW_ALL" -> visitorSettings.setAllPermissions(true);
            case "DENY_ALL" -> visitorSettings.setAllPermissions(false);
            case "BASIC" -> visitorSettings.setBasicPermissions();
            case "TRUSTED" -> visitorSettings.setTrustedPermissions();
        }
    }
    
    private boolean isBasicPermission(@NotNull String permissionKey) {
        // Basic preset allows safe interactions
        return switch (permissionKey) {
            case "VIEW_ISLAND", "TELEPORT_HERE", "USE_DOORS", "USE_BUTTONS", 
                 "USE_PRESSURE_PLATES", "PICKUP_ITEMS", "INTERACT_BLOCKS", 
                 "USE_CRAFTING" -> true;
            default -> false;
        };
    }
    
    private int getEnabledPermissionsCount(@NotNull OneblockIsland island) {
        int count = 0;
        for (var permission : VISITOR_PERMISSIONS) {
            if (isPermissionEnabled(island, permission.key())) {
                count++;
            }
        }
        return count;
    }
    
    private void saveVisitorSettings(@NotNull OneblockIsland island) {
        // Simplified save - in full implementation would use proper repository
        // For now, just log that settings would be saved
        java.util.logging.Logger.getLogger("JExOneblock").info("Visitor settings updated for island " + island.getId());
    }
    
    /**
     * Record representing a visitor permission configuration.
     */
    private record VisitorPermission(
        String key,
        Material material,
        String displayName,
        String description
    ) {}
}
