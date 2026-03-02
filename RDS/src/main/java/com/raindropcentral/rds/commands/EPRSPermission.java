package com.raindropcentral.rds.commands;

import de.jexcellence.evaluable.section.IPermissionNode;

/**
 * Permission nodes used by the primary player command ({@code /prt}).
 * <p>
 * Each enum value provides an {@code internalName} used by the configuration system
 * and a {@code fallbackNode} string that represents the default Bukkit permission
 * when no explicit mapping is provided.
 */

public enum EPRSPermission implements IPermissionNode{
    COMMAND("command","raindropshops.command"),
    INFO("commandInfo","raindropshops.command.info"),
    GIVE("commandGive","raindropshops.command.give"),
    MAIN("commandMain","raindropshops.command.main"),
    SEARCH("commandSearch","raindropshops.command.search"),
    STORE("commandStore","raindropshops.command.store")
    ;

    /** Internal key used by the configuration system to resolve permissions. */
    private final String internalName;

    /** Default Bukkit permission node used when no explicit mapping is provided. */
    private final String fallbackNode;

    /**
     * Construct a permission node descriptor.
     *
     * @param internalName internal configuration key
     * @param fallbackNode default Bukkit permission node
     */
    EPRSPermission(String internalName, String fallbackNode){
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }

    @Override
    public String getInternalName() {
        return this.internalName;
    }

    @Override
    public String getFallbackNode() {
        return this.fallbackNode;
    }
}
