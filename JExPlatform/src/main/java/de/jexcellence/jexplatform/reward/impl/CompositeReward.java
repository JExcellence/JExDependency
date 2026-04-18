package de.jexcellence.jexplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Grants multiple rewards sequentially or in parallel.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class CompositeReward extends AbstractReward {

    @JsonProperty("children") private final List<AbstractReward> children;
    @JsonProperty("parallel") private final boolean parallel;
    @JsonProperty("continueOnError") private final boolean continueOnError;

    public CompositeReward(@JsonProperty("children") @NotNull List<AbstractReward> children,
                           @JsonProperty("parallel") boolean parallel,
                           @JsonProperty("continueOnError") boolean continueOnError) {
        super("COMPOSITE");
        this.children = List.copyOf(children);
        this.parallel = parallel;
        this.continueOnError = continueOnError;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        if (parallel) {
            var futures = children.stream()
                    .map(r -> r.grant(player))
                    .toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(futures)
                    .thenApply(v -> true);
        }

        var result = CompletableFuture.completedFuture(true);
        for (var child : children) {
            result = result.thenCompose(prev -> {
                if (!prev && !continueOnError) {
                    return CompletableFuture.completedFuture(false);
                }
                return child.grant(player);
            });
        }
        return result;
    }

    @Override public @NotNull String descriptionKey() { return "reward.composite"; }

    @Override
    public double estimatedValue() {
        return children.stream().mapToDouble(AbstractReward::estimatedValue).sum();
    }
}
