package com.raindropcentral.core.database.entity.statistic;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * String-based statistic stored via the {@code STRING} discriminator. Useful for textual
 * metadata such as titles or serialized JSON blobs.
 * <p>
 * Callers should log via {@link com.raindropcentral.rplatform.logging.CentralLogger CentralLogger}
 * when textual payloads are created, truncated, or replaced. Debug logs capture cache misses prior
 * to hydrate large blobs, while info logs summarize high-level changes (identifier, plugin, length)
 * before persistence. Validation errors (null assignments or oversize payloads detected upstream)
 * should escalate to error severity to highlight serialization issues.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@DiscriminatorValue("STRING")
public class RStringStatistic extends RAbstractStatistic {

    /**
     * Column mapping for textual {@code statistic_value}. Stored as {@code TEXT} to support
     * long-form payloads without truncation.
     */
    @Column(name = "statistic_value", columnDefinition = "TEXT")
    private String value;

    /**
     * Hibernate constructor for reflective instantiation.
     */
    protected RStringStatistic() {}

    /**
     * Creates a string statistic instance.
     *
     * @param identifier statistic identifier unique within the aggregate
     * @param plugin     plugin namespace that manages the statistic
     * @param value      textual payload to persist
     */
    public RStringStatistic(
        final @NotNull String identifier,
        final @NotNull String plugin,
        final @NotNull String value
    ) {
        super(identifier, plugin);
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    /**
     * @return persisted string payload
     */
    @Override
    public @NotNull String getValue() {
        return this.value;
    }

    /**
     * Updates the textual payload.
     *
     * @param value new string value to persist
     */
    public void setValue(final @NotNull String value) {
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    /**
     * Determines whether the stored value contains any characters. This delegates directly to
     * {@link String#isEmpty()} so the call remains O(1) without additional allocations.
     *
     * @return {@code true} when the stored string is empty
     */
    public boolean isEmpty() {
        return this.value.isEmpty();
    }

    /**
     * Provides the current length of the persisted string. The value is retrieved via
     * {@link String#length()} which simply returns the cached character count maintained by the
     * JVM, ensuring constant-time access.
     *
     * @return length of the persisted string value
     */
    public int length() {
        return this.value.length();
    }

    /**
     * Generates a descriptive view of the statistic while avoiding large string allocations in
     * logs or debugger output. Values longer than fifty characters are truncated to forty-seven
     * characters with an ellipsis suffix so callers can quickly inspect context without forcing a
     * full copy of the payload.
     *
     * @return formatted representation with truncated value preview when necessary
     */
    @Override
    public String toString() {
        final String displayValue = value.length() > 50
            ? value.substring(0, 47) + "..."
            : value;
        return "RStringStatistic[id=%d, identifier=%s, plugin=%s, value=%s]"
            .formatted(getId(), identifier, getPlugin(), displayValue);
    }
}
