package de.jexcellence.jexplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Lets the player choose from multiple reward options.
 *
 * <p>The reward holds a list of options and the number of selections
 * the player can make. If {@code mutuallyExclusive} is {@code true},
 * choosing one option prevents the others from being granted.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class ChoiceReward extends AbstractReward {

    @JsonProperty("options") private final List<AbstractReward> options;
    @JsonProperty("maxSelections") private final int maxSelections;
    @JsonProperty("mutuallyExclusive") private final boolean mutuallyExclusive;

    /**
     * Creates a choice reward.
     *
     * @param options           the available reward options
     * @param maxSelections     the number of choices the player can make
     * @param mutuallyExclusive whether options are mutually exclusive
     */
    public ChoiceReward(@JsonProperty("options") @NotNull List<AbstractReward> options,
                        @JsonProperty("maxSelections") int maxSelections,
                        @JsonProperty("mutuallyExclusive") boolean mutuallyExclusive) {
        super("CHOICE");
        this.options = List.copyOf(options);
        this.maxSelections = Math.max(1, maxSelections);
        this.mutuallyExclusive = mutuallyExclusive;
    }

    /**
     * Grants the first {@code maxSelections} rewards from the options.
     *
     * <p>In a full GUI implementation, the player would select their choices.
     * This default implementation grants sequentially from the options list.
     *
     * @param player the player
     * @return a future resolving to {@code true} on success
     */
    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        var toGrant = options.stream()
                .limit(maxSelections)
                .toList();

        var result = CompletableFuture.completedFuture(true);
        for (var reward : toGrant) {
            result = result.thenCompose(prev -> {
                if (!prev && mutuallyExclusive) {
                    return CompletableFuture.completedFuture(false);
                }
                return reward.grant(player);
            });
        }
        return result;
    }

    @Override
    public @NotNull String descriptionKey() {
        return "reward.choice";
    }

    @Override
    public double estimatedValue() {
        return options.stream()
                .mapToDouble(AbstractReward::estimatedValue)
                .sorted()
                .limit(maxSelections)
                .sum();
    }
}
