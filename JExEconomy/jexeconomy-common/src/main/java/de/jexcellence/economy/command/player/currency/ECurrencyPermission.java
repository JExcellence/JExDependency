package de.jexcellence.economy.command.player.currency;

import de.jexcellence.economy.command.player.currency.ECurrencyAction;
import de.jexcellence.evaluable.section.IPermissionNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enumeration defining permission nodes for individual currency command operations within the JExEconomy system.
 *
 * <p>This enum provides a comprehensive permission framework for controlling access to individual
 * currency operations that players can perform. Unlike the broader currencies management system,
 * this enum focuses on permissions for single-currency operations and player-specific currency
 * interactions, enabling fine-grained control over currency access and modification rights.
 *
 * <p><strong>Permission Hierarchy:</strong>
 * <ul>
 *   <li><strong>Base Access:</strong> {@link #CURRENCY} - Basic individual currency command access</li>
 *   <li><strong>Cross-Player Operations:</strong> {@link #CURRENCY_OTHER} - Access to other players' currency data</li>
 * </ul>
 *
 * <p><strong>Security Model:</strong>
 * <ul>
 *   <li><strong>Player-Centric Design:</strong> Permissions focused on individual player currency operations</li>
 *   <li><strong>Privacy Protection:</strong> Separate permissions for accessing other players' data</li>
 *   <li><strong>Hierarchical Structure:</strong> Permissions follow a logical access control hierarchy</li>
 *   <li><strong>Framework Integration:</strong> Implements IPermissionNode for seamless integration</li>
 * </ul>
 *
 * <p><strong>Usage Patterns:</strong>
 * <ul>
 *   <li>Permission validation before individual currency operations</li>
 *   <li>Access control for player currency viewing and modification</li>
 *   <li>Administrative interface permission checks</li>
 *   <li>Cross-player currency operation authorization</li>
 * </ul>
 *
 * <p><strong>Integration Points:</strong>
 * <ul>
 *   <li>Individual currency command framework integration</li>
 *   <li>Player-specific currency user interfaces</li>
 *   <li>Currency balance and transaction systems</li>
 *   <li>Administrative tools and monitoring systems</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 * @see IPermissionNode
 * @see ECurrencyAction
 */
public enum ECurrencyPermission implements IPermissionNode {
	
	/**
	 * Base permission node for accessing individual currency commands and functionality.
 *
 * <p>This permission grants basic access to individual currency command operations,
	 * allowing players to execute currency-specific commands and access their own
	 * currency information. It serves as the foundation permission for all individual
	 * currency operations and player-specific currency interactions.
	 *
	 * <p><strong>Access Granted:</strong>
	 * <ul>
	 *   <li>Basic individual currency command execution</li>
	 *   <li>Access to personal currency information and balances</li>
	 *   <li>Currency-specific help and documentation</li>
	 *   <li>Personal currency transaction history viewing</li>
	 * </ul>
	 *
	 * <p><strong>Security Level:</strong>
	 * <ul>
	 *   <li><strong>Risk Level:</strong> Low - Limited to personal currency data access</li>
	 *   <li><strong>User Type:</strong> General players and administrators</li>
	 *   <li><strong>Scope:</strong> Personal currency data only</li>
	 *   <li><strong>Impact:</strong> No system-wide modifications possible</li>
	 * </ul>
	 *
	 * <p><strong>Typical Operations:</strong>
	 * <ul>
	 *   <li>Viewing personal currency balances</li>
	 *   <li>Accessing currency-specific information</li>
	 *   <li>Reviewing personal transaction history</li>
	 *   <li>Using currency-specific help commands</li>
	 * </ul>
	 */
	CURRENCY(
		"command",
		"currency.command"
	),
	
	/**
	 * Permission node for accessing and viewing other players' currency information.
 *
 * <p>This permission grants the ability to view and potentially interact with
	 * other players' currency data, including balances, transaction histories,
	 * and currency-specific information. This is a higher-privilege permission
	 * typically reserved for administrators and trusted staff members.
	 *
	 * <p><strong>Access Granted:</strong>
	 * <ul>
	 *   <li>Viewing other players' currency balances</li>
	 *   <li>Accessing other players' transaction histories</li>
	 *   <li>Monitoring currency usage across players</li>
	 *   <li>Administrative currency oversight functions</li>
	 * </ul>
	 *
	 * <p><strong>Security Level:</strong>
	 * <ul>
	 *   <li><strong>Risk Level:</strong> Medium - Access to sensitive player financial data</li>
	 *   <li><strong>User Type:</strong> Administrators, moderators, and trusted staff</li>
	 *   <li><strong>Scope:</strong> Cross-player currency data access</li>
	 *   <li><strong>Impact:</strong> Potential privacy implications for player data</li>
	 * </ul>
	 *
	 * <p><strong>Privacy Considerations:</strong>
	 * <ul>
	 *   <li>Should be granted only to trusted users</li>
	 *   <li>Access should be logged for audit purposes</li>
	 *   <li>Consider implementing additional confirmation for sensitive operations</li>
	 *   <li>Regular review of permission holders recommended</li>
	 * </ul>
	 *
	 * <p><strong>Prerequisites:</strong>
	 * <ul>
	 *   <li>Typically requires {@link #CURRENCY} base permission</li>
	 *   <li>Administrative or moderator role recommended</li>
	 *   <li>Understanding of privacy and data protection policies</li>
	 * </ul>
	 */
	CURRENCY_OTHER(
		"commandOther",
		"currency.command.other"
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
	 * Both identifiers are required and must be non-null for proper permission system
	 * integration and functionality.
	 *
	 * @param permissionInternalIdentifier the internal identifier used by the evaluation framework, must not be null
	 * @param fallbackPermissionNode the fallback permission string for external systems, must not be null
	 * @throws IllegalArgumentException if either parameter is null
	 */
	ECurrencyPermission(
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
	 * for configuration mapping, permission resolution, and system integration
	 * within the JExEconomy plugin's permission framework.
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
	 * internal permission system is not available. This ensures compatibility
	 * with various permission management plugins and systems.
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
	 * matching permission if found, or empty if no match exists. This method
	 * is useful for configuration parsing and permission resolution.
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
	public static @NotNull Optional<ECurrencyPermission> fromInternalName(final @Nullable String internalIdentifier) {
		if (
			internalIdentifier == null ||
			internalIdentifier.trim().isEmpty()
		) {
			return Optional.empty();
		}
		
		return Arrays.stream(ECurrencyPermission.values())
		             .filter(currencyPermission -> currencyPermission.getInternalName().equals(internalIdentifier.trim()))
		             .findFirst();
	}
	
	/**
	 * Attempts to resolve a currency permission from its fallback node string.
 *
 * <p>This method provides case-sensitive lookup of currency permissions based on
	 * their fallback permission node strings. This is useful for integration with
	 * external permission systems that use standardized permission strings and
	 * for configuration systems that reference fallback nodes.
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
	public static @NotNull Optional<ECurrencyPermission> fromFallbackNode(final @Nullable String fallbackNodeString) {
		if (
			fallbackNodeString == null ||
			fallbackNodeString.trim().isEmpty()
		) {
			return Optional.empty();
		}
		
		return Arrays.stream(ECurrencyPermission.values())
		             .filter(currencyPermission -> currencyPermission.getFallbackNode().equals(fallbackNodeString.trim()))
		             .findFirst();
	}
	
	/**
	 * Determines whether this permission involves access to other players' data.
 *
 * <p>This method identifies permissions that allow access to other players' currency
	 * information, which has privacy and security implications. Such permissions
	 * typically require higher trust levels and should be granted carefully with
	 * appropriate oversight and logging.
	 *
	 * <p><strong>Cross-Player Permissions:</strong>
	 * <ul>
	 *   <li>{@link #CURRENCY_OTHER} - Allows access to other players' currency data</li>
	 * </ul>
	 *
	 * <p><strong>Personal Permissions:</strong>
	 * <ul>
	 *   <li>{@link #CURRENCY} - Limited to personal currency data access</li>
	 * </ul>
	 *
	 * <p><strong>Security Implications:</strong>
	 * <ul>
	 *   <li>Cross-player permissions should be logged for audit purposes</li>
	 *   <li>Regular review of permission holders is recommended</li>
	 *   <li>Additional confirmation may be required for sensitive operations</li>
	 *   <li>Privacy policies should address cross-player data access</li>
	 * </ul>
	 *
	 * @return true if this permission allows access to other players' data, false otherwise
	 */
	public boolean allowsCrossPlayerAccess() {
		return this == CURRENCY_OTHER;
	}
	
	/**
	 * Determines whether this permission is suitable for general player access.
 *
 * <p>This method identifies permissions that can be safely granted to general
	 * players without significant security or privacy concerns. These permissions
	 * typically involve access to personal data only and do not affect other
	 * players or system-wide settings.
	 *
	 * <p><strong>General Player Permissions:</strong>
	 * <ul>
	 *   <li>{@link #CURRENCY} - Safe for general player access to personal data</li>
	 * </ul>
	 *
	 * <p><strong>Restricted Permissions:</strong>
	 * <ul>
	 *   <li>{@link #CURRENCY_OTHER} - Should be restricted to trusted users</li>
	 * </ul>
	 *
	 * <p><strong>Permission Granting Guidelines:</strong>
	 * <ul>
	 *   <li>General player permissions can be granted by default</li>
	 *   <li>Restricted permissions require manual review and approval</li>
	 *   <li>Consider role-based permission assignment for efficiency</li>
	 *   <li>Regular audit of permission assignments recommended</li>
	 * </ul>
	 *
	 * @return true if this permission is suitable for general players, false if it should be restricted
	 */
	public boolean isSuitableForGeneralPlayers() {
		return this == CURRENCY;
	}
}
