package de.jexcellence.quests.machine;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory registry of {@link MachineType} definitions. Thread-safe;
 * {@link #replace(Map)} atomically swaps the entire set during reload.
 */
public final class MachineRegistry {

    private final JExLogger logger;
    private final ConcurrentMap<String, MachineType> types = new ConcurrentHashMap<>();

    public MachineRegistry(@NotNull JExLogger logger) {
        this.logger = logger;
    }

    /** Registered identifiers (immutable snapshot). */
    public @NotNull Collection<String> identifiers() {
        return Map.copyOf(this.types).keySet();
    }

    /** Every type in registration order (no defined order — caller sorts). */
    public @NotNull Collection<MachineType> all() {
        return Map.copyOf(this.types).values();
    }

    public @NotNull Optional<MachineType> get(@NotNull String identifier) {
        return Optional.ofNullable(this.types.get(identifier));
    }

    /** Replace the entire registry atomically with a fresh map. */
    public void replace(@NotNull Map<String, MachineType> incoming) {
        this.types.clear();
        this.types.putAll(incoming);
        this.logger.info("Machine registry loaded {} types", this.types.size());
    }

    public int size() {
        return this.types.size();
    }
}
