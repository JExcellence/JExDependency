package com.raindropcentral.rdq.command;

import de.jexcellence.evaluable.section.IPermissionNode;
import org.jetbrains.annotations.NotNull;

/**
 * Permission nodes for the /rank command.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public enum ERankPermission implements IPermissionNode {
    USE("command", "rank.command"),
    VIEW("commandView", "rank.command.view"),
    PROGRESS("commandProgress", "rank.command.progress"),
    ADMIN("commandAdmin", "rank.command.admin"),
    GRANT("commandGrant", "rank.command.grant"),
    REVOKE("commandRevoke", "rank.command.revoke"),
    RELOAD("commandReload", "rank.command.reload");

    private final String internalName;
    private final String fallbackNode;

    ERankPermission(final @NotNull String internalName, final @NotNull String fallbackNode) {
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
