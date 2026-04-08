package de.jexcellence.home.exception;

/**
 * Exception thrown when a home is not found.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class HomeNotFoundException extends RuntimeException {

    private final String homeName;

    public HomeNotFoundException(String homeName) {
        super("Home not found: " + homeName);
        this.homeName = homeName;
    }

    public String getHomeName() {
        return homeName;
    }
}
