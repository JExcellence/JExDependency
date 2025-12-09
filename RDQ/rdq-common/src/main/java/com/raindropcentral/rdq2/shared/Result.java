package com.raindropcentral.rdq2.shared;

import com.raindropcentral.rdq2.shared.error.RDQError;
import com.raindropcentral.rdq2.shared.error.RDQException;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A sealed result type representing either success or failure.
 *
 * <p>This type provides a functional approach to error handling without exceptions.
 * Use pattern matching to handle both cases:
 *
 * <pre>{@code
 * Result<Player> result = service.getPlayer(id);
 * switch (result) {
 *     case Success<Player>(var player) -> handlePlayer(player);
 *     case Failure<Player>(var key, var ph) -> handleError(key, ph);
 * }
 * }</pre>
 *
 * @param <T> the type of the success value
 * @see Success
 * @see Failure
 */
public sealed interface Result<T> {

    record Success<T>(@NotNull T value) implements Result<T> {
        public Success {
            Objects.requireNonNull(value, "value");
        }
    }

    record Failure<T>(
        @NotNull String errorKey,
        @NotNull Map<String, Object> placeholders
    ) implements Result<T> {
        public Failure {
            Objects.requireNonNull(errorKey, "errorKey");
            placeholders = placeholders != null ? Map.copyOf(placeholders) : Map.of();
        }

        public Failure(@NotNull String errorKey) {
            this(errorKey, Map.of());
        }
    }

    default T getOrThrow() {
        return switch (this) {
            case Success<T>(var v) -> v;
            case Failure<T>(var key, var ph) -> throw new RDQException(new RDQError.InvalidState(key));
        };
    }

    default T getOrElse(@NotNull T defaultValue) {
        return switch (this) {
            case Success<T>(var v) -> v;
            case Failure<T> f -> defaultValue;
        };
    }

    default <R> Result<R> map(@NotNull Function<T, R> mapper) {
        return switch (this) {
            case Success<T>(var v) -> new Success<>(mapper.apply(v));
            case Failure<T>(var key, var ph) -> new Failure<>(key, ph);
        };
    }

    default <R> Result<R> flatMap(@NotNull Function<T, Result<R>> mapper) {
        return switch (this) {
            case Success<T>(var v) -> mapper.apply(v);
            case Failure<T>(var key, var ph) -> new Failure<>(key, ph);
        };
    }

    default Result<T> onSuccess(@NotNull Consumer<T> consumer) {
        if (this instanceof Success<T>(var v)) {
            consumer.accept(v);
        }
        return this;
    }

    default Result<T> onFailure(@NotNull Consumer<Failure<T>> consumer) {
        if (this instanceof Failure<T> f) {
            consumer.accept(f);
        }
        return this;
    }

    default boolean isSuccess() {
        return this instanceof Success<T>;
    }

    default boolean isFailure() {
        return this instanceof Failure<T>;
    }

    static <T> Result<T> success(@NotNull T value) {
        return new Success<>(value);
    }

    static <T> Result<T> failure(@NotNull String errorKey) {
        return new Failure<>(errorKey);
    }

    static <T> Result<T> failure(@NotNull String errorKey, @NotNull Map<String, Object> placeholders) {
        return new Failure<>(errorKey, placeholders);
    }
}
