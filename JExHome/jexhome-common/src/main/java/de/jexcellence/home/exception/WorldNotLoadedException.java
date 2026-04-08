package de.jexcellence.home.exception;

/**
 * Exception thrown when a world is not loaded.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class WorldNotLoadedException extends RuntimeException {

    private final String worldName;

    public WorldNotLoadedException(String worldName) {
        super("World not loaded: " + worldName);
        this.worldName = worldName;
    }

    public String getWorldName() {
        return worldName;
    }
}
