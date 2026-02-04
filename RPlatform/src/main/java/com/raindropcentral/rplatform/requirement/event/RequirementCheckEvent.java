package com.raindropcentral.rplatform.requirement.event;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Called before a requirement is checked.
 * <p>
 * Can be cancelled to prevent the check.
 * </p>
 */
public final class RequirementCheckEvent extends RequirementEvent implements Cancellable {

    private boolean cancelled = false;

    public RequirementCheckEvent(@NotNull Player player, @NotNull AbstractRequirement requirement) {
        super(player, requirement);
    }

    public RequirementCheckEvent(@NotNull Player player, @NotNull AbstractRequirement requirement, boolean async) {
        super(player, requirement, async);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
