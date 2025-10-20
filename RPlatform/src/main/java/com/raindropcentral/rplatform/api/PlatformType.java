package com.raindropcentral.rplatform.api;

/**
 * Enumerates the server platform variants supported by the {@link PlatformAPI} abstraction.
 *
 * <p>The ordering reflects the detection priority used by {@link PlatformAPIFactory}.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public enum PlatformType {
    /** Folia servers featuring regionized execution and strict thread guarantees. */
    FOLIA,
    /** Paper servers (1.20+) exposing modern Adventure and scheduling APIs. */
    PAPER,
    /** Legacy Spigot or Bukkit servers lacking the newer platform integrations. */
    SPIGOT
}
