package de.jexcellence.economy.command.player.currencies;

import de.jexcellence.economy.command.player.currencies.ECurrenciesAction;
import de.jexcellence.evaluable.section.IPermissionNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enumeration defining permission nodes for currency-related player commands within the JExEconomy system.
 *
 * <p>This enum provides a comprehensive permission framework for controlling access to currency management
 * operations. Each permission node represents a specific level of access to currency functionality,
 * enabling fine-grained control over who can perform various currency operations.
 *
 * <p><strong>Permission Hierarchy:</strong>
 * <ul>
 *   <li><strong>Base Access:</strong> {@link #CURRENCIES} - Basic currency command access</li>
 *   <li><strong>Administrative Operations:</strong> {@link #CREATE}, {@link #DELETE}, {@link #EDIT} - Currency modification rights</li>
 *   <li><strong>Information Access:</strong> {@link #OVERVIEW} - Currency viewing and listing permissions</li>
 * </ul>
 *
 * <p><strong>Security Model:</strong>
 * <ul>
 *   <li><strong>Granular Control:</strong> Each operation has its own permission node</li>
 *   <li><strong>Hierarchical Structure:</strong> Permissions follow a logical hierarchy</li>
 *   <li><strong>Fallback Support:</strong> Each permission includes a fallback node for compatibility</li>
 *   <li><strong>Integration Ready:</strong> Implements IPermissionNode for framework integration</li>
 * </ul>
 *
 * <p><strong>Usage Patterns:</strong>
 * <ul>
 *   <li>Permission checks before command execution</li>
 *   <li>Role-based access control configuration</li>
 *   <li>Administrative interface access control</li>
 *   <li>API endpoint security validation</li>
 * </ul>
 *
 * <p><strong>Integration Points:</strong>
 * <ul>
 *   <li>Command framework permission validation</li>
 *   <li>User interface access control</li>
 *   <li>Plugin configuration systems</li>
 *   <li>External permission management plugins</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 * @see IPermissionNode
 * @see ECurrenciesAction
 */
public enum ECurrenciesPermission implements IPermissionNode {
	
	/**
	 * Base permission node for accessing currency commands and functionality.
 *
 * <p>This permission grants basic access to the currency command system, allowing
	 * users to execute the base currency command and access general currency
	 * information. It serves as the foundation permission for all currency operations.
	 *
	 * <p><strong>Access Granted:</strong>
	 * <ul>
	 *   <li>Basic currency command execution</li>
	 *   <li>Access to currency help information</li>
	 *   <li>General currency system interaction</li>
	 *   <li>Foundation for other currency permissions</li>
	 * </ul>
	 *
	 * <p><strong>Security Level:</strong>
	 * <ul>
	 *   <li><strong>Risk Level:</strong> Low - Read-only access to basic functionality</li>
	 *   <li><strong>User Type:</strong> General users and administrators</li>
	 *   <li><strong>Impact:</strong> No system modifications possible</li>
	 * </ul>
	 */
	CURRENCIES(
		"command",
		"currencies.command"
	),
	
	/**
	 * Permission node for creating new currency entities within the system.
 *
 * <p>This permission grants the ability to create new currencies with custom
	 * properties, including identifiers, symbols, prefixes, and suffixes. Currency
	 * creation is a high-privilege operation that affects the entire system.
	 *
	 * <p><strong>Access Granted:</strong>
	 * <ul>
	 *   <li>Creation of new currency entities</li>
	 *   <li>Definition of currency properties and formatting</li>
	 *   <li>Automatic player account initialization</li>
	 *   <li>System-wide currency configuration</li>
	 * </ul>
	 *
	 * <p><strong>Security Level:</strong>
	 * <ul>
	 *   <li><strong>Risk Level:</strong> High - Creates system-wide changes</li>
	 *   <li><strong>User Type:</strong> Administrators and senior staff only</li>
	 *   <li><strong>Impact:</strong> Affects all players and system economy</li>
	 * </ul>
	 *
	 * <p><strong>Prerequisites:</strong>
	 * <ul>
	 *   <li>Typically requires {@link #CURRENCIES} base permission</li>
	 *   <li>Administrative role or equivalent access level</li>
	 *   <li>Understanding of currency system implications</li>
	 * </ul>
	 */
	CREATE(
		"commandCreate",
		"currencies.command.create"
	),
	
	/**
	 * Permission node for deleting existing currency entities from the system.
 *
 * <p>This permission grants the ability to permanently remove currencies from
	 * the database, including all associated player balances and transaction
	 * history. This is the most destructive currency operation available.
	 *
	 * <p><strong>Access Granted:</strong>
	 * <ul>
	 *   <li>Permanent deletion of currency entities</li>
	 *   <li>Removal of all associated player balances</li>
	 *   <li>Cleanup of transaction history and references</li>
	 *   <li>System cache and configuration updates</li>
	 * </ul>
	 *
	 * <p><strong>Security Level:</strong>
	 * <ul>
	 *   <li><strong>Risk Level:</strong> Critical - Irreversible data destruction</li>
	 *   <li><strong>User Type:</strong> Senior administrators only</li>
	 *   <li><strong>Impact:</strong> Permanent loss of player currency data</li>
	 * </ul>
	 *
	 * <p><strong>Safety Considerations:</strong>
	 * <ul>
	 *   <li>Should require additional confirmation mechanisms</li>
	 *   <li>Comprehensive audit logging recommended</li>
	 *   <li>Backup procedures should be in place</li>
	 *   <li>Consider implementing soft deletion alternatives</li>
	 * </ul>
	 */
	DELETE(
		"commandDelete",
		"currencies.command.delete"
	),
	
	/**
	 * Permission node for editing and updating existing currency properties.
 *
 * <p>This permission grants the ability to modify currency properties such as
	 * symbols, prefixes, suffixes, and identifiers. Editing operations affect
	 * how currencies are displayed and formatted throughout the system.
	 *
	 * <p><strong>Access Granted:</strong>
	 * <ul>
	 *   <li>Modification of currency symbols and formatting</li>
	 *   <li>Updates to currency identifiers (with uniqueness validation)</li>
	 *   <li>Changes to prefix and suffix display properties</li>
	 *   <li>Real-time system configuration updates</li>
	 * </ul>
	 *
	 * <p><strong>Security Level:</strong>
	 * <ul>
	 *   <li><strong>Risk Level:</strong> Medium - Modifies system-wide display and behavior</li>
	 *   <li><strong>User Type:</strong> Administrators and trusted staff</li>
	 *   <li><strong>Impact:</strong> Changes how currencies appear to all players</li>
	 * </ul>
	 *
	 * <p><strong>Validation Requirements:</strong>
	 * <ul>
	 *   <li>Currency existence validation before modification</li>
	 *   <li>Uniqueness checks for identifier changes</li>
	 *   <li>Format validation for symbols and display properties</li>
	 *   <li>Atomic updates to prevent inconsistent states</li>
	 * </ul>
	 */
	EDIT(
		"commandEdit",
		"currencies.command.update"
	),
	
	/**
	 * Permission node for viewing comprehensive currency overviews and listings.
 *
 * <p>This permission grants access to currency overview functionality, allowing
	 * users to view lists of all available currencies with their properties and
	 * status information. This is primarily an informational permission.
	 *
	 * <p><strong>Access Granted:</strong>
	 * <ul>
	 *   <li>Viewing complete currency listings</li>
	 *   <li>Access to currency property information</li>
	 *   <li>System status and configuration overview</li>
	 *   <li>Currency usage statistics and metrics</li>
	 * </ul>
	 *
	 * <p><strong>Security Level:</strong>
	 * <ul>
	 *   <li><strong>Risk Level:</strong> Low - Read-only access to system information</li>
	 *   <li><strong>User Type:</strong> General users, staff, and administrators</li>
	 *   <li><strong>Impact:</strong> No system modifications possible</li>
	 * </ul>
	 *
	 * <p><strong>Information Provided:</strong>
	 * <ul>
	 *   <li>Currency identifiers and symbols</li>
	 *   <li>Formatting properties (prefix, suffix)</li>
	 *   <li>System configuration status</li>
	 *   <li>Usage statistics and player account information</li>
	 * </ul>
	 */
	OVERVIEW(
		"commandOverview",
		"currencies.command.overview"
	);
	
	/**
	 * The internal identifier used for this permission node within the evaluation framework.
 *
 * <p>This field stores the internal name that the permission system uses to identify
	 * and reference this specific permission node. The internal name is used for
	 * configuration mapping, permission resolution, and framework integration.
	 */
	private final String permissionInternalIdentifier;
	
	/**
	 * The fallback permission node string used for compatibility and external integration.
 *
 * <p>This field provides a standardized permission string that can be used with
	 * external permission management systems or as a fallback when the internal
	 * permission system is not available. It follows standard permission naming conventions.
	 */
	private final String fallbackPermissionNode;
	
	/**
	 * Constructs a new currency permission enum constant with the specified identifiers.
 *
 * <p>This constructor initializes the permission node with both an internal identifier
	 * for framework integration and a fallback permission string for external compatibility.
	 * Both identifiers are required and must be non-null.
	 *
	 * @param permissionInternalIdentifier the internal identifier used by the evaluation framework, must not be null
	 * @param fallbackPermissionNode the fallback permission string for external systems, must not be null
	 * @throws IllegalArgumentException if either parameter is null
	 */
	ECurrenciesPermission(
		final @NotNull String permissionInternalIdentifier,
		final @NotNull String fallbackPermissionNode
	) {
		if (
			permissionInternalIdentifier == null ||
			fallbackPermissionNode == null
		) {
			throw new IllegalArgumentException("Permission identifiers cannot be null");
		}
		
		this.permissionInternalIdentifier = permissionInternalIdentifier;
		this.fallbackPermissionNode = fallbackPermissionNode;
	}
	
	/**
	 * Retrieves the internal identifier for this permission node.
 *
 * <p>This method returns the internal name used by the evaluation framework to
	 * identify and process this permission node. The internal identifier is used
	 * for configuration mapping, permission resolution, and system integration.
	 *
	 * @return the internal identifier string, never null
	 */
	@Override
	public @NotNull String getInternalName() {
		return this.permissionInternalIdentifier;
	}
	
	/**
	 * Retrieves the fallback permission node string for external compatibility.
 *
 * <p>This method returns the standardized permission string that can be used
	 * with external permission management systems or as a fallback when the
	 * internal permission system is not available.
	 *
	 * @return the fallback permission node string, never null
	 */
	@Override
	public @NotNull String getFallbackNode() {
		return this.fallbackPermissionNode;
	}
	
	/**
	 * Attempts to resolve a currency permission from its internal identifier.
 *
 * <p>This method provides case-sensitive lookup of currency permissions based on
	 * their internal identifier strings. It returns an Optional containing the
	 * matching permission if found, or empty if no match exists.
	 *
	 * <p><strong>Matching Behavior:</strong>
	 * <ul>
	 *   <li>Case-sensitive comparison for precise matching</li>
	 *   <li>Exact string matching against internal identifiers</li>
	 *   <li>Returns Optional.empty() for null or invalid input</li>
	 *   <li>Supports all defined permission constants</li>
	 * </ul>
	 *
	 * @param internalIdentifier the internal identifier to resolve, may be null
	 * @return an Optional containing the matching permission if found, empty otherwise
	 */
	public static @NotNull Optional<ECurrenciesPermission> fromInternalName(final @Nullable String internalIdentifier) {
		if (
			internalIdentifier == null ||
			internalIdentifier.trim().isEmpty()
		) {
			return Optional.empty();
		}
		
		return Arrays.stream(ECurrenciesPermission.values())
		             .filter(currencyPermission -> currencyPermission.getInternalName().equals(internalIdentifier.trim()))
		             .findFirst();
	}
	
	/**
	 * Attempts to resolve a currency permission from its fallback node string.
 *
 * <p>This method provides case-sensitive lookup of currency permissions based on
	 * their fallback permission node strings. This is useful for integration with
	 * external permission systems that use standardized permission strings.
	 *
	 * <p><strong>Matching Behavior:</strong>
	 * <ul>
	 *   <li>Case-sensitive comparison for precise matching</li>
	 *   <li>Exact string matching against fallback node strings</li>
	 *   <li>Returns Optional.empty() for null or invalid input</li>
	 *   <li>Supports external permission system integration</li>
	 * </ul>
	 *
	 * @param fallbackNodeString the fallback node string to resolve, may be null
	 * @return an Optional containing the matching permission if found, empty otherwise
	 */
	public static @NotNull Optional<ECurrenciesPermission> fromFallbackNode(final @Nullable String fallbackNodeString) {
		if (
			fallbackNodeString == null ||
			fallbackNodeString.trim().isEmpty()
		) {
			return Optional.empty();
		}
		
		return Arrays.stream(ECurrenciesPermission.values())
		             .filter(currencyPermission -> currencyPermission.getFallbackNode().equals(fallbackNodeString.trim()))
		             .findFirst();
	}
	
	/**
	 * Determines whether this permission represents an administrative operation.
 *
 * <p>This method identifies permissions that should be restricted to users with
	 * administrative privileges due to their system-wide impact or security
	 * implications. Administrative permissions typically affect global system
	 * state or other players' data.
	 *
	 * <p><strong>Administrative Permissions:</strong>
	 * <ul>
	 *   <li>{@link #CREATE} - Creates new system-wide currencies</li>
	 *   <li>{@link #DELETE} - Removes currencies affecting all players</li>
	 *   <li>{@link #EDIT} - Modifies currency properties affecting all users</li>
	 * </ul>
	 *
	 * <p><strong>Non-Administrative Permissions:</strong>
	 * <ul>
	 *   <li>{@link #CURRENCIES} - Basic command access</li>
	 *   <li>{@link #OVERVIEW} - Read-only information access</li>
	 * </ul>
	 *
	 * @return true if this permission requires administrative privileges, false otherwise
	 */
	public boolean isAdministrativePermission() {
		return this == CREATE ||
		       this == DELETE ||
		       this == EDIT;
	}
	
	/**
	 * Determines whether this permission represents a destructive operation.
 *
 * <p>This method identifies permissions that allow operations which permanently
	 * modify or remove data from the system. Destructive permissions require
	 * special handling in user interfaces and security systems.
	 *
	 * <p><strong>Destructive Permissions:</strong>
	 * <ul>
	 *   <li>{@link #DELETE} - Permanently removes currencies and associated data</li>
	 * </ul>
	 *
	 * <p><strong>Non-Destructive Permissions:</strong>
	 * <ul>
	 *   <li>{@link #CREATE} - Adds new data without removing existing data</li>
	 *   <li>{@link #EDIT} - Modifies existing data but preserves core structure</li>
	 *   <li>{@link #CURRENCIES} - Basic access with no data changes</li>
	 *   <li>{@link #OVERVIEW} - Read-only operation with no modifications</li>
	 * </ul>
	 *
	 * @return true if this permission allows destructive operations, false otherwise
	 */
	public boolean isDestructivePermission() {
		return this == DELETE;
	}
}
