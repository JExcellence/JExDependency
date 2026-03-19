package de.jexcellence.oneblock.manager.permission;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of trust levels for island members.
 * Trust levels determine what actions a player can perform on an island.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@Getter
public enum TrustLevel {
    
    /**
     * No trust - player cannot access the island
     */
    NONE("None", 0, false, false, false, false, false),
    
    /**
     * Visitor - can visit but cannot interact
     */
    VISITOR("Visitor", 1, true, false, false, false, false),
    
    /**
     * Basic trust - can interact with basic blocks and items
     */
    BASIC("Basic", 2, true, true, false, false, false),
    
    /**
     * Member - can build, destroy, and use most features
     */
    MEMBER("Member", 3, true, true, true, false, false),
    
    /**
     * Trusted - can manage some island settings and invite others
     */
    TRUSTED("Trusted", 4, true, true, true, true, false),
    
    /**
     * Moderator - can manage members and most island settings
     */
    MODERATOR("Moderator", 5, true, true, true, true, true),
    
    /**
     * Co-Owner - has almost all permissions except ownership transfer
     */
    CO_OWNER("Co-Owner", 6, true, true, true, true, true),
    
    /**
     * Owner - has all permissions
     */
    OWNER("Owner", 7, true, true, true, true, true);
    
    private final String displayName;
    private final int level;
    private final boolean canVisit;
    private final boolean canInteract;
    private final boolean canBuild;
    private final boolean canManageMembers;
    private final boolean canManageSettings;
    
    TrustLevel(String displayName, int level, boolean canVisit, boolean canInteract, 
               boolean canBuild, boolean canManageMembers, boolean canManageSettings) {
        this.displayName = displayName;
        this.level = level;
        this.canVisit = canVisit;
        this.canInteract = canInteract;
        this.canBuild = canBuild;
        this.canManageMembers = canManageMembers;
        this.canManageSettings = canManageSettings;
    }
    
    /**
     * Checks if this trust level is higher than or equal to another level.
     * 
     * @param other the other trust level to compare
     * @return true if this level is higher or equal
     */
    public boolean isAtLeast(@NotNull TrustLevel other) {
        return this.level >= other.level;
    }
    
    /**
     * Checks if this trust level is higher than another level.
     * 
     * @param other the other trust level to compare
     * @return true if this level is higher
     */
    public boolean isHigherThan(@NotNull TrustLevel other) {
        return this.level > other.level;
    }
    
    /**
     * Gets the trust level by name (case-insensitive).
     * 
     * @param name the trust level name
     * @return the trust level or NONE if not found
     */
    @NotNull
    public static TrustLevel fromName(@NotNull String name) {
        for (TrustLevel level : values()) {
            if (level.name().equalsIgnoreCase(name) || 
                level.displayName.equalsIgnoreCase(name)) {
                return level;
            }
        }
        return NONE;
    }
    
    /**
     * Gets the trust level by numeric level.
     * 
     * @param level the numeric level
     * @return the trust level or NONE if not found
     */
    @NotNull
    public static TrustLevel fromLevel(int level) {
        for (TrustLevel trustLevel : values()) {
            if (trustLevel.level == level) {
                return trustLevel;
            }
        }
        return NONE;
    }
    
    /**
     * Checks if this trust level allows the specified action.
     * 
     * @param action the action to check
     * @return true if action is allowed
     */
    public boolean allows(@NotNull TrustAction action) {
        return switch (action) {
            case VISIT -> canVisit;
            case INTERACT -> canInteract;
            case BUILD -> canBuild;
            case MANAGE_MEMBERS -> canManageMembers;
            case MANAGE_SETTINGS -> canManageSettings;
        };
    }
    
    /**
     * Actions that can be performed on an island.
     */
    public enum TrustAction {
        VISIT,
        INTERACT,
        BUILD,
        MANAGE_MEMBERS,
        MANAGE_SETTINGS
    }
}