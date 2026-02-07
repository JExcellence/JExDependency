package com.raindropcentral.rdt.commands;

import de.jexcellence.evaluable.section.IPermissionNode;
import lombok.Getter;

/**
 * Permission nodes used by the primary player command ({@code /prt}).
 * <p>
 * Each enum value provides an {@code internalName} used by the configuration system
 * and a {@code fallbackNode} string that represents the default Bukkit permission
 * when no explicit mapping is provided.
 */
@Getter
public enum EPRTPermission implements IPermissionNode{
    /** Generic permission to use the base command and tab completion. */
    COMMAND("command","raindroptowns.comamnd"),
    /** Create a new town. */
    CREATE("createCommand","raindroptowns.command.create"),
    /** Delete your town (mayor only). */
    DELETE("deleteCommand","raindroptowns.command.delete"),
    /** View information about your town. */
    INFO("infoCommand","raindroptowns.command.info"),
    /** Invite a player to your town. */
    INVITE("inviteCommand","raindroptowns.command.invite"),
    /** Request to join a town. */
    JOIN("joinCommand","raindroptowns.command.join"),
    /** Accept invitations or (as mayor) accept join requests. */
    ACCEPT("acceptCommand","raindroptowns.command.accept"),
    /** Claim chunks for your town (mayor only). */
    CLAIM("claimCommand","raindroptowns.command.claim"),
    //unclaim a chunk
    UNCLAIM("unclaimCommand","raindroptowns.command.unclaim"),
    /** Print debug output for your town. */
    DEBUG("debugCommand","raindroptowns.command.debug"),
    //deposit money to the town bank
    DEPOSIT("depositCommand","raindroptowns.command.deposit"),
    //withdraw money from the town bank
    WITHDRAW("withdrawCommand","raindroptowns.command.withdraw");

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
    EPRTPermission(String internalName, String fallbackNode){
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }
}