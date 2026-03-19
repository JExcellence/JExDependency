package de.jexcellence.oneblock.manager.permission;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of specific permission types for island actions.
 * Each permission type represents a specific action that can be performed on an island.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@Getter
public enum PermissionType {
    
    // Basic permissions
    VISIT("Visit", "Allow visiting the island", PermissionLevel.VISITOR),
    TELEPORT("Teleport", "Allow teleporting to the island", PermissionLevel.VISITOR),
    
    // Interaction permissions
    USE_DOORS("Use Doors", "Allow using doors and gates", PermissionLevel.INTERACT),
    USE_BUTTONS("Use Buttons", "Allow using buttons and levers", PermissionLevel.INTERACT),
    USE_PRESSURE_PLATES("Use Pressure Plates", "Allow triggering pressure plates", PermissionLevel.INTERACT),
    
    // Building permissions
    PLACE_BLOCKS("Place Blocks", "Allow placing blocks", PermissionLevel.BUILD),
    BREAK_BLOCKS("Break Blocks", "Allow breaking blocks", PermissionLevel.BUILD),
    USE_TOOLS("Use Tools", "Allow using tools and weapons", PermissionLevel.BUILD),
    
    // Container permissions
    OPEN_CHESTS("Open Chests", "Allow opening chests and storage", PermissionLevel.CONTAINER),
    USE_FURNACES("Use Furnaces", "Allow using furnaces and crafting stations", PermissionLevel.CONTAINER),
    USE_BREWING("Use Brewing", "Allow using brewing stands", PermissionLevel.CONTAINER),
    USE_ENCHANTING("Use Enchanting", "Allow using enchanting tables", PermissionLevel.CONTAINER),
    
    // Animal permissions
    INTERACT_ANIMALS("Interact Animals", "Allow interacting with animals", PermissionLevel.ANIMAL),
    BREED_ANIMALS("Breed Animals", "Allow breeding animals", PermissionLevel.ANIMAL),
    KILL_ANIMALS("Kill Animals", "Allow killing animals", PermissionLevel.ANIMAL),
    
    // Redstone permissions
    USE_REDSTONE("Use Redstone", "Allow using redstone components", PermissionLevel.REDSTONE),
    MODIFY_REDSTONE("Modify Redstone", "Allow modifying redstone circuits", PermissionLevel.REDSTONE),
    
    // Management permissions
    INVITE_PLAYERS("Invite Players", "Allow inviting other players", PermissionLevel.MANAGE),
    KICK_PLAYERS("Kick Players", "Allow kicking players", PermissionLevel.MANAGE),
    CHANGE_SETTINGS("Change Settings", "Allow changing island settings", PermissionLevel.MANAGE),
    MANAGE_WARPS("Manage Warps", "Allow managing island warps", PermissionLevel.MANAGE),
    
    // Administrative permissions
    BAN_PLAYERS("Ban Players", "Allow banning players", PermissionLevel.ADMIN),
    TRANSFER_OWNERSHIP("Transfer Ownership", "Allow transferring island ownership", PermissionLevel.ADMIN),
    DELETE_ISLAND("Delete Island", "Allow deleting the island", PermissionLevel.ADMIN),
    RESET_ISLAND("Reset Island", "Allow resetting the island", PermissionLevel.ADMIN);
    
    private final String displayName;
    private final String description;
    private final PermissionLevel requiredLevel;
    
    PermissionType(String displayName, String description, PermissionLevel requiredLevel) {
        this.displayName = displayName;
        this.description = description;
        this.requiredLevel = requiredLevel;
    }
    
    /**
     * Checks if a permission level allows this permission type.
     * 
     * @param permissionLevel the permission level to check
     * @return true if the permission level allows this action
     */
    public boolean isAllowedBy(@NotNull PermissionLevel permissionLevel) {
        return permissionLevel.isAtLeast(this.requiredLevel);
    }
    
    /**
     * Gets the permission type by name (case-insensitive).
     * 
     * @param name the permission type name
     * @return the permission type or null if not found
     */
    @NotNull
    public static PermissionType fromName(@NotNull String name) {
        for (PermissionType type : values()) {
            if (type.name().equalsIgnoreCase(name) || 
                type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown permission type: " + name);
    }
    
    /**
     * Gets all permission types that require a specific permission level or higher.
     * 
     * @param minLevel the minimum permission level
     * @return array of permission types
     */
    @NotNull
    public static PermissionType[] getByMinLevel(@NotNull PermissionLevel minLevel) {
        return java.util.Arrays.stream(values())
            .filter(type -> type.requiredLevel.isAtLeast(minLevel))
            .toArray(PermissionType[]::new);
    }
    
    /**
     * Gets all permission types for a specific permission level.
     * 
     * @param level the permission level
     * @return array of permission types
     */
    @NotNull
    public static PermissionType[] getByLevel(@NotNull PermissionLevel level) {
        return java.util.Arrays.stream(values())
            .filter(type -> type.requiredLevel == level)
            .toArray(PermissionType[]::new);
    }
}