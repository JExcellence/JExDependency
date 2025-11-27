package com.raindropcentral.rdq.view.perks;

import com.raindropcentral.rdq.database.entity.perk.RPerk;
import com.raindropcentral.rdq.type.EPerkCategory;
import com.raindropcentral.rdq.type.EPerkState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Data class representing a perk with its current state and display information for UI rendering.
 * <p>
 * This class combines perk entity data with player-specific state information to provide
 * a complete view of how a perk should be displayed in the user interface. It includes
 * the perk definition, current state, cooldown information, and category classification.
 * </p>
 *
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
 */
public final class PerkDisplayData {
    
    private final RPerk perk;
    private final EPerkState state;
    private final EPerkCategory category;
    private final LocalDateTime cooldownExpiry;
    private final long remainingCooldownSeconds;
    private final boolean canToggle;
    private final boolean canActivate;
    
    /**
     * Constructs perk display data with all necessary information.
     *
     * @param perk                      the perk entity
     * @param state                     current state of the perk for the player
     * @param category                  category classification of the perk
     * @param cooldownExpiry            when the cooldown expires (null if no cooldown)
     * @param remainingCooldownSeconds  seconds remaining on cooldown (0 if no cooldown)
     * @param canToggle                 whether the player can toggle this perk
     * @param canActivate               whether the player can activate this perk
     */
    public PerkDisplayData(
        final @NotNull RPerk perk,
        final @NotNull EPerkState state,
        final @NotNull EPerkCategory category,
        final @Nullable LocalDateTime cooldownExpiry,
        final long remainingCooldownSeconds,
        final boolean canToggle,
        final boolean canActivate
    ) {
        this.perk = Objects.requireNonNull(perk, "Perk cannot be null");
        this.state = Objects.requireNonNull(state, "State cannot be null");
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.cooldownExpiry = cooldownExpiry;
        this.remainingCooldownSeconds = Math.max(0, remainingCooldownSeconds);
        this.canToggle = canToggle;
        this.canActivate = canActivate;
    }
    
    /**
     * Gets the perk entity.
     *
     * @return the perk entity
     */
    public @NotNull RPerk getPerk() {
        return this.perk;
    }
    
    /**
     * Gets the current state of the perk.
     *
     * @return the perk state
     */
    public @NotNull EPerkState getState() {
        return this.state;
    }
    
    /**
     * Gets the category of the perk.
     *
     * @return the perk category
     */
    public @NotNull EPerkCategory getCategory() {
        return this.category;
    }
    
    /**
     * Gets the cooldown expiry time.
     *
     * @return the cooldown expiry time, or null if no cooldown
     */
    public @Nullable LocalDateTime getCooldownExpiry() {
        return this.cooldownExpiry;
    }
    
    /**
     * Gets the remaining cooldown time in seconds.
     *
     * @return seconds remaining on cooldown, 0 if no cooldown
     */
    public long getRemainingCooldownSeconds() {
        return this.remainingCooldownSeconds;
    }
    
    /**
     * Checks if the player can toggle this perk.
     *
     * @return true if the perk can be toggled, false otherwise
     */
    public boolean canToggle() {
        return this.canToggle;
    }
    
    /**
     * Checks if the player can activate this perk.
     *
     * @return true if the perk can be activated, false otherwise
     */
    public boolean canActivate() {
        return this.canActivate;
    }
    
    /**
     * Checks if the perk is currently on cooldown.
     *
     * @return true if on cooldown, false otherwise
     */
    public boolean isOnCooldown() {
        return this.remainingCooldownSeconds > 0;
    }
    
    /**
     * Gets a formatted cooldown string for display.
     *
     * @return formatted cooldown string (e.g., "5m 30s"), empty if no cooldown
     */
    public @NotNull String getFormattedCooldown() {
        if (this.remainingCooldownSeconds <= 0) {
            return "";
        }
        
        final long minutes = this.remainingCooldownSeconds / 60;
        final long seconds = this.remainingCooldownSeconds % 60;
        
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Gets the perk identifier.
     *
     * @return the perk identifier
     */
    public @NotNull String getIdentifier() {
        return this.perk.getIdentifier();
    }
    
    /**
     * Gets the display name key for localization.
     *
     * @return the display name key
     */
    public @NotNull String getDisplayNameKey() {
        return this.perk.getDisplayNameKey();
    }
    
    /**
     * Gets the description key for localization.
     *
     * @return the description key
     */
    public @NotNull String getDescriptionKey() {
        return this.perk.getDescriptionKey();
    }
    
    /**
     * Checks if this perk matches the given category filter.
     *
     * @param categoryFilter the category to filter by, or null for no filter
     * @return true if the perk matches the filter, false otherwise
     */
    public boolean matchesCategory(final @Nullable EPerkCategory categoryFilter) {
        return categoryFilter == null || this.category == categoryFilter;
    }
    
    /**
     * Checks if this perk matches the given state filter.
     *
     * @param stateFilter the state to filter by, or null for no filter
     * @return true if the perk matches the filter, false otherwise
     */
    public boolean matchesState(final @Nullable EPerkState stateFilter) {
        return stateFilter == null || this.state == stateFilter;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final PerkDisplayData that = (PerkDisplayData) obj;
        return Objects.equals(this.perk.getIdentifier(), that.perk.getIdentifier());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(this.perk.getIdentifier());
    }
    
    @Override
    public String toString() {
        return "PerkDisplayData{" +
               "identifier='" + this.perk.getIdentifier() + '\'' +
               ", state=" + this.state +
               ", category=" + this.category +
               ", cooldown=" + this.remainingCooldownSeconds + "s" +
               '}';
    }
}