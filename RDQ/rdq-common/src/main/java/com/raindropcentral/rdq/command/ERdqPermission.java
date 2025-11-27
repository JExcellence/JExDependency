package com.raindropcentral.rdq.command;

import de.jexcellence.evaluable.section.IPermissionNode;
import org.jetbrains.annotations.NotNull;

/**
 * Permission nodes for the /rdq command.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public enum ERdqPermission implements IPermissionNode {
    USE("command", "rdq.command"),
    RELOAD("commandReload", "rdq.command.reload"),
    ADMIN("commandAdmin", "rdq.command.admin");

    private final String internalName;
    private final String fallbackNode;

    ERdqPermission(final @NotNull String internalName, final @NotNull String fallbackNode) {
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
