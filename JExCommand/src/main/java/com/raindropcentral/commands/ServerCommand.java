package com.raindropcentral.commands;

import de.jexcellence.evaluable.error.CommandError;
import de.jexcellence.evaluable.error.EErrorType;
import de.jexcellence.evaluable.section.ACommandSection;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Base command implementation for console-only interactions, ensuring only the server
 * console can execute the command logic while deferring to subclasses for behavior.
 * <p>
 * The {@link CommandFactory} injects the relevant {@link ACommandSection} so console commands honour edition gating
 * (for example, maintenance commands that only ship with premium editions) and reuse localization hooks for feedback.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public abstract class ServerCommand extends BukkitCommand {

        /**
         * Creates a console-restricted command bound to the provided configuration section.
         * Behaviour: wires command metadata so the factory can apply edition gating and localization prior to execution.
         * Failure modes: none aside from passing {@code null}. Asynchronous considerations: instantiate via the factory on
         * the main thread to mirror Bukkit&apos;s threading model.
         *
         * @param commandSection configuration node providing metadata, permissions, and messages
         */
        protected ServerCommand(
                final @NotNull ACommandSection commandSection
        ) {

                super(commandSection);
        }

        /**
         * Executes the command logic for a verified {@link ConsoleCommandSender}.
         * Behaviour: runs synchronously with edition-aware context prepared by the command section. Failure modes:
         * throw {@link CommandError} so localized feedback can reach the console. Asynchronous considerations: avoid
         * blocking the main thread; schedule long-running tasks when necessary.
         *
         * @param console console sender invoking the command
         * @param alias   alias used to trigger the command
         * @param args    full argument list supplied by Bukkit
         * @throws CommandError when the subclass needs to abort with a user-facing error
         */
        protected abstract void onPlayerInvocation(
                final @NotNull ConsoleCommandSender console,
                final @NotNull String alias,
                final @NotNull String[] args
        );

        /**
         * Verifies the sender is the server console and delegates to
         * {@link #onPlayerInvocation(ConsoleCommandSender, String, String[])}.
         * Behaviour: bridges edition-aware localization so unauthorized senders receive consistent messaging. Failure
         * modes: raises {@link CommandError} when the sender is not the console. Asynchronous considerations: invoked
         * on the synchronous command thread; avoid calling from asynchronous contexts.
         *
         * @throws CommandError when the sender is not the console
         */
        @Override
        protected void onInvocation(
                final @NotNull CommandSender sender,
                final @NotNull String alias,
                final @NotNull String[] args
        ) {
		
		if (! (sender instanceof ConsoleCommandSender console)) {
			throw new CommandError(
				null,
				EErrorType.NOT_A_CONSOLE
			);
		}
		
		this.onPlayerInvocation(
			console,
			alias,
			args
		);
	}
	
        /**
         * Server commands do not provide tab completion suggestions.
         * Behaviour: always returns an immutable empty list to signal that console commands should be executed explicitly.
         * Failure modes: none. Asynchronous considerations: synchronous invocation only; method is intentionally
         * side-effect free.
         *
         * @return immutable empty list for all requests
         */
        @Override
        protected List<String> onTabCompletion(
                final CommandSender sender,
                final String alias,
                final String[] args
        ) {
		//throw new UnsupportedOperationException("Tab completion is not supported for server commands. Use player commands instead.");
		return List.of();
	}
	
}
