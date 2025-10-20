package com.raindropcentral.rplatform.placeholder;

import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Test stub simulating the PlaceholderAPI expansion bridge so unit tests can
 * verify registration and unregistration flows without loading the actual
 * PlaceholderAPI dependency.
 */
public final class PAPIHook {

    private static final AtomicInteger REGISTER_CALLS = new AtomicInteger();
    private static final AtomicInteger UNREGISTER_CALLS = new AtomicInteger();

    private final JavaPlugin plugin;
    private final String identifier;

    public PAPIHook(final JavaPlugin plugin, final String identifier) {
        this.plugin = plugin;
        this.identifier = identifier;
    }

    public void register() {
        REGISTER_CALLS.incrementAndGet();
        plugin.getLogger().info("Registered test placeholder: " + identifier);
    }

    public void unregister() {
        UNREGISTER_CALLS.incrementAndGet();
        plugin.getLogger().info("Unregistered test placeholder: " + identifier);
    }

    public static void reset() {
        REGISTER_CALLS.set(0);
        UNREGISTER_CALLS.set(0);
    }

    public static int getRegisterCalls() {
        return REGISTER_CALLS.get();
    }

    public static int getUnregisterCalls() {
        return UNREGISTER_CALLS.get();
    }
}
