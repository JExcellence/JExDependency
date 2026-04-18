package de.jexcellence.economy.api;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Type of balance change operation.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public enum ChangeType {

    /** Money added to account. */
    DEPOSIT("deposit"),

    /** Money removed from account. */
    WITHDRAW("withdraw"),

    /** Balance set to specific amount. */
    SET("set"),

    /** Outgoing transfer to another player. */
    TRANSFER_OUT("transfer_out"),

    /** Incoming transfer from another player. */
    TRANSFER_IN("transfer_in");

    private static final Map<String, ChangeType> BY_ID = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(ChangeType::identifier, t -> t));

    private final String identifier;

    ChangeType(@NotNull String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns the lowercase identifier.
     *
     * @return the identifier
     */
    public @NotNull String identifier() {
        return identifier;
    }

    /**
     * Resolves a change type by identifier (case-insensitive).
     *
     * @param identifier the identifier to match
     * @return the matching type, or empty
     */
    public static @NotNull Optional<ChangeType> fromIdentifier(@NotNull String identifier) {
        return Optional.ofNullable(BY_ID.get(identifier.toLowerCase(Locale.ROOT)));
    }
}
