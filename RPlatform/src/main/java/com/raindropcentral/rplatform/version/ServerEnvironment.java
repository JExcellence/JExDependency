package com.raindropcentral.rplatform.version;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class ServerEnvironment {

    private static final Logger LOGGER = Logger.getLogger(ServerEnvironment.class.getName());

    private static volatile ServerEnvironment instance;

    private final ServerType serverType;
    private final String serverVersion;
    private final String minecraftVersion;
    private final boolean modernVersion;

    private ServerEnvironment() {
        this.serverType = detectServerType();
        this.minecraftVersion = detectMinecraftVersion();
        this.serverVersion = detectServerVersion();
        this.modernVersion = isVersionModern();

        logEnvironmentInfo();
    }

    public static @NotNull ServerEnvironment getInstance() {
        if (instance == null) {
            synchronized (ServerEnvironment.class) {
                if (instance == null) {
                    instance = new ServerEnvironment();
                }
            }
        }
        return instance;
    }

    public @NotNull ServerType getServerType() {
        return serverType;
    }

    public @NotNull String getServerVersion() {
        return serverVersion;
    }

    public @NotNull String getMinecraftVersion() {
        return minecraftVersion;
    }

    public boolean isModern() {
        return modernVersion;
    }

    public boolean isPaper() {
        return serverType == ServerType.PAPER || serverType == ServerType.PURPUR;
    }

    public boolean isFolia() {
        return serverType == ServerType.FOLIA;
    }

    public boolean isSpigot() {
        return serverType == ServerType.SPIGOT;
    }

    public boolean hasClass(final @NotNull String className) {
        try {
            Class.forName(className);
            return true;
        } catch (final ClassNotFoundException e) {
            return false;
        }
    }

    public boolean isVersionAtLeast(final @NotNull String targetVersion) {
        return serverVersion.compareTo(targetVersion) >= 0;
    }

    private @NotNull ServerType detectServerType() {
        if (hasClass("io.papermc.paper.threadedregions.RegionizedServer")) {
            return ServerType.FOLIA;
        }

        if (hasClass("com.destroystokyo.paper.ParticleBuilder")) {
            if (hasClass("org.purpurmc.purpur.PurpurConfig")) {
                return ServerType.PURPUR;
            }
            return ServerType.PAPER;
        }

        return ServerType.SPIGOT;
    }

    private @NotNull String detectMinecraftVersion() {
        try {
            return Bukkit.getVersion();
        } catch (final Exception e) {
            LOGGER.warning("Failed to detect Minecraft version: " + e.getMessage());
            return "unknown";
        }
    }

    private @NotNull String detectServerVersion() {
        try {
            final String packageName = Bukkit.getServer().getClass().getPackage().getName();
            final String[] parts = packageName.split("\\.");
            
            if (parts.length > 3) {
                return parts[3];
            }
        } catch (final Exception e) {
            LOGGER.warning("Failed to detect server version: " + e.getMessage());
        }
        
        return "unknown";
    }

    private boolean isVersionModern() {
        if ("unknown".equals(serverVersion)) {
            return true;
        }
        return serverVersion.compareTo("v1_13") >= 0;
    }

    private void logEnvironmentInfo() {
        LOGGER.info("=== Server Environment ===");
        LOGGER.info("Type: " + serverType);
        LOGGER.info("Version: " + serverVersion);
        LOGGER.info("Minecraft: " + minecraftVersion);
        LOGGER.info("Modern: " + modernVersion);
        LOGGER.info("=========================");
    }

    public enum ServerType {
        FOLIA,
        PAPER,
        PURPUR,
        SPIGOT
    }
}
