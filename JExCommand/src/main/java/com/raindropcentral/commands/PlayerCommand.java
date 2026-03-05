package com.raindropcentral.commands;

import com.raindropcentral.commands.permission.PermissionParentProvider;
import de.jexcellence.evaluable.error.CommandError;
import de.jexcellence.evaluable.error.EErrorType;
import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.evaluable.section.IPermissionNode;
import de.jexcellence.evaluable.section.PermissionsSection;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base command implementation that restricts execution to player senders and provides
 * helper hooks for permission checks and tab completion routing.
 * <p>
 * The {@link CommandFactory} supplies the associated {@link ACommandSection} so player-only commands inherit edition
 * gating rules (for example, premium-exclusive handlers) and localized responses when permissions fail or arguments are
 * invalid. Implementations should keep logic lightweight because invocations occur on the synchronous Bukkit thread.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public abstract class PlayerCommand extends BukkitCommand {

        private static final String CONFIGURED_NODES_FIELD = "nodes";

        /**
         * Creates a player-restricted command bound to the provided configuration section.
         * Behaviour: registers the section metadata so edition gating and localization hooks apply before any execution.
         * Failure modes: none beyond passing {@code null}. Asynchronous considerations: should be instantiated during
         * synchronous plugin startup through the factory pipeline.
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
         * Behaviour: runs synchronously on the main thread with localization context already prepared by the section.
         * Failure modes: throw {@link CommandError} to feed localized feedback through the Bukkit pipeline. Asynchronous
         * considerations: defer long-running work to asynchronous tasks to avoid blocking the main thread.
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
         * Behaviour: provides edition-aware error messaging through the base {@link BukkitCommand} when non-player
         * contexts attempt to run the command. Failure modes: raises {@link CommandError} to inform the sender.
         * Asynchronous considerations: invoked on the synchronous command thread; do not call from asynchronous contexts.
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
         * Behaviour: enforces edition gating and localization by returning empty suggestions for non-player contexts.
         * Failure modes: none; non-player senders receive an immutable empty list. Asynchronous considerations: invoked
         * synchronously by Bukkit and should remain lightweight.
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
         * Behaviour: taps into {@link PermissionsSection} so localization hooks communicate edition-specific messaging
         * when access is blocked. Failure modes: returns {@code true} when the player lacks permission and triggers a
         * localized response. Asynchronous considerations: must run on the main thread because permission checks and
         * messaging depend on Bukkit APIs.
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
			this.hasPermission(
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
         * Checks whether the player has the supplied permission node without sending feedback.
         * Behaviour: uses the section-local permission mapping so command-specific aliases and parent registration
         * remain consistent across execution, GUI visibility, and tab completion checks.
         *
         * @param player         player being evaluated
         * @param permissionNode permission node defined by the command
         * @return {@code true} when the player has the permission or no section is configured
         */
        protected boolean hasPermission(
                final @NotNull Player player,
                final @NotNull IPermissionNode permissionNode
        ) {

		if (
			player.isOp()
		) {
			return true;
		}

		final PermissionsSection permissionsSection = this.commandSection.getPermissions();
		if (
			permissionsSection == null
		) {
			return true;
		}

		if (
			permissionsSection.hasPermission(
				player,
				permissionNode
			)
		) {
			return true;
		}

		if (! (this.commandSection instanceof PermissionParentProvider permissionParentProvider)) {
			return false;
		}

		final Map<String, List<String>> permissionParents = permissionParentProvider.getPermissionParents();
		if (permissionParents.isEmpty()) {
			return false;
		}

		final Map<String, String> configuredNodes = this.extractConfiguredNodes(permissionsSection);
		final String targetNode = this.resolveNode(
			configuredNodes,
			permissionNode.getInternalName(),
			permissionNode.getFallbackNode()
		);

		if (
			targetNode == null
		) {
			return false;
		}

		return this.hasInheritedPermission(
			player,
			targetNode,
			configuredNodes,
			permissionParents,
			new HashSet<>()
		);
        }

        private boolean hasInheritedPermission(
                final @NotNull Player player,
                final @NotNull String targetNode,
                final @NotNull Map<String, String> configuredNodes,
                final @NotNull Map<String, List<String>> permissionParents,
                final @NotNull Set<String> visitedNodes
        ) {

		if (! visitedNodes.add(targetNode)) {
			return false;
		}

		for (Map.Entry<String, List<String>> entry : permissionParents.entrySet()) {
			final String parentNode = this.resolveNode(
				configuredNodes,
				entry.getKey(),
				null
			);

			if (
				parentNode == null
			) {
				continue;
			}

			final List<String> children = entry.getValue();
			if (
				children == null ||
				children.isEmpty()
			) {
				continue;
			}

			for (String childReference : children) {
				final String childNode = this.resolveNode(
					configuredNodes,
					childReference,
					null
				);

				if (! targetNode.equals(childNode)) {
					continue;
				}

				if (
					player.hasPermission(parentNode) ||
					this.hasInheritedPermission(
						player,
						parentNode,
						configuredNodes,
						permissionParents,
						visitedNodes
					)
				) {
					return true;
				}
			}
		}

		return false;
        }

        private Map<String, String> extractConfiguredNodes(
                final @NotNull PermissionsSection permissionsSection
        ) {

		final Map<String, String> configuredNodes = new HashMap<>();

		try {
			final Field nodesField = PermissionsSection.class.getDeclaredField(CONFIGURED_NODES_FIELD);
			nodesField.setAccessible(true);

			final Object rawNodes = nodesField.get(permissionsSection);
			if (! (rawNodes instanceof Map<?, ?> nodesMap)) {
				return Map.of();
			}

			for (Map.Entry<?, ?> entry : nodesMap.entrySet()) {
				if (
					entry.getKey() instanceof String internalName &&
					entry.getValue() instanceof String permissionNode
				) {
					configuredNodes.put(
						internalName,
						permissionNode
					);
				}
			}
		} catch (final ReflectiveOperationException ignored) {
			return Map.of();
		}

		return configuredNodes;
        }

        private String resolveNode(
                final @NotNull Map<String, String> configuredNodes,
                final String permissionReference,
                final String fallbackNode
        ) {

		if (permissionReference != null) {
			final String trimmedReference = permissionReference.trim();

			if (! trimmedReference.isEmpty()) {
				final String configuredNode = configuredNodes.get(trimmedReference);
				if (
					configuredNode != null &&
					! configuredNode.isBlank()
				) {
					return configuredNode;
				}

				if (trimmedReference.contains(".")) {
					return trimmedReference;
				}
			}
		}

		if (
			fallbackNode == null ||
			fallbackNode.isBlank()
		) {
			return null;
		}

		return fallbackNode;
        }
	
        /**
         * Generates tab completion suggestions for validated {@link Player} senders.
         * Behaviour: should produce lightweight suggestions sourced from data that already respects edition gating.
         * Failure modes: subclasses may return an empty list when no suggestions are appropriate. Asynchronous
         * considerations: executes synchronously; pre-compute heavy data asynchronously if needed.
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
