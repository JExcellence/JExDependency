package com.raindropcentral.rdq.command;

import de.jexcellence.evaluable.section.IPermissionNode;
import org.jetbrains.annotations.NotNull;

/**
 * Permission nodes for the /bounty command.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public enum EBountyPermission implements IPermissionNode {
    USE("command", "bounty.command"),
    CREATE("commandCreate", "bounty.command.create"),
    LIST("commandList", "bounty.command.list"),
    CANCEL("commandCancel", "bounty.command.cancel"),
    LEADERBOARD("commandLeaderboard", "bounty.command.leaderboard"),
    ADMIN("commandAdmin", "bounty.command.admin"),
    REMOVE("commandRemove", "bounty.command.remove"),
    RELOAD("commandReload", "bounty.command.reload");

    private final String internalName;
    private final String fallbackNode;

    EBountyPermission(final @NotNull String internalName, final @NotNull String fallbackNode) {
        this.internalName = internalName;
        this.fallbackNode = fallbackNode;
    }

    @Override
    public @NotNull String getInternalName() {
        return internalName;
    }

    @Override
    public @NotNull String getFallbackNode() {
        return fallbackNode;
    }
}
