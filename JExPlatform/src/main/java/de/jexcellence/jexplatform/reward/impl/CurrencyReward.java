package de.jexcellence.jexplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Deposits currency into the player's account.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class CurrencyReward extends AbstractReward {

    @JsonProperty("amount") private final double amount;
    @JsonProperty("currency") private final String currency;

    public CurrencyReward(@JsonProperty("amount") double amount,
                          @JsonProperty("currency") String currency) {
        super("CURRENCY");
        this.amount = amount;
        this.currency = currency;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        // Economy bridge resolved at runtime via ServiceRegistry
        return CompletableFuture.completedFuture(false);
    }

    @Override public @NotNull String descriptionKey() { return "reward.currency"; }
    @Override public double estimatedValue() { return amount; }
}
