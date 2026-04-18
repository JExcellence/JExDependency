package de.jexcellence.jexplatform.reward;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for reward types. Not a singleton — create one per platform instance.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class RewardRegistry {

    private final Map<String, RewardType> types = new ConcurrentHashMap<>();
    private final JExLogger logger;

    /**
     * Creates a new reward registry.
     *
     * @param logger the platform logger
     */
    public RewardRegistry(@NotNull JExLogger logger) {
        this.logger = logger;
    }

    /** Registers a reward type. */
    public void register(@NotNull RewardType type) {
        types.put(type.id().toUpperCase(), type);
        logger.debug("Registered reward type: {}", type.qualifiedName());
    }

    /** Unregisters a reward type. */
    public void unregister(@NotNull String typeName) {
        types.remove(typeName.toUpperCase());
    }

    /** Finds a reward type by name. */
    public @NotNull Optional<RewardType> find(@NotNull String typeName) {
        return Optional.ofNullable(types.get(typeName.toUpperCase()));
    }

    /** Returns all registered types. */
    public @NotNull Map<String, RewardType> types() {
        return Map.copyOf(types);
    }

    /** Returns the total number of registered types. */
    public int size() {
        return types.size();
    }
}
