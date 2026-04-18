package de.jexcellence.jexplatform.utility;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Fluent builder for constructing immutable or mutable maps through chained operations.
 *
 * <pre>{@code
 * var data = Maps.<String, Object>merge(existing)
 *     .with("key", value)
 *     .onlyIf(condition, "optional", optVal)
 *     .remove("unwanted")
 *     .immutable();
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class Maps {

    private Maps() {
    }

    /**
     * Starts a builder seeded with the entries from the provided map.
     *
     * @param <K>  the key type
     * @param <V>  the value type
     * @param base the seed map (may be {@code null})
     * @return a new builder pre-populated with the base entries
     */
    public static <K, V> @NotNull Builder<K, V> merge(@Nullable Map<K, V> base) {
        return new Builder<>(base);
    }

    /**
     * Starts a builder seeded with the entries from an unchecked map reference.
     *
     * @param <K>  the key type
     * @param <V>  the value type
     * @param base the seed object cast to {@code Map<K, V>}
     * @return a new builder pre-populated with the base entries
     */
    @SuppressWarnings("unchecked")
    public static <K, V> @NotNull Builder<K, V> merge(@Nullable Object base) {
        return new Builder<>((Map<K, V>) base);
    }

    /**
     * Fluent map construction helper with conditional insertion and merge support.
     *
     * @param <K> the key type
     * @param <V> the value type
     */
    public static final class Builder<K, V> {

        private final Map<K, V> data;

        private Builder(@Nullable Map<K, V> base) {
            this.data = new HashMap<>();
            if (base != null) {
                data.putAll(base);
            }
        }

        /**
         * Inserts a single key-value pair.
         *
         * @param key   the map key
         * @param value the map value
         * @return this builder
         */
        public @NotNull Builder<K, V> with(@NotNull K key, @NotNull V value) {
            data.put(key, value);
            return this;
        }

        /**
         * Merges all entries from the provided map.
         *
         * @param map the entries to merge (may be {@code null})
         * @return this builder
         */
        public @NotNull Builder<K, V> with(@Nullable Map<? extends K, ? extends V> map) {
            if (map != null) {
                data.putAll(map);
            }
            return this;
        }

        /**
         * Removes a key from the builder.
         *
         * @param key the key to remove
         * @return this builder
         */
        public @NotNull Builder<K, V> remove(@NotNull K key) {
            data.remove(key);
            return this;
        }

        /**
         * Conditionally inserts a key-value pair when the condition is {@code true}.
         *
         * @param condition whether to insert the entry
         * @param key       the map key
         * @param value     the map value
         * @return this builder
         */
        public @NotNull Builder<K, V> onlyIf(boolean condition, @NotNull K key, @NotNull V value) {
            if (condition) {
                data.put(key, value);
            }
            return this;
        }

        /**
         * Conditionally merges all entries when the condition is {@code true}.
         *
         * @param condition whether to merge the entries
         * @param map       the entries to merge (may be {@code null})
         * @return this builder
         */
        public @NotNull Builder<K, V> onlyIf(boolean condition,
                                              @Nullable Map<? extends K, ? extends V> map) {
            if (condition && map != null) {
                data.putAll(map);
            }
            return this;
        }

        /**
         * Returns an unmodifiable view of the accumulated entries.
         *
         * @return immutable map snapshot
         */
        public @NotNull Map<K, V> immutable() {
            return Collections.unmodifiableMap(new HashMap<>(data));
        }

        /**
         * Returns the mutable backing map.
         *
         * @return mutable map reference
         */
        public @NotNull Map<K, V> mutable() {
            return data;
        }
    }
}
