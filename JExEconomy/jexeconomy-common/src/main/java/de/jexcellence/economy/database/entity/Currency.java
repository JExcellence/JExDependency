package de.jexcellence.economy.database.entity;

import de.jexcellence.economy.api.CurrencySnapshot;
import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A server currency with display properties.
 *
 * @author JExcellence
 * @since 3.0.0
 */
@Entity
@Table(name = "economy_currency")
public class Currency extends LongIdEntity {

    @Column(name = "identifier", unique = true, nullable = false, length = 32)
    private String identifier;

    @Column(name = "symbol", nullable = false, length = 8)
    private String symbol;

    @Column(name = "prefix", length = 32)
    private String prefix;

    @Column(name = "suffix", length = 32)
    private String suffix;

    @Column(name = "icon", length = 64)
    private String icon;

    /** JPA constructor. */
    protected Currency() {
    }

    /**
     * Creates a currency with all properties.
     *
     * @param identifier unique programmatic name
     * @param symbol     short display symbol (e.g. "$")
     * @param prefix     text before amounts (nullable)
     * @param suffix     text after amounts (nullable)
     * @param icon       material name for GUI display (nullable, defaults to GOLD_INGOT)
     */
    public Currency(@NotNull String identifier,
                    @NotNull String symbol,
                    @Nullable String prefix,
                    @Nullable String suffix,
                    @Nullable String icon) {
        this.identifier = identifier;
        this.symbol = symbol;
        this.prefix = prefix != null ? prefix : "";
        this.suffix = suffix != null ? suffix : "";
        this.icon = icon != null ? icon : "GOLD_INGOT";
    }

    /**
     * Creates a currency with just an identifier and sensible defaults.
     *
     * @param identifier unique programmatic name
     */
    public Currency(@NotNull String identifier) {
        this(identifier, "", "", "", "GOLD_INGOT");
    }

    // ── Accessors ───────────────────────────────────────────────────────────────

    /**
     * Returns the unique identifier.
     *
     * @return the identifier
     */
    public @NotNull String getIdentifier() {
        return identifier;
    }

    /**
     * Sets the identifier.
     *
     * @param identifier the identifier
     */
    public void setIdentifier(@NotNull String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns the display symbol.
     *
     * @return the symbol
     */
    public @NotNull String getSymbol() {
        return symbol;
    }

    /**
     * Sets the display symbol.
     *
     * @param symbol the symbol
     */
    public void setSymbol(@NotNull String symbol) {
        this.symbol = symbol;
    }

    /**
     * Returns the amount prefix.
     *
     * @return the prefix
     */
    public @NotNull String getPrefix() {
        return prefix;
    }

    /**
     * Sets the amount prefix.
     *
     * @param prefix the prefix
     */
    public void setPrefix(@NotNull String prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns the amount suffix.
     *
     * @return the suffix
     */
    public @NotNull String getSuffix() {
        return suffix;
    }

    /**
     * Sets the amount suffix.
     *
     * @param suffix the suffix
     */
    public void setSuffix(@NotNull String suffix) {
        this.suffix = suffix;
    }

    /**
     * Returns the Material name used as GUI icon.
     *
     * @return the icon material name
     */
    public @NotNull String getIcon() {
        return icon;
    }

    /**
     * Sets the icon material name.
     *
     * @param icon the material name
     */
    public void setIcon(@NotNull String icon) {
        this.icon = icon;
    }

    /**
     * Creates an API-safe snapshot of this currency.
     *
     * <p>For unpersisted currencies (e.g. pre-create event payloads) the id
     * falls back to {@code 0L} since the DB hasn't assigned one yet —
     * callers should treat {@code id <= 0} as "not persisted".
     *
     * @return a lightweight, immutable snapshot
     */
    public @NotNull CurrencySnapshot toSnapshot() {
        var id = getId();
        return new CurrencySnapshot(
                id != null ? id : 0L,
                identifier,
                symbol,
                prefix != null ? prefix : "",
                suffix != null ? suffix : "",
                icon != null ? icon : "GOLD_INGOT");
    }

    /**
     * Formats an amount using this currency's prefix, symbol, and suffix.
     *
     * @param amount the amount to format
     * @return the formatted string
     */
    public @NotNull String format(double amount) {
        var formatted = String.format("%.2f", amount);
        return prefix + formatted + symbol + suffix;
    }

    @Override
    public String toString() {
        return "Currency[" + identifier + "]";
    }
}
