package com.raindropcentral.rdq.command.player.rq;

import de.jexcellence.evaluable.section.IPermissionNode;
import org.jetbrains.annotations.NotNull;

/**
 * Enumeration of permission nodes used for RaindropQuests commands.
 * <p>
 * Each enum constant represents a specific permission node, providing both
 * an internal name and a fallback permission node string for use in permission checks.
 * </p>
 *
 * @author ItsRainingHP, JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public enum ERQPermission implements IPermissionNode {

    COMMAND("command", "raindropquests.command"),
    ADMIN("commandAdmin", "raindropquests.command.admin"),
    BOUNTY("commandBounty", "raindropquests.command.bounty"),
    MAIN("commandMain", "raindropquests.command.main"),
    QUESTS("commandQuests", "raindropquests.command.quests"),
    RANKS("commandRanks", "raindropquests.command.ranks"),
    PERKS("commandPerks", "raindropquests.command.perks");

    private final String internalName;
    private final String fallbackNode;

    ERQPermission(
            final @NotNull String internalName,
            final @NotNull String fallbackNode
    ) {
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }

    @Override
    public @NotNull String getInternalName() {
        return this.internalName;
    }

    @Override
    public @NotNull String getFallbackNode() {
        return this.fallbackNode;
    }
}