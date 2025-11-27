package com.raindropcentral.rdq.command;

import de.jexcellence.evaluable.section.IPermissionNode;
import org.jetbrains.annotations.NotNull;

/**
 * Permission nodes for the /perk command.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public enum EPerkPermission implements IPermissionNode {
    USE("command", "perk.command"),
    LIST("commandList", "perk.command.list"),
    ACTIVATE("commandActivate", "perk.command.activate"),
    DEACTIVATE("commandDeactivate", "perk.command.deactivate"),
    INFO("commandInfo", "perk.command.info"),
    ADMIN("commandAdmin", "perk.command.admin"),
    GRANT("commandGrant", "perk.command.grant"),
    REVOKE("commandRevoke", "perk.command.revoke"),
    RELOAD("commandReload", "perk.command.reload");

    private final String internalName;
    private final String fallbackNode;

    EPerkPermission(final @NotNull String internalName, final @NotNull String fallbackNode) {
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
