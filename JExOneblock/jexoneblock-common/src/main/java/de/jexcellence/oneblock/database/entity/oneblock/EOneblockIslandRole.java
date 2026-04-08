package de.jexcellence.oneblock.database.entity.oneblock;

import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of island member roles with hierarchical permissions.
 * Defines the different roles a player can have on an island.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
public enum EOneblockIslandRole {
    
    /**
     * Visitor - Can only visit public areas
     */
    VISITOR(0, "island.role.visitor", "island.role.description.visitor"),
    
    /**
     * Member - Basic island member with limited permissions
     */
    MEMBER(1, "island.role.member", "island.role.description.member"),
    
    /**
     * Trusted - Trusted member with extended permissions
     */
    TRUSTED(2, "island.role.trusted", "island.role.description.trusted"),
    
    /**
     * Moderator - Can manage other members and island settings
     */
    MODERATOR(3, "island.role.moderator", "island.role.description.moderator"),
    
    /**
     * Co-Owner - Has almost all permissions except ownership transfer
     */
    CO_OWNER(4, "island.role.co_owner", "island.role.description.co_owner");
    
    private final int level;
    private final String displayNameKey;
    private final String descriptionKey;
    
    EOneblockIslandRole(int level, String displayNameKey, String descriptionKey) {
        this.level = level;
        this.displayNameKey = displayNameKey;
        this.descriptionKey = descriptionKey;
    }
    
    /**
     * Gets the permission level of this role.
     * Higher numbers indicate higher permissions.
     * 
     * @return the permission level
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Gets the raw display name key for this role.
     * Use this with the i18n system in views.
     * 
     * @return the display name translation key
     */
    @NotNull
    public String getDisplayNameKey() {
        return displayNameKey;
    }
    
    /**
     * Gets the raw description key for this role.
     * Use this with the i18n system in views.
     * 
     * @return the description translation key
     */
    @NotNull
    public String getDescriptionKey() {
        return descriptionKey;
    }
    
    /**
     * Checks if this role has permission to manage the target role.
     * 
     * @param targetRole the role to check against
     * @return true if this role can manage the target role
     */
    public boolean canManage(@NotNull EOneblockIslandRole targetRole) {
        return this.level > targetRole.level;
    }
    
    /**
     * Checks if this role has at least the required permission level.
     * 
     * @param requiredRole the minimum required role
     * @return true if this role meets or exceeds the requirement
     */
    public boolean hasPermission(@NotNull EOneblockIslandRole requiredRole) {
        return this.level >= requiredRole.level;
    }
    
    /**
     * Gets a role by its permission level.
     * 
     * @param level the permission level
     * @return the role with that level, or null if not found
     */
    public static EOneblockIslandRole getByLevel(int level) {
        for (EOneblockIslandRole role : values()) {
            if (role.level == level) {
                return role;
            }
        }
        return null;
    }
    
    /**
     * Gets a role by its display name key.
     * 
     * @param displayNameKey the display name key
     * @return the role with that key, or null if not found
     */
    public static EOneblockIslandRole getByDisplayNameKey(@NotNull String displayNameKey) {
        for (EOneblockIslandRole role : values()) {
            if (role.displayNameKey.equals(displayNameKey)) {
                return role;
            }
        }
        return null;
    }
}