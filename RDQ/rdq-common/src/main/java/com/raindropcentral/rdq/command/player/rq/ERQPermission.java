package com.raindropcentral.rdq.command.player.rq;

import de.jexcellence.evaluable.section.IPermissionNode;
import org.jetbrains.annotations.NotNull;

/**
 * Declares the permission nodes governing access to {@code /prq} actions.
 * <p>
 * Each enum constant supplies the internal configuration key consumed by the
 * command framework along with a fallback Bukkit permission string used when
 * edition-specific overrides are absent.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public enum ERQPermission implements IPermissionNode {

    /**
     * Root permission gating access to the base {@code /prq} command executor.
     */
    COMMAND("command", "raindropquests.command"),

    /**
     * Administrative permission granting access to moderator-only subcommands.
     */
    ADMIN("commandAdmin", "raindropquests.command.admin"),

    /**
     * Permission protecting bounty-related interactions inside the quest command.
     */
    BOUNTY("commandBounty", "raindropquests.command.bounty"),

    /**
     * Permission node for the main quest GUI and dashboard entry point.
     */
    MAIN("commandMain", "raindropquests.command.main"),

    /**
     * Permission exposing the quest listing interfaces and actions.
     */
    QUESTS("commandQuests", "raindropquests.command.quests"),

    /**
     * Permission controlling access to the player rank management views.
     */
    RANKS("commandRanks", "raindropquests.command.ranks"),

    /**
     * Permission required to open and use perk related quest interfaces.
     */
    PERKS("commandPerks", "raindropquests.command.perks");

    /**
     * Identifier stored within the command section configuration.
     */
    private final String internalName;

    /**
     * Bukkit permission used when no translated permission node exists.
     */
    private final String fallbackNode;

    /**
     * Creates a new permission mapping.
     *
     * @param internalName configuration key that identifies the command section
     * @param fallbackNode default Bukkit permission used when no override exists
     */
    ERQPermission(
            final @NotNull String internalName,
            final @NotNull String fallbackNode
    ) {
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }

    /**
     * Provides the command-section identifier used when resolving permissions
     * from configuration.
     *
     * @return internal configuration key for the permission
     */
    @Override
    public @NotNull String getInternalName() {
        return this.internalName;
    }

    /**
     * Supplies the fallback Bukkit permission node consumed by the command
     * framework when no edition override is present.
     *
     * @return fallback permission node string
     */
    @Override
    public @NotNull String getFallbackNode() {
        return this.fallbackNode;
    }
}