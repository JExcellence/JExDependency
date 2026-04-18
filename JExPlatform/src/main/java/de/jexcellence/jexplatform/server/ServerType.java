package de.jexcellence.jexplatform.server;

/**
 * Detected server implementation type.
 *
 * <p>Each variant carries runtime-specific capabilities. Use
 * {@link ServerDetector#detect()} to resolve the current server type,
 * then pattern-match to branch on platform differences:
 *
 * <pre>{@code
 * switch (serverType) {
 *     case Folia f  -> log.info("Folia detected");
 *     case Paper p  -> log.info("Paper detected");
 *     case Spigot s -> log.info("Spigot detected");
 * }
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public sealed interface ServerType {

    /** Returns a human-readable name for this server type. */
    String name();

    /** Returns true if the server natively supports the Adventure component API. */
    boolean hasNativeAdventure();

    /** Returns true if the server uses region-threaded scheduling (Folia). */
    boolean isRegionThreaded();

    /** Folia — region-threaded Paper fork with per-region schedulers. */
    record Folia() implements ServerType {

        @Override
        public String name() {
            return "Folia";
        }

        @Override
        public boolean hasNativeAdventure() {
            return true;
        }

        @Override
        public boolean isRegionThreaded() {
            return true;
        }
    }

    /** Paper — modern Minecraft server with native Adventure API. */
    record Paper() implements ServerType {

        @Override
        public String name() {
            return "Paper";
        }

        @Override
        public boolean hasNativeAdventure() {
            return true;
        }

        @Override
        public boolean isRegionThreaded() {
            return false;
        }
    }

    /** Spigot — legacy server requiring Adventure platform bridge. */
    record Spigot() implements ServerType {

        @Override
        public String name() {
            return "Spigot";
        }

        @Override
        public boolean hasNativeAdventure() {
            return false;
        }

        @Override
        public boolean isRegionThreaded() {
            return false;
        }
    }
}
