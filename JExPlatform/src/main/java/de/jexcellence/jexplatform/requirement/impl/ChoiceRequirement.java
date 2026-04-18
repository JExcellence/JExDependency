package de.jexcellence.jexplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Requires the player to meet N choices from a list of options.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class ChoiceRequirement extends AbstractRequirement {

    @JsonProperty("choices")
    private final List<AbstractRequirement> choices;

    @JsonProperty("requiredCount")
    private final int requiredCount;

    @JsonProperty("mutuallyExclusive")
    private final boolean mutuallyExclusive;

    /**
     * Creates a choice requirement.
     *
     * @param choices           the available options
     * @param requiredCount     how many must be met
     * @param mutuallyExclusive whether choices exclude each other
     */
    public ChoiceRequirement(
            @JsonProperty("choices") @NotNull List<AbstractRequirement> choices,
            @JsonProperty("requiredCount") int requiredCount,
            @JsonProperty("mutuallyExclusive") boolean mutuallyExclusive) {
        super("CHOICE");
        this.choices = List.copyOf(choices);
        this.requiredCount = Math.max(1, requiredCount);
        this.mutuallyExclusive = mutuallyExclusive;
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        var met = choices.stream().filter(r -> r.isMet(player)).count();
        return met >= requiredCount;
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        if (requiredCount <= 0) {
            return 1.0;
        }
        var met = choices.stream().filter(r -> r.isMet(player)).count();
        return Math.min(1.0, (double) met / requiredCount);
    }

    @Override
    public void consume(@NotNull Player player) {
        var consumed = 0;
        for (var choice : choices) {
            if (consumed >= requiredCount) {
                break;
            }
            if (choice.isMet(player)) {
                choice.consume(player);
                consumed++;
            }
        }
    }

    @Override
    public @NotNull String descriptionKey() {
        return "requirement.choice";
    }
}
