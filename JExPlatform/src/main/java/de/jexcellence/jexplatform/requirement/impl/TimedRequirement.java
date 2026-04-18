package de.jexcellence.jexplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Requires a wrapped requirement to be met within a time window.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class TimedRequirement extends AbstractRequirement {

    @JsonProperty("delegate")
    private final AbstractRequirement delegate;

    @JsonProperty("startTimeEpoch")
    private final long startTimeEpoch;

    @JsonProperty("endTimeEpoch")
    private final long endTimeEpoch;

    /**
     * Creates a timed requirement.
     *
     * @param delegate       the wrapped requirement
     * @param startTimeEpoch the window start (epoch millis, 0 for no start)
     * @param endTimeEpoch   the window end (epoch millis, 0 for no end)
     */
    public TimedRequirement(@JsonProperty("delegate") @NotNull AbstractRequirement delegate,
                            @JsonProperty("startTimeEpoch") long startTimeEpoch,
                            @JsonProperty("endTimeEpoch") long endTimeEpoch) {
        super("TIME_BASED");
        this.delegate = delegate;
        this.startTimeEpoch = startTimeEpoch;
        this.endTimeEpoch = endTimeEpoch;
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        if (!isWithinWindow()) {
            return false;
        }
        return delegate.isMet(player);
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        if (!isWithinWindow()) {
            return 0.0;
        }
        return delegate.calculateProgress(player);
    }

    @Override
    public void consume(@NotNull Player player) {
        if (isWithinWindow()) {
            delegate.consume(player);
        }
    }

    @Override
    public @NotNull String descriptionKey() {
        return "requirement.timed";
    }

    private boolean isWithinWindow() {
        var now = System.currentTimeMillis();
        if (startTimeEpoch > 0 && now < startTimeEpoch) {
            return false;
        }
        return endTimeEpoch <= 0 || now <= endTimeEpoch;
    }
}
