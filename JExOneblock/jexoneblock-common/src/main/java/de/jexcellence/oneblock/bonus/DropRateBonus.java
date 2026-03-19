package de.jexcellence.oneblock.bonus;

import org.jetbrains.annotations.NotNull;

/**
 * Drop rate bonus - increases item drop rates.
 * 
 * @author JExcellence
 * @since 2.0.0
 */
public final class DropRateBonus extends AbstractBonus {
    
    public DropRateBonus(double multiplier) {
        super(Type.DROP_RATE, multiplier);
    }

    public DropRateBonus(double multiplier, boolean active, long duration) {
        super(Type.DROP_RATE, multiplier, active, duration);
    }

    /**
     * Gets the drop rate multiplier.
     * @return multiplier (1.0 = normal, 1.5 = 50% increase)
     */
    public double getMultiplier() {
        return value;
    }

    @Override
    public @NotNull String getFormattedDescription() {
        if (value <= 1.0) return "No drop rate bonus";
        return String.format("Drop Rate +%.0f%%", (value - 1.0) * 100);
    }
}