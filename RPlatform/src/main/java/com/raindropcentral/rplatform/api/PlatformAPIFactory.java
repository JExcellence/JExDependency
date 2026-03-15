package com.raindropcentral.rplatform.api;

import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility responsible for instantiating {@link PlatformAPI} implementations based on the active.
 * server environment.
 *
 * <p>The factory performs a hierarchy of reflection checks to determine whether Folia, modern
 * Paper, or legacy Spigot classes are present and logs fallbacks for operator awareness.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class PlatformAPIFactory {

    /**
     * Logger used to communicate detection and fallback results through the centralized logging.
     * pipeline.
     *
     * <p><strong>Lifecycle:</strong> Shared across the JVM; the JUL bridge ensures the handlers and
     * formatters managed by {@link CentralLogger} receive all diagnostics.</p>
     */
    private static final Logger LOGGER = Logger.getLogger(PlatformAPIFactory.class.getName());

    /**
     * Prevents instantiation because the factory exposes only static helpers.
     */
    private PlatformAPIFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Detects the most suitable {@link PlatformType} based on available server classes.
     *
     * <p><strong>Usage:</strong> Invoked automatically by {@link #create(JavaPlugin)}, but also
     * useful for logging or telemetry regarding the current runtime.</p>
     *
     * @return the detected platform type
     */
    public static @NotNull PlatformType detectPlatformType() {
        // Detect Folia
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return PlatformType.FOLIA;
        } catch (final ClassNotFoundException ignored) {
        }

        // Detect modern Paper (1.20+)
        try {
            Class.forName("io.papermc.paper.plugin.entrypoint.LaunchEntryPointHandler");
            return PlatformType.PAPER;
        } catch (final ClassNotFoundException ignored) {
        }

        // Fallback: Spigot
        return PlatformType.SPIGOT;
    }

    /**
     * Creates a {@link PlatformAPI} instance tailored to the provided plugin's runtime platform.
     *
     * <p><strong>Usage:</strong> Call during plugin enable to obtain the abstraction used across the
     * rest of the codebase. The platform type is resolved once per invocation.</p>
     *
     * @param plugin the owning plugin requesting the API
     * @return the platform-specific API implementation
     */
    public static @NotNull PlatformAPI create(final @NotNull JavaPlugin plugin) {
        final PlatformType type = detectPlatformType();

        return switch (type) {
            case FOLIA -> createFoliaAPI(plugin);
            case PAPER -> createPaperAPI(plugin);
            case SPIGOT -> createSpigotAPI(plugin);
        };
    }

    /**
     * Attempts to create the Folia platform implementation, falling back to Paper on failure.
     *
     * <p><strong>Usage:</strong> Not intended for direct consumption; the fallback logs any
     * reflective instantiation errors to assist with diagnosing missing shaded classes.</p>
     *
     * @param plugin the plugin requesting the implementation
     * @return a Folia-aware platform API when available
     */
    private static @NotNull PlatformAPI createFoliaAPI(final @NotNull JavaPlugin plugin) {
        try {
            final Class<?> clazz = Class.forName("com.raindropcentral.rplatform.api.impl.FoliaPlatformAPI");
            return (PlatformAPI) clazz.getConstructor(JavaPlugin.class).newInstance(plugin);
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "FoliaPlatformAPI unavailable, falling back to Paper", e);
            return createPaperAPI(plugin);
        }
    }

    /**
     * Attempts to create the Paper platform implementation, falling back to Spigot on failure.
     *
     * <p><strong>Usage:</strong> Retains compatibility with servers that may remove Paper-only
     * entrypoints by logging the failure before delegating to the legacy implementation.</p>
     *
     * @param plugin the plugin requesting the implementation
     * @return a Paper-specific platform API when available
     */
    private static @NotNull PlatformAPI createPaperAPI(final @NotNull JavaPlugin plugin) {
        try {
            final Class<?> clazz = Class.forName("com.raindropcentral.rplatform.api.impl.PaperPlatformAPI");
            return (PlatformAPI) clazz.getConstructor(JavaPlugin.class).newInstance(plugin);
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "PaperPlatformAPI unavailable, falling back to Spigot", e);
            return createSpigotAPI(plugin);
        }
    }

    /**
     * Creates the Spigot platform implementation and surfaces failures as runtime exceptions.
     *
     * <p><strong>Usage:</strong> Final fallback path; if reflective instantiation fails the plugin
     * cannot operate and a descriptive runtime exception is raised.</p>
     *
     * @param plugin the plugin requesting the implementation
     * @return the Spigot-compatible platform API
     */
    private static @NotNull PlatformAPI createSpigotAPI(final @NotNull JavaPlugin plugin) {
        try {
            final Class<?> clazz = Class.forName("com.raindropcentral.rplatform.api.impl.SpigotPlatformAPI");
            return (PlatformAPI) clazz.getConstructor(JavaPlugin.class).newInstance(plugin);
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "SpigotPlatformAPI unavailable", e);
            throw new RuntimeException("Failed to create platform API: Could not load SpigotPlatformAPI (class missing or constructor signature mismatch)", e);
        }
    }
}
