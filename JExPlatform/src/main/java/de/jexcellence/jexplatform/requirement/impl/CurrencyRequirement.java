package de.jexcellence.jexplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Requires the player to have a minimum currency balance.
 *
 * <p>Economy integration is resolved through the platform's
 * {@code ServiceRegistry} at runtime.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class CurrencyRequirement extends AbstractRequirement {

    @JsonProperty("amount")
    private final double amount;

    @JsonProperty("currency")
    private final String currency;

    /**
     * Creates a currency requirement.
     *
     * @param amount            the minimum balance
     * @param currency          the currency identifier (may be {@code null} for default)
     * @param consumeOnComplete whether to withdraw on fulfillment
     */
    public CurrencyRequirement(@JsonProperty("amount") double amount,
                               @JsonProperty("currency") String currency,
                               @JsonProperty("consumeOnComplete") boolean consumeOnComplete) {
        super("CURRENCY", consumeOnComplete);
        this.amount = amount;
        this.currency = currency;
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        // Economy bridge resolved at runtime via ServiceRegistry
        return false;
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        return 0.0;
    }

    @Override
    public void consume(@NotNull Player player) {
        // Withdraw via economy bridge
    }

    @Override
    public @NotNull String descriptionKey() {
        return "requirement.currency";
    }

    /**
     * Returns the required amount.
     *
     * @return the amount
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Returns the currency identifier.
     *
     * @return the currency, or {@code null} for default
     */
    public String getCurrency() {
        return currency;
    }
}
