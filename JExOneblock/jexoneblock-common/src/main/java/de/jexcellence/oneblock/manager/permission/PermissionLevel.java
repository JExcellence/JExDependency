package de.jexcellence.oneblock.manager.permission;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of permission levels for island actions.
 * Permission levels determine what specific actions a player can perform.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@Getter
public enum PermissionLevel {
    
    /**
     * No permissions - completely restricted
     */
    NONE("None", 0),
    
    /**
     * Basic visitor permissions - can walk around
     */
    VISITOR("Visitor", 1),
    
    /**
     * Basic interaction permissions - can use doors, buttons, etc.
     */
    INTERACT("Interact", 2),
    
    /**
     * Building permissions - can place and break blocks
     */
    BUILD("Build", 3),
    
    /**
     * Container permissions - can access chests, furnaces, etc.
     */
    CONTAINER("Container", 4),
    
    /**
     * Animal permissions - can interact with animals
     */
    ANIMAL("Animal", 5),
    
    /**
     * Redstone permissions - can use redstone components
     */
    REDSTONE("Redstone", 6),
    
    /**
     * Management permissions - can manage island settings
     */
    MANAGE("Manage", 7),
    
    /**
     * Administrative permissions - full control
     */
    ADMIN("Admin", 8);
    
    private final String displayName;
    private final int level;
    
    PermissionLevel(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }
    
    /**
     * Checks if this permission level is higher than or equal to another level.
     * 
     * @param other the other permission level to compare
     * @return true if this level is higher or equal
     */
    public boolean isAtLeast(@NotNull PermissionLevel other) {
        return this.level >= other.level;
    }
    
    /**
     * Checks if this permission level is higher than another level.
     * 
     * @param other the other permission level to compare
     * @return true if this level is higher
     */
    public boolean isHigherThan(@NotNull PermissionLevel other) {
        return this.level > other.level;
    }
    
    /**
     * Gets the permission level by name (case-insensitive).
     * 
     * @param name the permission level name
     * @return the permission level or NONE if not found
     */
    @NotNull
    public static PermissionLevel fromName(@NotNull String name) {
        for (PermissionLevel level : values()) {
            if (level.name().equalsIgnoreCase(name) || 
                level.displayName.equalsIgnoreCase(name)) {
                return level;
            }
        }
        return NONE;
    }
    
    /**
     * Gets the permission level by numeric level.
     * 
     * @param level the numeric level
     * @return the permission level or NONE if not found
     */
    @NotNull
    public static PermissionLevel fromLevel(int level) {
        for (PermissionLevel permissionLevel : values()) {
            if (permissionLevel.level == level) {
                return permissionLevel;
            }
        }
        return NONE;
    }
    
    /**
     * Converts a trust level to the corresponding permission level.
     * 
     * @param trustLevel the trust level
     * @return the corresponding permission level
     */
    @NotNull
    public static PermissionLevel fromTrustLevel(@NotNull TrustLevel trustLevel) {
        return switch (trustLevel) {
            case NONE -> NONE;
            case VISITOR -> VISITOR;
            case BASIC -> INTERACT;
            case MEMBER -> BUILD;
            case TRUSTED -> CONTAINER;
            case MODERATOR -> MANAGE;
            case CO_OWNER, OWNER -> ADMIN;
        };
    }
}