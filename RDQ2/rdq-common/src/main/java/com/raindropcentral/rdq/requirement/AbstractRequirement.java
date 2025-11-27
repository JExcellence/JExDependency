package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;



public abstract non-sealed class AbstractRequirement implements Requirement {
    protected final Type type;

    protected AbstractRequirement(@NotNull Type type) {
        this.type = type;
    }

    @Override
    public @NotNull Type getType() {
        return type;
    }
}