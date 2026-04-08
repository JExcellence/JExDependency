package de.jexcellence.oneblock.view.island;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIslandMember;
import de.jexcellence.oneblock.database.entity.oneblock.EOneblockIslandRole;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.regex.Pattern;

public class IslandSettingsView extends BaseView {
    
    private final State<JExOneblock> plugin = initialState("plugin");
    private final State<OneblockIsland> island = initialState("island");
    
    private static final Pattern ISLAND_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\s]{3,32}$");
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 32;
    
    @Override
    protected String getKey() {
        return "island_settings_ui";
    }
    
    @Override
    protected String[] getLayout() {
        return new String[]{
            "XXXXXXXXX",
            "X N D P X",
            "X V T R X",
            "X S L B X",
            "XXXXXXXXX"
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
            "island_name", islandData.getIslandName() != null ? islandData.getIslandName() : "Unnamed Island",
            "privacy", islandData.isPublic() ? "Public" : "Private"
        );
    }
    
    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        var islandData = island.get(render);
        
        if (!hasSettingsPermission(render, player, islandData)) {
            i18n("island.settings.no_permission", player).includePrefix().build().sendMessage();
            player.closeInventory();
            return;
        }
        
        renderSettingsOptions(render, player, islandData);
        renderBorder(render);
    }
    
    private void renderSettingsOptions(@NotNull RenderContext render, @NotNull Player player, @NotNull OneblockIsland island) {
        // Island Name
        render.layoutSlot('N', UnifiedBuilderFactory
            .item(Material.NAME_TAG)
            .setName(i18n("island.settings.name.title", player).build().component())
            .setLore(i18n("island.settings.name.lore", player)
                .withPlaceholder("current_name", island.getIslandName() != null ? island.getIslandName() : "Unnamed Island")
                .withPlaceholder("min_length", MIN_NAME_LENGTH)
                .withPlaceholder("max_length", MAX_NAME_LENGTH)
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            ctx.getPlayer().closeInventory();
            i18n("island.settings.name.prompt", player)
                .withPlaceholder("current", island.getIslandName() != null ? island.getIslandName() : "Unnamed Island")
                .includePrefix()
                .build().sendMessage();
        });
        
        // Island Description
        render.layoutSlot('D', UnifiedBuilderFactory
            .item(Material.WRITABLE_BOOK)
            .setName(i18n("island.settings.description.title", player).build().component())
            .setLore(i18n("island.settings.description.lore", player)
                .withPlaceholder("current_description", island.getDescription() != null ? 
                    (island.getDescription().length() > 50 ? 
                        island.getDescription().substring(0, 47) + "..." : 
                        island.getDescription()) : "No description set")
                .withPlaceholder("max_length", MAX_DESCRIPTION_LENGTH)
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            // Close GUI and prompt for new description
            ctx.getPlayer().closeInventory();
            i18n("island.settings.description.prompt", player)
                .withPlaceholder("current", island.getDescription() != null ? island.getDescription() : "No description")
                .includePrefix()
                .build().sendMessage();
            // TODO: Implement chat input handler for description change
        });
        
        // Privacy Toggle (P)
        var isPublic = island.isPublic();
        render.layoutSlot('P', UnifiedBuilderFactory
            .item(isPublic ? Material.LIME_CONCRETE : Material.RED_CONCRETE)
            .setName(i18n("island.settings.privacy.title", player)
                .withPlaceholder("status", isPublic ? "Public" : "Private")
                .build().component())
            .setLore(i18n("island.settings.privacy.lore", player)
                .withPlaceholder("current_status", isPublic ? "Public" : "Private")
                .withPlaceholder("new_status", isPublic ? "Private" : "Public")
                .withPlaceholder("description", isPublic ? 
                    "Anyone can find and visit your island" : 
                    "Only invited players can visit your island")
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            togglePrivacy(ctx, player, island);
            ctx.update();
        });
        
        // Visitor Settings (V)
        render.layoutSlot('V', UnifiedBuilderFactory
            .item(Material.IRON_DOOR)
            .setName(i18n("island.settings.visitors.title", player).build().component())
            .setLore(i18n("island.settings.visitors.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> ctx.openForPlayer(VisitorSettingsView.class, ctx.getInitialData()));
        
        // Teleport Settings (T)
        render.layoutSlot('T', UnifiedBuilderFactory
            .item(Material.ENDER_PEARL)
            .setName(i18n("island.settings.teleport.title", player).build().component())
            .setLore(i18n("island.settings.teleport.lore", player)
                .withPlaceholder("spawn_x", island.getSpawnX())
                .withPlaceholder("spawn_y", island.getSpawnY())
                .withPlaceholder("spawn_z", island.getSpawnZ())
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            // Set spawn to player's current location
            setIslandSpawn(ctx, player, island);
            ctx.update();
        });
        
        // Reset Island (R) - Dangerous operation
        var canReset = hasResetPermission(player, island);
        render.layoutSlot('R', UnifiedBuilderFactory
            .item(canReset ? Material.TNT : Material.GRAY_DYE)
            .setName(i18n("island.settings.reset.title", player).build().component())
            .setLore(canReset ? 
                i18n("island.settings.reset.lore", player).build().children() :
                i18n("island.settings.reset.no_permission", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            if (canReset) {
                showResetConfirmation(player, island);
            } else {
                i18n("island.settings.reset.no_permission_msg", player).includePrefix().build().sendMessage();
            }
        });
        
        // Statistics (S)
        render.layoutSlot('S', UnifiedBuilderFactory
            .item(Material.BOOK)
            .setName(i18n("island.settings.statistics.title", player).build().component())
            .setLore(i18n("island.settings.statistics.lore", player)
                .withPlaceholder("created", island.getCreatedAt().toLocalDate().toString())
                .withPlaceholder("level", island.getLevel())
                .withPlaceholder("experience", String.format("%.1f", island.getExperience()))
                .withPlaceholder("blocks_broken", formatNumber(island.getTotalBlocksBroken()))
                .withPlaceholder("coins", formatNumber(island.getIslandCoins()))
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> showDetailedStatistics(ctx, player, island));
        
        // Language Settings (L)
        render.layoutSlot('L', UnifiedBuilderFactory
            .item(Material.ENCHANTED_BOOK)
            .setName(i18n("island.settings.language.title", player).build().component())
            .setLore(i18n("island.settings.language.lore", player)
                .withPlaceholder("current_language", "English") // TODO: Get from player settings
                .build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            // TODO: Implement language selection
            i18n("island.settings.language.coming_soon", player).includePrefix().build().sendMessage();
        });
        
        // Back Button (B)
        render.layoutSlot('B', UnifiedBuilderFactory
            .item(Material.BARRIER)
            .setName(i18n("common.back", player).build().component())
            .build()
        ).onClick(ctx -> ctx.openForPlayer(IslandMainView.class, ctx.getInitialData()));
    }
    
    private void renderBorder(@NotNull RenderContext render) {
        render.layoutSlot('X', UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(Component.text(""))
            .build()
        );
    }
    
    // Helper methods
    
    private boolean hasSettingsPermission(@NotNull RenderContext render, @NotNull Player player, @NotNull OneblockIsland island) {
        // Owner can always modify settings
        if (island.getOwnerUuid().equals(player.getUniqueId())) {
            return true;
        }
        
        // Check member role - Co-Owner+ can modify settings
        var memberService = plugin.get(render).getIslandMemberService();
        // Create OneblockPlayer from UUID - simplified approach
        var oneblockPlayer = new de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer();
        oneblockPlayer.setUniqueId(player.getUniqueId());
        var role = memberService.getMemberRole(island, oneblockPlayer);
        
        return role.isPresent() && role.get() == OneblockIslandMember.MemberRole.CO_OWNER;
    }
    
    private boolean hasResetPermission(@NotNull Player player, @NotNull OneblockIsland island) {
        // Only owner can reset island
        return island.getOwnerUuid().equals(player.getUniqueId());
    }
    
    private void togglePrivacy(@NotNull SlotClickContext ctx, @NotNull Player player, @NotNull OneblockIsland island) {
        var newStatus = !island.isPublic();
        island.setPublic(newStatus);
        
        // Save to database
        saveIslandSettings(ctx, island);
        
        var statusText = newStatus ? "Public" : "Private";
        i18n("island.settings.privacy.changed", player)
            .withPlaceholder("status", statusText)
            .includePrefix()
            .build().sendMessage();
        
        if (newStatus) {
            i18n("island.settings.privacy.public_warning", player).build().sendMessage();
        } else {
            i18n("island.settings.privacy.private_info", player).build().sendMessage();
        }
    }
    
    private void setIslandSpawn(@NotNull SlotClickContext ctx, @NotNull Player player, @NotNull OneblockIsland island) {
        var location = player.getLocation();
        
        // Validate player is within island bounds
        var islandRegion = island.getRegion();
        if (islandRegion != null && !islandRegion.contains(location)) {
            i18n("island.settings.spawn.outside_bounds", player).includePrefix().build().sendMessage();
            return;
        }
        
        island.setSpawnX(location.getBlockX());
        island.setSpawnY(location.getBlockY());
        island.setSpawnZ(location.getBlockZ());
        
        // Save to database
        saveIslandSettings(ctx, island);
        
        i18n("island.settings.spawn.set", player)
            .withPlaceholder("x", location.getBlockX())
            .withPlaceholder("y", location.getBlockY())
            .withPlaceholder("z", location.getBlockZ())
            .includePrefix()
            .build().sendMessage();
    }
    
    private void showResetConfirmation(@NotNull Player player, @NotNull OneblockIsland island) {
        i18n("island.settings.reset.confirm.header", player).includePrefix().build().sendMessage();
        i18n("island.settings.reset.confirm.warning1", player).build().sendMessage();
        i18n("island.settings.reset.confirm.warning2", player).build().sendMessage();
        i18n("island.settings.reset.confirm.warning3", player).build().sendMessage();
        
        i18n("island.settings.reset.confirm.what_keeps", player).build().sendMessage();
        i18n("island.settings.reset.confirm.keeps.members", player).build().sendMessage();
        i18n("island.settings.reset.confirm.keeps.settings", player).build().sendMessage();
        i18n("island.settings.reset.confirm.keeps.name", player).build().sendMessage();
        
        i18n("island.settings.reset.confirm.what_resets", player).build().sendMessage();
        i18n("island.settings.reset.confirm.resets.blocks", player).build().sendMessage();
        i18n("island.settings.reset.confirm.resets.level", player).build().sendMessage();
        i18n("island.settings.reset.confirm.resets.evolution", player).build().sendMessage();
        i18n("island.settings.reset.confirm.resets.inventory", player).build().sendMessage();
        
        i18n("island.settings.reset.confirm.prompt", player).build().sendMessage();
        
        // TODO: Implement confirmation system
        // For now, just show the warning
    }
    
    private void showDetailedStatistics(@NotNull SlotClickContext ctx, @NotNull Player player, @NotNull OneblockIsland island) {
        i18n("island.settings.statistics.detailed.header", player).includePrefix().build().sendMessage();
        
        i18n("island.settings.statistics.detailed.basic", player)
            .withPlaceholder("name", island.getIslandName() != null ? island.getIslandName() : "Unnamed Island")
            .withPlaceholder("level", island.getLevel())
            .withPlaceholder("experience", String.format("%.1f", island.getExperience()))
            .build().sendMessage();
        
        i18n("island.settings.statistics.detailed.progress", player)
            .withPlaceholder("evolution", island.getCurrentEvolution())
            .withPlaceholder("blocks_broken", formatNumber(island.getTotalBlocksBroken()))
            .withPlaceholder("coins", formatNumber(island.getIslandCoins()))
            .build().sendMessage();
        
        i18n("island.settings.statistics.detailed.dates", player)
            .withPlaceholder("created", island.getCreatedAt().toLocalDate().toString())
            .withPlaceholder("last_activity", java.time.Instant.ofEpochMilli(island.getLastActivity()).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString())
            .build().sendMessage();
        
        // Member statistics - simplified for now
        var memberService = plugin.get(ctx).getIslandMemberService();
        // var members = memberService.getMembers(island);
        // var activeMembers = memberService.getActiveMembers(island);
        
        i18n("island.settings.statistics.detailed.members", player)
            .withPlaceholder("total_members", "N/A") // Simplified - would need async handling
            .withPlaceholder("active_members", "N/A") // Simplified - would need async handling
            .build().sendMessage();
        
        // Size and upgrades
        i18n("island.settings.statistics.detailed.technical", player)
            .withPlaceholder("size", island.getSize())
            .withPlaceholder("privacy", island.isPublic() ? "Public" : "Private")
            .withPlaceholder("spawn", island.getSpawnX() + ", " + island.getSpawnY() + ", " + island.getSpawnZ())
            .build().sendMessage();
    }
    
    private void saveIslandSettings(@NotNull SlotClickContext ctx, @NotNull OneblockIsland island) {
        // Simplified save - in full implementation would use proper repository
        // For now, just log that settings would be saved
        java.util.logging.Logger.getLogger("JExOneblock").info("Island settings updated for island " + island.getId());
    }
    
    private String formatNumber(long number) {
        if (number >= 1_000_000_000) return String.format("%.1fB", number / 1_000_000_000.0);
        if (number >= 1_000_000) return String.format("%.1fM", number / 1_000_000.0);
        if (number >= 1_000) return String.format("%.1fK", number / 1_000.0);
        return String.valueOf(number);
    }
    
    // Validation methods
    
    public static boolean isValidIslandName(@NotNull String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        var trimmed = name.trim();
        if (trimmed.length() < MIN_NAME_LENGTH || trimmed.length() > MAX_NAME_LENGTH) {
            return false;
        }
        
        return ISLAND_NAME_PATTERN.matcher(trimmed).matches();
    }
    
    public static boolean isValidDescription(@NotNull String description) {
        return description != null && description.length() <= MAX_DESCRIPTION_LENGTH;
    }
    
    public static String getNameValidationError(@NotNull String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Name cannot be empty";
        }
        
        var trimmed = name.trim();
        if (trimmed.length() < MIN_NAME_LENGTH) {
            return "Name must be at least " + MIN_NAME_LENGTH + " characters long";
        }
        
        if (trimmed.length() > MAX_NAME_LENGTH) {
            return "Name cannot be longer than " + MAX_NAME_LENGTH + " characters";
        }
        
        if (!ISLAND_NAME_PATTERN.matcher(trimmed).matches()) {
            return "Name can only contain letters, numbers, spaces, hyphens, and underscores";
        }
        
        return null; // Valid
    }
    
    public static String getDescriptionValidationError(@NotNull String description) {
        if (description == null) {
            return null; // Null descriptions are allowed (means no description)
        }
        
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            return "Description cannot be longer than " + MAX_DESCRIPTION_LENGTH + " characters";
        }
        
        return null; // Valid
    }
}
