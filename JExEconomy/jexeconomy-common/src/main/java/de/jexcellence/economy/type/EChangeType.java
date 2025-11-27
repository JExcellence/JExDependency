package de.jexcellence.economy.type;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enum representing the type of balance change.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public enum EChangeType {

    /**
     * Money is being added to the account.
     */
    DEPOSIT("deposit", "Money is being added to the account."),

    /**
     * Money is being removed from the account.
     */
    WITHDRAW("withdraw", "Money is being removed from the account."),

    /**
     * The balance is being set to a specific amount.
     */
    SET("set", "The balance is being set to a specific amount."),

    /**
     * The balance is being transferred to another player.
     */
    TRANSFER_OUT("transfer_out", "The balance is being transferred to another player."),

    /**
     * The balance is being received from another player.
     */
    TRANSFER_IN("transfer_in", "The balance is being received from another player.");

    private static final Map<String, EChangeType> IDENTIFIER_LOOKUP = Collections.unmodifiableMap(
            Arrays.stream(values())
                    .collect(Collectors.toMap(type -> type.identifier, Function.identity()))
    );

    private final String identifier;
    private final String description;

    EChangeType(final @NotNull String identifier, final @NotNull String description) {
        this.identifier = identifier.toLowerCase(Locale.ROOT);
        this.description = description;
    }

    /**
     * Provides the lower-case identifier associated with this change type.
     *
     * @return the identifier string for this change type
     */
    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    /**
     * Provides a human readable description of this change type.
     *
     * @return the description for this change type
     */
    public @NotNull String getDescription() {
        return this.description;
    }

    /**
     * Attempts to resolve a change type from its identifier, ignoring character case.
     *
     * @param identifier the identifier to match
     * @return an optional containing the matching change type when present, or empty otherwise
     */
    public static @NotNull Optional<EChangeType> findByIdentifier(final @NotNull String identifier) {
        Objects.requireNonNull(identifier, "identifier");
        return Optional.ofNullable(IDENTIFIER_LOOKUP.get(identifier.toLowerCase(Locale.ROOT)));
    }
}
