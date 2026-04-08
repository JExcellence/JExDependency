package de.jexcellence.home.exception;

/**
 * Exception thrown when a player has reached their home limit.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class HomeLimitReachedException extends RuntimeException {

    private final int currentCount;
    private final int maxLimit;

    public HomeLimitReachedException(int currentCount, int maxLimit) {
        super("Home limit reached: " + currentCount + "/" + maxLimit);
        this.currentCount = currentCount;
        this.maxLimit = maxLimit;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public int getMaxLimit() {
        return maxLimit;
    }
}
