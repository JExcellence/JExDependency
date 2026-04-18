package com.raindropcentral.commands.permission;

import de.jexcellence.evaluable.section.PermissionsSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared utility for extracting and resolving configured permission nodes
 * from a {@link PermissionsSection} via reflection.
 *
 * @author JExcellence
 * @since 1.0.1
 * @version 2.0.0
 */
public final class PermissionNodeResolver {

    private static final Logger LOG = LoggerFactory.getLogger(PermissionNodeResolver.class);
    private static final String NODES_FIELD = "nodes";

    private PermissionNodeResolver() {}

    /**
     * Extracts the internal-name-to-permission-node mapping via reflection.
     *
     * @return map of internal names to permission strings, or empty map on failure
     */
    @NotNull
    public static Map<String, String> extractConfiguredNodes(@NotNull PermissionsSection section) {
        try {
            var field = PermissionsSection.class.getDeclaredField(NODES_FIELD);
            field.setAccessible(true);

            var raw = field.get(section);
            if (!(raw instanceof Map<?, ?> map)) return Map.of();

            var result = new HashMap<String, String>();
            for (var entry : map.entrySet()) {
                if (entry.getKey() instanceof String k && entry.getValue() instanceof String v) {
                    result.put(k, v);
                }
            }
            return result;
        } catch (ReflectiveOperationException e) {
            LOG.warn("Unable to read configured permission nodes", e);
            return Map.of();
        }
    }

    /**
     * Resolves a reference to its actual permission node string.
     *
     * <p>Tries: configured node lookup, then literal (if contains a dot), then fallback.
     */
    @Nullable
    public static String resolveNode(
            @NotNull Map<String, String> configuredNodes,
            @Nullable String reference,
            @Nullable String fallback
    ) {
        if (reference != null) {
            var trimmed = reference.trim();
            if (!trimmed.isEmpty()) {
                var configured = configuredNodes.get(trimmed);
                if (configured != null && !configured.isBlank()) return configured;
                if (trimmed.contains(".")) return trimmed;
            }
        }
        return (fallback != null && !fallback.isBlank()) ? fallback : null;
    }

    /** Resolves without a fallback. */
    @Nullable
    public static String resolveNode(@NotNull Map<String, String> configuredNodes, @Nullable String reference) {
        return resolveNode(configuredNodes, reference, null);
    }
}
