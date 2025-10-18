package com.raindropcentral.commands;

import de.jexcellence.evaluable.error.CommandError;
import de.jexcellence.evaluable.error.EErrorType;
import de.jexcellence.evaluable.section.ACommandSection;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class ServerCommand extends BukkitCommand {
	
	protected ServerCommand(
		final @NotNull ACommandSection commandSection
	) {
		
		super(commandSection);
	}
	
	protected abstract void onPlayerInvocation(
		final @NotNull ConsoleCommandSender console,
		final @NotNull String alias,
		final @NotNull String[] args
	);
	
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
