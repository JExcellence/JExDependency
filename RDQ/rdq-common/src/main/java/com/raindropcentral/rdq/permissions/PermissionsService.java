package com.raindropcentral.rdq.permissions;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.permissions.PermissionsSection;
import de.jexcellence.evaluable.ConfigKeeper;
import de.jexcellence.evaluable.ConfigManager;
import de.jexcellence.jextranslate.i18n.I18n;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages loading, querying, and assigning plugin permission sets.
 * <p>
 * This service is responsible for:
 * <ul>
 *     <li>Loading permission groupings from a YAML configuration file ({@code /permissions/permissions.yml})</li>
 *     <li>Providing access to parsed permission sets for assigning or displaying</li>
 *     <li>Adding permission sets to players, with dependency validation for the required service/plugin</li>
 *     <li>Sending appropriate, localized messages if dependencies are missing</li>
 * </ul>
 * <p>
 * <b>Integration:</b> Designed for plugin use as a single point of permission set logic and assignment.
 * Integrates with RCoreImpl's LuckPermsService, supports dependency-checked sets, and uses I18n for messages.
 * </p>
 *
 * <p><b>Configuration expected at:</b> <code>/permissions/permissions.yml</code> in plugin resources.</p>
 *
 * @author ItsRainingHP
 * @version 1.1.0
 * @since TBD
 */
public class PermissionsService {
	
	private final static Logger LOGGER = Logger.getLogger(PermissionsService.class.getName());
	private final static String FOLDER_PATH = "permissions";
	private final static String FILE_NAME   = "permissions.yml";
	
	/**
	 * Map of permission set names to lists of permission nodes.
	 * Populated from the YAML config.
     * -- GETTER --
     *  Returns all loaded permission sets.
     *
     * @return Map with keys as permission set/group names,
     * and values as lists of permission nodes; or {@code null} if not loaded.

     */
	@Getter
    private Map<String, List<String>> permissions;

	/**
	 * Default permissions group loaded from configuration.
	 * -- GETTER --
	 * Returns the configured default group name.
	 *
	 * @return the configured default group, or {@code "default"} when unspecified
	 */
	@Getter
	private String defaultGroup = "default";
	
	private final RDQ rdq;
	
	/**
	 * Loads permission sets from configuration.
	 *
	 * @param rdq Main plugin/context instance
	 */
	public PermissionsService(
		@NotNull RDQ rdq
	) {
		this.rdq = rdq;
		try {
			var cfgManager = new ConfigManager(rdq.getPlugin(), FOLDER_PATH);
			var cfgKeeper = new ConfigKeeper<>(cfgManager, FILE_NAME, PermissionsSection.class);
			var rootSection = cfgKeeper.rootSection;
			permissions = rootSection.getPermissions();
			defaultGroup = rootSection.getDefaultGroup();
		} catch (
			  final Exception exception
		) {
			LOGGER.log(
				Level.WARNING,
				"Error loading permissions.yml",
				exception
			);
		}
	}

    /**
	 * Assigns a set of permissions to a player, optionally gating by the presence of a service/plugin.
	 * <p>
	 * If {@code check} is true, validates the required service before proceeding.
	 * If dependency is missing, informs the player of the missing requirement.
	 * Integrates (if present) with LuckPermsService for permission assignment.
	 *
	 * @param player      The player to assign permissions to
	 * @param pluginName  The required service/plugin class to check for (used as a dependency)
	 * @param group       The permission group/set name
	 * @param permissions The permission nodes to add
	 * @param check       If true, validates the dependency before assignment
	 */
	public boolean addPermissionSet(
		final @NotNull Player player,
		final @NotNull String pluginName,
		final @NotNull String group,
		final @NotNull List<String> permissions,
		final boolean check
	) {
		if (
			check &&
			! Bukkit.getPluginManager().isPluginEnabled(pluginName)
		) {
            new I18n.Builder("admin_permissions_overview_ui.errors.plugin_not_found", player)
                    .includePrefix()
                    .withPlaceholder("plugin_name", pluginName)
                    .build().sendMessage();
			return false;
		}
		
		if (
			this.rdq.getLuckPermsService() == null
		) {
            new I18n.Builder("admin_permissions_overview_ui.errors.no_luckperms_installed", player)
                    .includePrefix()
                    .build().sendMessage();
			return false;
		}
		
		// Assign permissions - the LuckPermsService will create the group if it doesn't exist
		for (String permission : permissions) {
			this.rdq.getLuckPermsService().assignPermission(group, permission);
		}
		
		LOGGER.info("Assigning " + permissions.size() + " permissions to group '" + group + "'");
		
		return true;
	}
	
	/**
	 * Assigns a set of permissions to a player, optionally gating by the presence of a service/plugin.
	 * <p>
	 * If {@code check} is true, validates the required service before proceeding.
	 * If dependency is missing, informs the player of the missing requirement.
	 * Integrates (if present) with LuckPermsService for permission assignment.
	 *
	 * @param pluginName  The required service/plugin class to check for (used as a dependency)
	 * @param group       The permission group/set name
	 * @param permissions The permission nodes to add
	 * @param check       If true, validates the dependency before assignment
	 */
	public void addPermissionSet(
		@NotNull String pluginName,
		@NotNull String group,
		@NotNull List<String> permissions,
		boolean check
	) {
		if (
			check &&
			! Bukkit.getPluginManager().isPluginEnabled(pluginName)
		) {
			LOGGER.info("Notice: No LuckPerms service is enabled. Skipping permission assignment.");
			return;
		}
		
		if (
			this.rdq.getLuckPermsService() == null
		) {
			LOGGER.info("Notice: No LuckPerms service is enabled. Skipping permission assignment.");
			return;
		}
		
		for (
			final String permission : permissions
		) {
			this.rdq.getLuckPermsService().assignPermission(group, permission);
		}
		
		LOGGER.info(
			"Added " + permissions.size() + " permissions to group '" + group + "'."
		);
	}
	
	/**
	 * Assigns all loaded permission sets to the specified player.
	 * <p>
	 * Iterates through every permission group defined in the configuration and assigns
	 * each set of permissions to the player using the LuckPerms service, if available.
	 * Dependency on LuckPerms is checked for each assignment.
	 * </p>
	 *
	 * @param player the player to whom all permission sets will be assigned
	 */
	public void addAllPermissionSets(
		final @NotNull Player player
	) {
		this.permissions.forEach((permission, value) -> {
			addPermissionSet(
				player,
				"LuckPerms",
				permission,
				value,
				true
			);
		});
	}
	
	/**
	 * Assigns all loaded permission sets globally (e.g., to groups or the server).
	 * <p>
	 * Iterates through every permission group defined in the configuration and assigns
	 * each set of permissions using the LuckPerms service, if available.
	 * Dependency on LuckPerms is checked for each assignment.
	 * </p>
	 */
	public void addAllPermissionSets() {
		this.permissions.forEach((permission, value) -> {
			addPermissionSet(
				"LuckPerms",
				permission,
				value,
				true
			);
		});
	}
}
