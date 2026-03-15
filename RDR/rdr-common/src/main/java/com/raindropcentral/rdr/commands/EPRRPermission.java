package com.raindropcentral.rdr.commands;

import de.jexcellence.evaluable.section.IPermissionNode;

/**
 * Permission nodes used by the primary player command ({@code /prr}).
 *
 * <p>Each enum value provides an {@code internalName} used by the configuration system
 * and a {@code fallbackNode} string that represents the default Bukkit permission
 * when no explicit mapping is provided.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public enum EPRRPermission implements IPermissionNode{
    COMMAND("command","raindroprdr.command"),
    ADMIN("commandAdmin","raindroprdr.command.admin"),
    INFO("commandInfo","raindroprdr.command.info"),
    SCOREBOARD("commandScoreboard","raindroprdr.command.scoreboard"),
    STORAGE("commandStorage","raindroprdr.command.storage"),
    TRADE("commandTrade","raindroprdr.command.trade")
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
    EPRRPermission(String internalName, String fallbackNode){
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
