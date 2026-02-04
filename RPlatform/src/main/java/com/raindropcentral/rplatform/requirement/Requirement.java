package com.raindropcentral.rplatform.requirement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Sealed interface for all requirement types in the RPlatform system.
 * <p>
 * Each requirement defines its own type through its class and registration.
 * </p>
 */
public sealed interface Requirement 
    permits AbstractRequirement {

    @NotNull String getTypeId();
    boolean isMet(@NotNull Player player);
    double calculateProgress(@NotNull Player player);
    void consume(@NotNull Player player);
    
    @JsonIgnore
    @NotNull String getDescriptionKey();
}
