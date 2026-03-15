package com.raindropcentral.core.commands;

import de.jexcellence.evaluable.section.IPermissionNode;
import org.jetbrains.annotations.NotNull;

/**
 * Permission nodes for RaindropCentral integration commands.
 */
public enum ERCentralPermission implements IPermissionNode {

    CONNECT("connect", "rcore.central.connect"),
    DISCONNECT("disconnect", "rcore.central.disconnect");

    private final String permissionInternalIdentifier;
    private final String fallbackPermissionNode;

    ERCentralPermission(
            final @NotNull String permissionInternalIdentifier,
            final @NotNull String fallbackPermissionNode
    ) {
        this.permissionInternalIdentifier = permissionInternalIdentifier;
        this.fallbackPermissionNode = fallbackPermissionNode;
    }

    /**
     * Gets internalName.
     */
    @Override
    public @NotNull String getInternalName() {
        return this.permissionInternalIdentifier;
    }

    /**
     * Gets fallbackNode.
     */
    @Override
    public @NotNull String getFallbackNode() {
        return this.fallbackPermissionNode;
    }
}
