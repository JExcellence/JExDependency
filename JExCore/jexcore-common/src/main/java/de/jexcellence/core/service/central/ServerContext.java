package de.jexcellence.core.service.central;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Snapshot of the running server's runtime identity. Produced at plugin load
 * and passed into the {@link CentralService} to establish a stable UUID for
 * remote registration.
 *
 * @param serverUuid stable UUID identifying this server across restarts
 * @param serverVersion Paper/Spigot version string
 * @param pluginVersion JExCore version currently running
 */
public record ServerContext(@NotNull UUID serverUuid, @NotNull String serverVersion, @NotNull String pluginVersion) {
}
