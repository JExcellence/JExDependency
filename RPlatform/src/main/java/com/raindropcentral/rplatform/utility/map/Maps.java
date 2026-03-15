package com.raindropcentral.rplatform.utility.map;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the Maps API type.
 */
public final class Maps {

    private Maps() {}

    /**
     * Executes merge.
     */
    public static <K,V> Builder<K,V> merge(Map<K,V> base) {
        return new Builder<>(base);
    }

    /**
     * Executes merge.
     */
    public static <K,V> Builder<K,V> merge(Object base) {
        return new Builder<>(base);
    }

    /**
     * Represents the Builder API type.
     */
    public static final class Builder<K,V> {

        private final Map<K,V> data;

        private Builder(Map<K,V> base) {
            this.data = new HashMap<>();
            if (base != null) data.putAll(base);
        }

        private Builder(Object base) {
            this((Map<K, V>) base);
        }

        /**
         * Executes with.
         */
        public Builder<K,V> with(K key, V value) {
            data.put(key, value);
            return this;
        }

        /**
         * Executes with.
         */
        public Builder<K,V> with(Map<? extends K, ? extends V> map) {
            if (map != null) data.putAll(map);
            return this;
        }

        /**
         * Executes remove.
         */
        public Builder<K,V> remove(K key) {
            data.remove(key);
            return this;
        }

        /**
         * Executes onlyIf.
         */
        public Builder<K,V> onlyIf(boolean condition, K key, V value) {
            if (condition) data.put(key, value);
            return this;
        }

        /**
         * Executes onlyIf.
         */
        public Builder<K,V> onlyIf(boolean condition, Map<? extends K, ? extends V> map) {
            if (condition && map != null) data.putAll(map);
            return this;
        }

        /**
         * Executes immutable.
         */
        public Map<K,V> immutable() {
            return Collections.unmodifiableMap(new HashMap<>(data));
        }

        /**
         * Executes mutable.
         */
        public Map<K,V> mutable() {
            return data;
        }
    }
}
