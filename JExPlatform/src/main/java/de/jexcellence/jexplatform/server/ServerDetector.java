package de.jexcellence.jexplatform.server;

import org.jetbrains.annotations.NotNull;

/**
 * Detects the running server implementation via class-presence probes.
 *
 * <p>Detection order: Folia &rarr; Paper &rarr; Spigot. The first match wins.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class ServerDetector {

    private static final String FOLIA_MARKER = "io.papermc.paper.threadedregions.RegionizedServer";
    private static final String PAPER_MARKER = "io.papermc.paper.event.player.AsyncChatEvent";

    private ServerDetector() {
        // Utility class
    }

    /**
     * Probes the classpath and returns the detected server type.
     *
     * @return the server type, never null
     */
    public static @NotNull ServerType detect() {
        if (classExists(FOLIA_MARKER)) return new ServerType.Folia();
        if (classExists(PAPER_MARKER)) return new ServerType.Paper();
        return new ServerType.Spigot();
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
