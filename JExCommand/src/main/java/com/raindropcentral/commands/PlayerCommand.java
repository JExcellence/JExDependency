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

/**
 * Base command implementation that restricts execution to player senders and provides
 * helper hooks for permission checks and tab completion routing.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public abstract class PlayerCommand extends BukkitCommand {
	
        /**
         * Creates a player-restricted command bound to the provided configuration section.
         *
         * @param commandSection configuration node providing metadata, permissions, and messages
         */
        protected PlayerCommand(
                final @NotNull ACommandSection commandSection
        ) {

                super(commandSection);
        }

        /**
         * Executes the command logic for a verified {@link Player} sender.
         *
         * @param player validated player sender
         * @param alias  alias used to trigger the command
         * @param args   full argument list supplied by Bukkit
         * @throws CommandError when the subclass needs to abort with a user-facing error
         */
        protected abstract void onPlayerInvocation(
                final @NotNull Player player,
                final @NotNull String alias,
                final @NotNull String[] args
        );
	
        /**
         * Ensures the sender is a {@link Player} before delegating to
         * {@link #onPlayerInvocation(Player, String, String[])}.
         *
         * @throws CommandError when the sender is not a player
         */
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
	
        /**
         * Restricts tab completion to player senders and forwards the request to
         * {@link #onPlayerTabCompletion(Player, String, String[])} when applicable.
         *
         * @return immutable empty list for non-player senders or subclass suggestions for players
         */
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
	
        /**
         * Evaluates the supplied permission node against the player and sends a localized missing
         * permission message when access should be denied.
         *
         * @param player          player attempting to execute the command
         * @param permissionNode  permission node defined in the command section
         * @return {@code true} when the player lacks the permission, otherwise {@code false}
         */
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
	
        /**
         * Generates tab completion suggestions for validated {@link Player} senders.
         *
         * @param player player requesting tab completions
         * @param alias  alias used to trigger the command
         * @param args   argument list supplied by Bukkit
         * @return suggestions to present to the player
         */
        protected abstract List<String> onPlayerTabCompletion(
                final @NotNull Player player,
                final @NotNull String alias,
                final @NotNull String[] args
	);
	
}
