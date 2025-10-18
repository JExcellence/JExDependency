package com.raindropcentral.rplatform.api;

import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PlatformAPIFactory {

    // Use JUL logger so messages flow through CentralLogger handlers/formatters.
    private static final Logger LOGGER = CentralLogger.getLogger(RPlatform.class);

    private PlatformAPIFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

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

    public static @NotNull PlatformAPI create(final @NotNull JavaPlugin plugin) {
        final PlatformType type = detectPlatformType();

        return switch (type) {
            case FOLIA -> createFoliaAPI(plugin);
            case PAPER -> createPaperAPI(plugin);
            case SPIGOT -> createSpigotAPI(plugin);
        };
    }

    private static @NotNull PlatformAPI createFoliaAPI(final @NotNull JavaPlugin plugin) {
        try {
            final Class<?> clazz = Class.forName("com.raindropcentral.rplatform.api.impl.FoliaPlatformAPI");
            return (PlatformAPI) clazz.getConstructor(JavaPlugin.class).newInstance(plugin);
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "FoliaPlatformAPI unavailable, falling back to Paper", e);
            return createPaperAPI(plugin);
        }
    }

    private static @NotNull PlatformAPI createPaperAPI(final @NotNull JavaPlugin plugin) {
        try {
            final Class<?> clazz = Class.forName("com.raindropcentral.rplatform.api.impl.PaperPlatformAPI");
            return (PlatformAPI) clazz.getConstructor(JavaPlugin.class).newInstance(plugin);
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "PaperPlatformAPI unavailable, falling back to Spigot", e);
            return createSpigotAPI(plugin);
        }
    }

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