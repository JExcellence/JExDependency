package de.jexcellence.economy.command.player.pay;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Command section configuration for the player pay command.
 * <p>
 * This section provides configuration for the "/pay" command including
 * settings for which currencies can be paid and whether the command is enabled.
 * </p>
 *
 * <h3>Configuration Options:</h3>
 * <ul>
 *   <li><strong>enabled:</strong> Whether the pay command is enabled</li>
 *   <li><strong>allowed-currencies:</strong> List of currency identifiers that can be paid</li>
 *   <li><strong>min-amount:</strong> Minimum amount that can be paid</li>
 *   <li><strong>max-amount:</strong> Maximum amount that can be paid (0 = unlimited)</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 2.0.0
 * @see ACommandSection
 * @see PPay
 */
public class PPaySection extends ACommandSection {

    private static final String PAY_COMMAND_NAME = "pay";

    /**
     * Whether the pay command is enabled.
     */
    private boolean enabled = true;

    /**
     * List of currency identifiers that are allowed to be paid.
     * Empty list means all currencies are allowed.
     */
    private List<String> allowedCurrencies = List.of();

    /**
     * Minimum amount that can be paid in a single transaction.
     */
    private double minAmount = 0.01;

    /**
     * Maximum amount that can be paid in a single transaction.
     * 0 means unlimited.
     */
    private double maxAmount = 0;

    /**
     * Constructs a new pay command section with the specified evaluation environment.
     *
     * @param evaluationEnvironmentBuilder the builder for the evaluation environment, must not be null
     */
    public PPaySection(final @NotNull EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
        super(PAY_COMMAND_NAME, evaluationEnvironmentBuilder);
    }

    /**
     * Checks if the pay command is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Sets whether the pay command is enabled.
     *
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the list of allowed currency identifiers.
     *
     * @return list of allowed currency identifiers, empty means all allowed
     */
    public @NotNull List<String> getAllowedCurrencies() {
        return this.allowedCurrencies;
    }

    /**
     * Sets the list of allowed currency identifiers.
     *
     * @param allowedCurrencies list of currency identifiers to allow
     */
    public void setAllowedCurrencies(final @NotNull List<String> allowedCurrencies) {
        this.allowedCurrencies = allowedCurrencies;
    }

    /**
     * Checks if a currency is allowed for pay transactions.
     *
     * @param currencyIdentifier the currency identifier to check
     * @return true if the currency is allowed, false otherwise
     */
    public boolean isCurrencyAllowed(final @NotNull String currencyIdentifier) {
        if (this.allowedCurrencies.isEmpty()) {
            return true;
        }
        return this.allowedCurrencies.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(currencyIdentifier));
    }

    /**
     * Gets the minimum amount that can be paid.
     *
     * @return the minimum amount
     */
    public double getMinAmount() {
        return this.minAmount;
    }

    /**
     * Sets the minimum amount that can be paid.
     *
     * @param minAmount the minimum amount
     */
    public void setMinAmount(final double minAmount) {
        this.minAmount = minAmount;
    }

    /**
     * Gets the maximum amount that can be paid.
     *
     * @return the maximum amount, 0 means unlimited
     */
    public double getMaxAmount() {
        return this.maxAmount;
    }

    /**
     * Sets the maximum amount that can be paid.
     *
     * @param maxAmount the maximum amount, 0 means unlimited
     */
    public void setMaxAmount(final double maxAmount) {
        this.maxAmount = maxAmount;
    }

    /**
     * Validates if an amount is within the configured limits.
     *
     * @param amount the amount to validate
     * @return true if the amount is valid, false otherwise
     */
    public boolean isAmountValid(final double amount) {
        if (amount < this.minAmount) {
            return false;
        }
        if (this.maxAmount > 0 && amount > this.maxAmount) {
            return false;
        }
        return true;
    }
}
