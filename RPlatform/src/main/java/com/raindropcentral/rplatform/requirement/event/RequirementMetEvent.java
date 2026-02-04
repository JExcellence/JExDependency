package com.raindropcentral.rplatform.requirement.event;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a requirement is met.
 */
public final class RequirementMetEvent extends RequirementEvent {

    private final double progress;

    public RequirementMetEvent(@NotNull Player player, @NotNull AbstractRequirement requirement, double progress) {
        super(player, requirement);
        this.progress = progress;
    }

    public RequirementMetEvent(@NotNull Player player, @NotNull AbstractRequirement requirement, double progress, boolean async) {
        super(player, requirement, async);
        this.progress = progress;
    }

    public double getProgress() {
        return progress;
    }
}
