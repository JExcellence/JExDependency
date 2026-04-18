package de.jexcellence.jexplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Aggregates multiple requirements with AND, OR, or MINIMUM semantics.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class CompositeRequirement extends AbstractRequirement {

    /** How child requirements are evaluated. */
    public enum CompositeMode { AND, OR, MINIMUM }

    @JsonProperty("children")
    private final List<AbstractRequirement> children;

    @JsonProperty("mode")
    private final CompositeMode mode;

    @JsonProperty("minimum")
    private final int minimum;

    /**
     * Creates a composite requirement.
     *
     * @param children the child requirements
     * @param mode     the evaluation mode
     * @param minimum  the minimum count for {@link CompositeMode#MINIMUM}
     */
    public CompositeRequirement(@JsonProperty("children") @NotNull List<AbstractRequirement> children,
                                @JsonProperty("mode") CompositeMode mode,
                                @JsonProperty("minimum") int minimum) {
        super("COMPOSITE");
        this.children = List.copyOf(children);
        this.mode = mode != null ? mode : CompositeMode.AND;
        this.minimum = Math.max(0, minimum);
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        return switch (mode) {
            case AND -> children.stream().allMatch(r -> r.isMet(player));
            case OR -> children.stream().anyMatch(r -> r.isMet(player));
            case MINIMUM -> children.stream().filter(r -> r.isMet(player)).count() >= minimum;
        };
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        if (children.isEmpty()) {
            return 1.0;
        }
        return children.stream()
                .mapToDouble(r -> r.calculateProgress(player))
                .average()
                .orElse(0.0);
    }

    @Override
    public void consume(@NotNull Player player) {
        children.forEach(r -> r.consume(player));
    }

    @Override
    public @NotNull String descriptionKey() {
        return "requirement.composite";
    }
}
