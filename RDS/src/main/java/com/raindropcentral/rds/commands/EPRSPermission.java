/*
 * EPRSPermission.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

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
    BAR("commandBar","raindropshops.command.bar"),
    INFO("commandInfo","raindropshops.command.info"),
    GIVE("commandGive","raindropshops.command.give"),
    SCOREBOARD("commandScoreboard","raindropshops.command.scoreboard"),
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

    /**
     * Returns the internal name.
     *
     * @return the internal name
     */
    @Override
    public String getInternalName() {
        return this.internalName;
    }

    /**
     * Returns the fallback node.
     *
     * @return the fallback node
     */
    @Override
    public String getFallbackNode() {
        return this.fallbackNode;
    }
}