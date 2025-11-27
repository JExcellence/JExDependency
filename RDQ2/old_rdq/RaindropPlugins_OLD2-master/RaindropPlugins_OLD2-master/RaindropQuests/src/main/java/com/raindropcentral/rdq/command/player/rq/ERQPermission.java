package com.raindropcentral.rdq.command.player.rq;

import de.jexcellence.evaluable.section.IPermissionNode;

/**
 * Enumeration of permission nodes used for admin-related commands.
 * <p>
 * Each enum constant represents a specific permission node, providing both an internal name
 * and a fallback permission node string for use in permission checks.
 * </p>
 *
 * @author ItsRainingHP, JExcellence
 * @version 1.0.0
 * @since TBD
 */
public enum ERQPermission implements IPermissionNode {

    /**
     * Permission node for the admin command.
     */
    COMMAND("command", "raindropquests.admin.command"),
    ADMIN("commandAdmin", "raindropquests.admin.command.admin"),
    BOUNTY("commandBounty", "raindropquests.admin.command.bounty"),
    MAIN("commandMain", "raindropquests.admin.command.main"),
    QUESTS("commandQuests", "raindropquests.admin.command.quests"),
    RANKS("commandRanks", "raindropquests.admin.command.ranks"),
    PERKS("commandPerks", "raindropquests.admin.command.perks"),
    ;

    /**
     * The internal name of the permission node.
     */
    private final String internalName;

    /**
     * The fallback permission node string.
     */
    private final String fallbackNode;

    /**
     * Constructs a new {@code EAdminPermission} enum constant.
     *
     * @param internalName the internal name of the permission node
     * @param fallbackNode the fallback permission node string
     */
    ERQPermission(final String internalName, final String fallbackNode) {
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }

    /**
     * Gets the internal name of this permission node.
     *
     * @return the internal name
     */
    public String getInternalName() {
        return this.internalName;
    }

    /**
     * Gets the fallback permission node string.
     *
     * @return the fallback node string
     */
    public String getFallbackNode() {
        return this.fallbackNode;
    }
}
