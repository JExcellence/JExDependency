package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Sealed interface for all requirement types in the RDQ system.
 */
public sealed interface Requirement 
    permits AbstractRequirement {

    enum Type {
        ITEM, PLAYTIME, ACHIEVEMENT, PREVIOUS_LEVEL, COMPOSITE, CHOICE, 
        TIME_BASED, EXPERIENCE_LEVEL, PERMISSION, LOCATION, CUSTOM, 
        CURRENCY, JOBS, SKILLS
    }

    @NotNull Type getType();
    boolean isMet(@NotNull Player player);
    double calculateProgress(@NotNull Player player);
    void consume(@NotNull Player player);
    
    @JsonIgnore
    @NotNull String getDescriptionKey();
}