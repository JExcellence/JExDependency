package de.jexcellence.oneblock.bonus;

import org.jetbrains.annotations.NotNull;

/**
 * Experience bonus - increases experience gain.
 * 
 * @author JExcellence
 * @since 2.0.0
 */
public final class ExperienceBonus extends AbstractBonus {
    
    public ExperienceBonus(double multiplier) {
        super(Type.EXPERIENCE, multiplier);
    }

    public ExperienceBonus(double multiplier, boolean active, long duration) {
        super(Type.EXPERIENCE, multiplier, active, duration);
    }

    /**
     * Gets the experience multiplier.
     * @return multiplier (1.0 = normal, 1.25 = 25% increase)
     */
    public double getMultiplier() {
        return value;
    }

    @Override
    public @NotNull String getFormattedDescription() {
        if (value <= 1.0) return "No experience bonus";
        return String.format("Experience +%.0f%%", (value - 1.0) * 100);
    }
}