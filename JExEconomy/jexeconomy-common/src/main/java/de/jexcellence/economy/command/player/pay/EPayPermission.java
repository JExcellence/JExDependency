package de.jexcellence.economy.command.player.pay;

import de.jexcellence.evaluable.section.IPermissionNode;
import org.jetbrains.annotations.NotNull;

/**
 * Permission enumeration for the pay command system.
 * <p>
 * Defines the permission nodes required for executing pay operations
 * within the JExEconomy plugin.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 2.0.0
 */
public enum EPayPermission implements IPermissionNode {

    /**
     * Base permission required to use the pay command.
     */
    PAY("command", "pay.command"),

    /**
     * Permission to bypass pay restrictions (e.g., disabled currencies).
     */
    BYPASS("commandBypass", "pay.command.bypass");

    private final String internalName;
    private final String fallbackNode;

    EPayPermission(
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

    /**
     * Returns the permission node string (fallback node).
     *
     * @return the permission node
     */
    public @NotNull String getPermission() {
        return this.fallbackNode;
    }

    @Override
    public String toString() {
        return this.fallbackNode;
    }
}
