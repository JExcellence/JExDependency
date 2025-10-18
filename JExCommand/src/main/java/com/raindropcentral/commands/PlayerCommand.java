package com.raindropcentral.commands;

import de.jexcellence.evaluable.error.CommandError;
import de.jexcellence.evaluable.error.EErrorType;
import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.evaluable.section.IPermissionNode;
import de.jexcellence.evaluable.section.PermissionsSection;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class PlayerCommand extends BukkitCommand {
	
	protected PlayerCommand(
		final @NotNull ACommandSection commandSection
	) {
		
		super(commandSection);
	}
	
	protected abstract void onPlayerInvocation(
		final @NotNull Player player,
		final @NotNull String alias,
		final @NotNull String[] args
	);
	
	@Override
	protected void onInvocation(
		final @NotNull CommandSender sender,
		final @NotNull String alias,
		final @NotNull String[] args
	) {
		
		if (! (sender instanceof Player player)) {
			throw new CommandError(
				null,
				EErrorType.NOT_A_PLAYER
			);
		}
		
		this.onPlayerInvocation(
			player,
			alias,
			args
		);
	}
	
	@Override
	protected List<String> onTabCompletion(
		final @NotNull CommandSender sender,
		final @NotNull String alias,
		final @NotNull String[] args
	) {
		
		if (! (sender instanceof Player player)) {
			return List.of();
		}
		
		return this.onPlayerTabCompletion(
			player,
			alias,
			args
		);
	}
	
	protected boolean hasNoPermission(
		final @NotNull Player player,
		final @NotNull IPermissionNode permissionNode
	) {
		
		final PermissionsSection permissionsSection = this.commandSection.getPermissions();
		if (
			permissionsSection == null
		) {
			return false;
		}
		
		if (
			permissionsSection.hasPermission(
				player,
				permissionNode
			)
		) {
			return false;
		}
		
		permissionsSection.sendMissingMessage(
			player,
			permissionNode
		);
		return true;
	}
	
	protected abstract List<String> onPlayerTabCompletion(
		final @NotNull Player player,
		final @NotNull String alias,
		final @NotNull String[] args
	);
	
}
