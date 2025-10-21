package com.raindropcentral.rplatform.placeholder;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test stub simulating the PlaceholderAPI expansion bridge so unit tests can
 * verify registration and unregistration flows without loading the actual
 * PlaceholderAPI dependency.
 */
public final class PAPIHook {

    private static final AtomicInteger REGISTER_CALLS = new AtomicInteger();
    private static final AtomicInteger UNREGISTER_CALLS = new AtomicInteger();
    private static final Set<String> ACTIVE_IDENTIFIERS = ConcurrentHashMap.newKeySet();
    private static final List<String> CALL_SEQUENCE = Collections.synchronizedList(new ArrayList<>());
    private static final String IDENTIFIER_PATTERN = "^[a-z0-9_]+$";

    private final JavaPlugin plugin;
    private final String identifier;

    public PAPIHook(final JavaPlugin plugin, final String identifier) {
        this.plugin = plugin;
        this.identifier = identifier;
    }

    public void register() {
        validateIdentifier();

        if (!ACTIVE_IDENTIFIERS.add(identifier)) {
            throw new IllegalStateException("Identifier already registered: " + identifier);
        }

        REGISTER_CALLS.incrementAndGet();
        CALL_SEQUENCE.add("register:" + identifier);
        plugin.getLogger().info("Registered test placeholder: " + identifier);
    }

    public void unregister() {
        if (!ACTIVE_IDENTIFIERS.remove(identifier)) {
            throw new IllegalStateException("Identifier not registered: " + identifier);
        }

        UNREGISTER_CALLS.incrementAndGet();
        CALL_SEQUENCE.add("unregister:" + identifier);
        plugin.getLogger().info("Unregistered test placeholder: " + identifier);
    }

    private void validateIdentifier() {
        if (identifier == null || identifier.isBlank() || !identifier.matches(IDENTIFIER_PATTERN)) {
            throw new IllegalArgumentException("Invalid identifier: " + identifier);
        }
    }

    public static void reset() {
        REGISTER_CALLS.set(0);
        UNREGISTER_CALLS.set(0);
        CALL_SEQUENCE.clear();
        ACTIVE_IDENTIFIERS.clear();
    }

    public static int getRegisterCalls() {
        return REGISTER_CALLS.get();
    }

    public static int getUnregisterCalls() {
        return UNREGISTER_CALLS.get();
    }

    public static List<String> getCallSequence() {
        synchronized (CALL_SEQUENCE) {
            return List.copyOf(CALL_SEQUENCE);
        }
    }

    public static boolean isIdentifierActive(final String candidate) {
        return ACTIVE_IDENTIFIERS.contains(candidate);
    }
}
