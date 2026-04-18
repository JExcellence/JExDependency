package com.raindropcentral.commands;

import de.jexcellence.evaluable.error.CommandError;
import de.jexcellence.evaluable.error.ErrorType;
import de.jexcellence.evaluable.section.CommandSection;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Base command restricted to the server console.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 2.0.0
 */
public abstract class ServerCommand extends BukkitCommand {

    protected ServerCommand(final @NotNull CommandSection commandSection) {
        super(commandSection);
    }

    /**
     * Console-specific execution logic.
     *
     * @throws CommandError for user-facing errors
     */
    protected abstract void onConsoleInvocation(
            @NotNull ConsoleCommandSender console, @NotNull String alias, @NotNull String[] args
    );

    @Override
    protected void onInvocation(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof ConsoleCommandSender console)) {
            throw new CommandError(null, ErrorType.NOT_A_CONSOLE);
        }
        onConsoleInvocation(console, alias, args);
    }

    @Override
    protected List<String> onTabCompletion(CommandSender sender, String alias, String[] args) {
        return List.of();
    }
}
