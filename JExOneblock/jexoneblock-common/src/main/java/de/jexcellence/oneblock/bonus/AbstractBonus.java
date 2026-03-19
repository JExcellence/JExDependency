package de.jexcellence.oneblock.bonus;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for all evolution bonuses.
 * Provides common functionality for bonus implementations.
 * 
 * @author JExcellence
 * @since 2.0.0
 */
public abstract class AbstractBonus implements Bonus {
    protected final Type type;
    protected final double value;
    protected final boolean active;
    protected final long duration;

    protected AbstractBonus(@NotNull Type type, double value) {
        this(type, value, true, -1L);
    }

    protected AbstractBonus(@NotNull Type type, double value, boolean active, long duration) {
        this.type = type;
        this.value = value;
        this.active = active;
        this.duration = duration;
    }

    @Override
    public @NotNull Type getType() {
        return type;
    }

    @Override
    public double getValue() {
        return value;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public @NotNull String getFormattedDescription() {
        if (value == 0) return "No bonus";
        
        String valueStr;
        if (type == Type.SPEED || type == Type.DAMAGE || type == Type.DEFENSE) {
            // These are usually additive bonuses
            valueStr = String.format("+%.1f", value);
        } else {
            // Most bonuses are percentage-based
            valueStr = String.format("+%.0f%%", value * 100);
        }
        
        return String.format("%s %s", type.getDisplayName(), valueStr);
    }

    @Override
    public String toString() {
        return String.format("%s{type=%s, value=%.2f, active=%s, duration=%d}", 
            getClass().getSimpleName(), type, value, active, duration);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AbstractBonus that = (AbstractBonus) obj;
        return Double.compare(that.value, value) == 0 &&
               active == that.active &&
               duration == that.duration &&
               type == that.type;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, value, active, duration);
    }
}