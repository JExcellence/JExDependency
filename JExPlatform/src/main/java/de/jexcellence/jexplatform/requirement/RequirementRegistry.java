package de.jexcellence.jexplatform.requirement;

import de.jexcellence.jexplatform.requirement.plugin.PluginRequirementProvider;
import de.jexcellence.jexplatform.logging.JExLogger;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for requirement types with dynamic type registration.
 *
 * <p>Not a singleton — create one per platform instance and register
 * it via {@code ServiceRegistry}.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class RequirementRegistry {

    private final Map<String, RequirementType> types = new ConcurrentHashMap<>();
    private final Map<String, PluginRequirementProvider> providers = new ConcurrentHashMap<>();
    private final JExLogger logger;

    /**
     * Creates a new requirement registry.
     *
     * @param logger the platform logger
     */
    public RequirementRegistry(@NotNull JExLogger logger) {
        this.logger = logger;
    }

    // ── Type registration ─────────────────────────────────────────────────────

    /**
     * Registers a requirement type.
     *
     * @param type the requirement type to register
     */
    public void register(@NotNull RequirementType type) {
        var key = type.id().toUpperCase();
        types.put(key, type);
        logger.debug("Registered requirement type: {}", type.qualifiedName());
    }

    /**
     * Unregisters a requirement type.
     *
     * @param typeName the type name to unregister
     */
    public void unregister(@NotNull String typeName) {
        var removed = types.remove(typeName.toUpperCase());
        if (removed != null) {
            logger.debug("Unregistered requirement type: {}", removed.qualifiedName());
        }
    }

    /**
     * Finds a requirement type by name.
     *
     * @param typeName the type name
     * @return the type, or empty if not registered
     */
    public @NotNull Optional<RequirementType> find(@NotNull String typeName) {
        return Optional.ofNullable(types.get(typeName.toUpperCase()));
    }

    /**
     * Checks whether a type is registered.
     *
     * @param typeName the type name
     * @return {@code true} if registered
     */
    public boolean isRegistered(@NotNull String typeName) {
        return types.containsKey(typeName.toUpperCase());
    }

    /**
     * Returns all registered types.
     *
     * @return an unmodifiable map of type names to types
     */
    public @NotNull Map<String, RequirementType> types() {
        return Map.copyOf(types);
    }

    // ── Provider management ───────────────────────────────────────────────────

    /**
     * Registers a plugin requirement provider and all its types.
     *
     * @param provider the provider to register
     */
    public void registerProvider(@NotNull PluginRequirementProvider provider) {
        var pluginId = provider.pluginId();

        if (providers.containsKey(pluginId)) {
            logger.warn("Provider already registered for plugin: {}", pluginId);
            return;
        }

        providers.put(pluginId, provider);
        provider.register(this);
        logger.info("Registered requirement provider: {}", pluginId);
    }

    /**
     * Unregisters a plugin requirement provider.
     *
     * @param pluginId the plugin ID to unregister
     */
    public void unregisterProvider(@NotNull String pluginId) {
        var provider = providers.remove(pluginId);
        if (provider != null) {
            provider.unregister(this);
            logger.info("Unregistered requirement provider: {}", pluginId);
        }
    }

    /**
     * Returns all registered providers.
     *
     * @return an unmodifiable map
     */
    public @NotNull Map<String, PluginRequirementProvider> providers() {
        return Map.copyOf(providers);
    }

    /**
     * Returns the total number of registered types.
     *
     * @return the type count
     */
    public int size() {
        return types.size();
    }
}
