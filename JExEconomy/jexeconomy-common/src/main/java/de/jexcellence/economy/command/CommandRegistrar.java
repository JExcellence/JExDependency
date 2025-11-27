package de.jexcellence.economy.command;

import com.raindropcentral.commands.BukkitCommand;
import de.jexcellence.evaluable.section.ACommandSection;
import org.jetbrains.annotations.NotNull;

/**
 * Abstraction for registering command handlers and their configuration sections with the command framework.
 * <p>
 * The currency module depends on this contract so it can remain agnostic to the underlying Paper/Spigot
 * registration mechanics while still wiring all required command pairs during startup.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
@FunctionalInterface
public interface CommandRegistrar {

    /**
     * Registers a command section and its corresponding command class with the platform command framework.
     *
     * @param sectionClass the configuration section describing the command metadata, must not be null
     * @param commandClass the command implementation to register, must not be null
     * @return a {@link CommandRegistration} handle representing the active registration, never null
     */
    @NotNull
    CommandRegistration registerCommand(
            @NotNull Class<? extends ACommandSection> sectionClass,
            @NotNull Class<? extends BukkitCommand> commandClass
    );
}
