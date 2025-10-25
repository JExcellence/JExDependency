package de.jexcellence.economy.command;

/**
 * Represents an active command registration managed by the currency command handler.
 * <p>
 * Instances of this interface encapsulate framework specific state returned by the underlying
 * command registration subsystem. The handler stores each registration so it can dispose of them
 * when the plugin shuts down or the currency module is reloaded.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public interface CommandRegistration {

    /**
     * Disposes the registration and removes the command from the server command map.
     * <p>
     * Implementations should perform any necessary cleanup required by the hosting command
     * framework. The handler guarantees that {@code dispose} is only invoked once per registration.
     * </p>
     */
    void dispose();
}
