package com.raindropcentral.rplatform.requirement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Sealed interface for all requirement types in the RPlatform system.
 *
 * <p>Each requirement defines its own type through its class and registration.
 */
public sealed interface Requirement 
    permits AbstractRequirement {

    /**
     * Executes this member.
     */
    /**
     * Returns whether met.
     */
    @NotNull String getTypeId();
    /**
     * Executes isMet.
     */
    boolean isMet(@NotNull Player player);
    /**
     * Executes calculateProgress.
     */
    double calculateProgress(@NotNull Player player);
    /**
     * Executes consume.
     */
    void consume(@NotNull Player player);
    
    /**
     * Executes this member.
     */
    @JsonIgnore
    @NotNull String getDescriptionKey();
}
