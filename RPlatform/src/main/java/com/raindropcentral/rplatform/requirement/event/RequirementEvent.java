package com.raindropcentral.rplatform.requirement.event;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Base event for requirement-related events.
 */
public abstract class RequirementEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final AbstractRequirement requirement;

    protected RequirementEvent(@NotNull Player player, @NotNull AbstractRequirement requirement) {
        this.player = player;
        this.requirement = requirement;
    }

    protected RequirementEvent(@NotNull Player player, @NotNull AbstractRequirement requirement, boolean async) {
        super(async);
        this.player = player;
        this.requirement = requirement;
    }

    /**
     * Gets player.
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets requirement.
     */
    @NotNull
    public AbstractRequirement getRequirement() {
        return requirement;
    }

    /**
     * Gets handlers.
     */
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Gets handlerList.
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
