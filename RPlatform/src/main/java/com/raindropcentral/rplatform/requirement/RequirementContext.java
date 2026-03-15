package com.raindropcentral.rplatform.requirement;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context that provides external dependencies to requirements.
 * This allows requirements to access plugin-specific services without
 * needing to discover them at runtime via reflection.
 */
public class RequirementContext {
    
    private static final RequirementContext INSTANCE = new RequirementContext();
    
    private final Map<String, Object> services = new ConcurrentHashMap<>();
    
    private RequirementContext() {}
    
    /**
     * Gets instance.
     */
    public static RequirementContext getInstance() {
        return INSTANCE;
    }
    
    /**
     * Register a service in the context.
     */
    public void registerService(String key, Object service) {
        services.put(key, service);
    }
    
    /**
     * Get a service from the context.
     */
    @Nullable
    public <T> T getService(String key, Class<T> type) {
        Object service = services.get(key);
        if (type.isInstance(service)) {
            return type.cast(service);
        }
        return null;
    }
    
    /**
     * Get a service as Object (for reflection-based access).
     */
    @Nullable
    public Object getService(String key) {
        return services.get(key);
    }
    
    /**
     * Check if a service is registered.
     */
    public boolean hasService(String key) {
        return services.containsKey(key);
    }
    
    /**
     * Clear all services (useful for testing).
     */
    public void clear() {
        services.clear();
    }
}
