package de.jexcellence.economy.command.player.currencies;

import de.jexcellence.economy.command.player.currencies.CurrencyCommandHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enumeration defining the complete set of currency management actions available to players.
 * <p>
 * This enum serves as a comprehensive catalog of all operations that can be performed
 * through the currency command system. Each action represents a distinct functional
 * area within the currency management framework, providing clear separation of concerns
 * and enabling structured command processing.
 * </p>
 *
 * <h3>Action Categories:</h3>
 * <ul>
 *   <li><strong>Creation Operations:</strong> Actions for creating new currency entities</li>
 *   <li><strong>Modification Operations:</strong> Actions for editing existing currency properties</li>
 *   <li><strong>Deletion Operations:</strong> Actions for removing currencies from the system</li>
 *   <li><strong>Information Operations:</strong> Actions for retrieving currency data and details</li>
 *   <li><strong>Utility Operations:</strong> Actions for help, overview, and system navigation</li>
 * </ul>
 *
 * <h3>Command Integration:</h3>
 * <p>
 * This enum is designed to work seamlessly with the command parsing system, allowing
 * for type-safe action identification and validation. Each enum constant corresponds
 * to a specific subcommand that players can execute through the currency command interface.
 * </p>
 *
 * <h3>Usage Patterns:</h3>
 * <ul>
 *   <li>Command parsers use this enum to validate and categorize user input</li>
 *   <li>Command handlers dispatch operations based on the identified action</li>
 *   <li>Tab completion systems reference these actions for auto-completion</li>
 *   <li>Help systems enumerate available actions for user guidance</li>
 * </ul>
 *
 * <h3>Design Principles:</h3>
 * <ul>
 *   <li><strong>Clarity:</strong> Each action has a clear, unambiguous purpose</li>
 *   <li><strong>Completeness:</strong> Covers all essential currency management operations</li>
 *   <li><strong>Consistency:</strong> Follows consistent naming conventions</li>
 *   <li><strong>Extensibility:</strong> New actions can be added without breaking existing code</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 * @see CurrencyCommandHandler
 */
public enum ECurrenciesAction {
	
	/**
	 * Action for creating new currency entities within the system.
	 * <p>
	 * This action enables administrators to define new currencies with custom
	 * properties including identifiers, symbols, prefixes, and suffixes. The
	 * creation process includes validation to ensure uniqueness and data integrity.
	 * </p>
	 *
	 * <h3>Operation Details:</h3>
	 * <ul>
	 *   <li>Validates currency identifier uniqueness</li>
	 *   <li>Creates currency entity with specified properties</li>
	 *   <li>Initializes player accounts for all existing users</li>
	 *   <li>Persists currency data to the database</li>
	 * </ul>
	 *
	 * <h3>Required Parameters:</h3>
	 * <ul>
	 *   <li><strong>identifier:</strong> Unique currency identifier</li>
	 *   <li><strong>symbol:</strong> Currency symbol for display</li>
	 *   <li><strong>prefix:</strong> Optional prefix for currency amounts</li>
	 *   <li><strong>suffix:</strong> Optional suffix for currency amounts</li>
	 * </ul>
	 */
	CREATE,
	
	/**
	 * Action for removing existing currency entities from the system.
	 * <p>
	 * This action allows administrators to permanently delete currencies from
	 * the database. The operation includes comprehensive cleanup of related
	 * data and validation to prevent accidental deletions.
	 * </p>
	 *
	 * <h3>Operation Details:</h3>
	 * <ul>
	 *   <li>Validates currency existence before deletion</li>
	 *   <li>Removes currency entity from database</li>
	 *   <li>Cleans up associated player accounts</li>
	 *   <li>Updates system cache and references</li>
	 * </ul>
	 *
	 * <h3>Required Parameters:</h3>
	 * <ul>
	 *   <li><strong>identifier:</strong> Identifier of currency to delete</li>
	 * </ul>
	 *
	 * <h3>Security Considerations:</h3>
	 * <ul>
	 *   <li>Requires appropriate administrative permissions</li>
	 *   <li>Operation is irreversible and affects all player balances</li>
	 *   <li>Comprehensive logging for audit purposes</li>
	 * </ul>
	 */
	DELETE,
	
	/**
	 * Action for modifying properties of existing currency entities.
	 * <p>
	 * This action enables administrators to update currency properties such as
	 * symbols, prefixes, suffixes, and identifiers. The editing process includes
	 * validation to maintain data integrity and prevent conflicts.
	 * </p>
	 *
	 * <h3>Editable Properties:</h3>
	 * <ul>
	 *   <li><strong>symbol:</strong> Currency symbol displayed in transactions</li>
	 *   <li><strong>prefix:</strong> Text displayed before currency amounts</li>
	 *   <li><strong>suffix:</strong> Text displayed after currency amounts</li>
	 *   <li><strong>identifier:</strong> Unique currency identifier (with uniqueness validation)</li>
	 * </ul>
	 *
	 * <h3>Required Parameters:</h3>
	 * <ul>
	 *   <li><strong>identifier:</strong> Current currency identifier</li>
	 *   <li><strong>field:</strong> Property name to modify</li>
	 *   <li><strong>value:</strong> New value for the specified property</li>
	 * </ul>
	 *
	 * <h3>Validation Rules:</h3>
	 * <ul>
	 *   <li>Currency must exist in the system</li>
	 *   <li>Field name must be valid and editable</li>
	 *   <li>New identifier values must be unique</li>
	 *   <li>Changes are persisted atomically</li>
	 * </ul>
	 */
	EDIT,
	
	/**
	 * Action for displaying comprehensive information about specific currencies.
	 * <p>
	 * This action provides detailed information about a currency including all
	 * its properties, configuration settings, and usage statistics. The information
	 * is formatted for easy reading and administrative review.
	 * </p>
	 *
	 * <h3>Displayed Information:</h3>
	 * <ul>
	 *   <li><strong>Basic Properties:</strong> Identifier, symbol, prefix, suffix</li>
	 *   <li><strong>Configuration:</strong> Display settings and formatting rules</li>
	 *   <li><strong>Statistics:</strong> Usage metrics and player account information</li>
	 *   <li><strong>Metadata:</strong> Creation date and modification history</li>
	 * </ul>
	 *
	 * <h3>Required Parameters:</h3>
	 * <ul>
	 *   <li><strong>identifier:</strong> Identifier of currency to display information for</li>
	 * </ul>
	 *
	 * <h3>Use Cases:</h3>
	 * <ul>
	 *   <li>Administrative review of currency configuration</li>
	 *   <li>Troubleshooting currency-related issues</li>
	 *   <li>Verification of currency properties before editing</li>
	 *   <li>Documentation and audit purposes</li>
	 * </ul>
	 */
	INFO,
	
	/**
	 * Action for displaying help information and usage guidance for currency commands.
	 * <p>
	 * This action provides comprehensive help documentation including command syntax,
	 * parameter descriptions, usage examples, and troubleshooting information. It
	 * serves as the primary reference for users learning the currency command system.
	 * </p>
	 *
	 * <h3>Help Content:</h3>
	 * <ul>
	 *   <li><strong>Command Syntax:</strong> Proper format for each currency command</li>
	 *   <li><strong>Parameter Descriptions:</strong> Detailed explanation of required and optional parameters</li>
	 *   <li><strong>Usage Examples:</strong> Practical examples demonstrating common operations</li>
	 *   <li><strong>Error Resolution:</strong> Common error scenarios and solutions</li>
	 * </ul>
	 *
	 * <h3>Organization:</h3>
	 * <ul>
	 *   <li>Categorized by operation type (create, edit, delete, etc.)</li>
	 *   <li>Progressive complexity from basic to advanced operations</li>
	 *   <li>Cross-references to related commands and concepts</li>
	 *   <li>Quick reference section for experienced users</li>
	 * </ul>
	 *
	 * <h3>Accessibility:</h3>
	 * <ul>
	 *   <li>Available without parameters for general help</li>
	 *   <li>Supports specific help topics when parameters provided</li>
	 *   <li>Formatted for readability in game chat interface</li>
	 * </ul>
	 */
	HELP,
	
	/**
	 * Action for displaying a comprehensive overview of all available currencies.
	 * <p>
	 * This action provides a structured listing of all currencies currently
	 * registered in the system, including their key properties and status
	 * information. The overview serves as a quick reference for administrators
	 * and users to understand the available currency options.
	 * </p>
	 *
	 * <h3>Overview Content:</h3>
	 * <ul>
	 *   <li><strong>Currency List:</strong> All registered currencies with identifiers</li>
	 *   <li><strong>Key Properties:</strong> Symbol, prefix, suffix for each currency</li>
	 *   <li><strong>Status Information:</strong> Active/inactive status and usage metrics</li>
	 *   <li><strong>Summary Statistics:</strong> Total currency count and system overview</li>
	 * </ul>
	 *
	 * <h3>Display Format:</h3>
	 * <ul>
	 *   <li>Tabular format for easy scanning and comparison</li>
	 *   <li>Sorted by currency identifier for consistent ordering</li>
	 *   <li>Pagination support for systems with many currencies</li>
	 *   <li>Color coding for different currency types or statuses</li>
	 * </ul>
	 *
	 * <h3>Use Cases:</h3>
	 * <ul>
	 *   <li>Administrative review of all system currencies</li>
	 *   <li>Quick reference for currency identifiers and symbols</li>
	 *   <li>System health monitoring and currency auditing</li>
	 *   <li>User education about available currency options</li>
	 * </ul>
	 */
	OVERVIEW;
	
	/**
	 * Attempts to resolve a currency action from a string representation.
	 * <p>
	 * This method provides case-insensitive lookup of currency actions based on
	 * string input, typically from command arguments. It returns an Optional
	 * containing the matching action if found, or empty if no match exists.
	 * </p>
	 *
	 * <h3>Matching Behavior:</h3>
	 * <ul>
	 *   <li>Case-insensitive comparison for user convenience</li>
	 *   <li>Exact string matching against enum constant names</li>
	 *   <li>Returns Optional.empty() for null or invalid input</li>
	 *   <li>Supports all defined action constants</li>
	 * </ul>
	 *
	 * <h3>Usage Examples:</h3>
	 * <pre>{@code
	 * Optional<ECurrenciesAction> action = ECurrenciesAction.fromString("create");
	 * if (action.isPresent()) {
	 *     // Handle the CREATE action
	 * }
	 *
	 * // Case-insensitive matching
	 * ECurrenciesAction.fromString("CREATE");  // Returns Optional.of(CREATE)
	 * ECurrenciesAction.fromString("Create");  // Returns Optional.of(CREATE)
	 * ECurrenciesAction.fromString("invalid"); // Returns Optional.empty()
	 * }</pre>
	 *
	 * @param actionString the string representation of the action to resolve, may be null
	 * @return an Optional containing the matching ECurrenciesAction if found, empty otherwise
	 */
	public static @NotNull Optional<ECurrenciesAction> fromString(final @Nullable String actionString) {
		if (
			actionString == null ||
			actionString.trim().isEmpty()
		) {
			return Optional.empty();
		}
		
		return Arrays.stream(ECurrenciesAction.values())
		             .filter(currencyAction -> currencyAction.name().equalsIgnoreCase(actionString.trim()))
		             .findFirst();
	}
	
	/**
	 * Retrieves the lowercase string representation of this action.
	 * <p>
	 * This method provides a standardized lowercase string representation of the
	 * action, suitable for display purposes, command parsing, and user interfaces.
	 * The lowercase format is consistent with common command-line conventions.
	 * </p>
	 *
	 * <h3>Usage Examples:</h3>
	 * <pre>{@code
	 * ECurrenciesAction.CREATE.getActionName();   // Returns "create"
	 * ECurrenciesAction.DELETE.getActionName();   // Returns "delete"
	 * ECurrenciesAction.OVERVIEW.getActionName(); // Returns "overview"
	 * }</pre>
	 *
	 * <h3>Common Use Cases:</h3>
	 * <ul>
	 *   <li>Display in user interfaces and help messages</li>
	 *   <li>Command parsing and validation</li>
	 *   <li>Logging and debugging output</li>
	 *   <li>Configuration file generation</li>
	 * </ul>
	 *
	 * @return the lowercase string representation of this action, never null
	 */
	public @NotNull String getActionName() {
		return this.name().toLowerCase();
	}
	
	/**
	 * Determines whether this action represents a destructive operation.
	 * <p>
	 * This method identifies actions that permanently modify or remove data
	 * from the system, requiring additional confirmation or elevated permissions.
	 * Destructive operations typically require special handling in user interfaces
	 * and security systems.
	 * </p>
	 *
	 * <h3>Destructive Actions:</h3>
	 * <ul>
	 *   <li><strong>DELETE:</strong> Permanently removes currencies and associated data</li>
	 * </ul>
	 *
	 * <h3>Non-Destructive Actions:</h3>
	 * <ul>
	 *   <li><strong>CREATE:</strong> Adds new data without removing existing data</li>
	 *   <li><strong>EDIT:</strong> Modifies existing data but preserves core structure</li>
	 *   <li><strong>INFO:</strong> Read-only operation with no data changes</li>
	 *   <li><strong>HELP:</strong> Read-only operation providing documentation</li>
	 *   <li><strong>OVERVIEW:</strong> Read-only operation listing existing data</li>
	 * </ul>
	 *
	 * <h3>Security Implications:</h3>
	 * <ul>
	 *   <li>Destructive actions may require additional confirmation prompts</li>
	 *   <li>Enhanced logging and audit trails for destructive operations</li>
	 *   <li>Elevated permission requirements for destructive actions</li>
	 *   <li>Backup and recovery considerations for destructive operations</li>
	 * </ul>
	 *
	 * @return true if this action permanently modifies or removes system data, false otherwise
	 */
	public boolean isDestructiveAction() {
		return this == DELETE;
	}
	
	/**
	 * Determines whether this action requires administrative privileges.
	 * <p>
	 * This method identifies actions that should be restricted to users with
	 * administrative permissions due to their system-wide impact or security
	 * implications. Administrative actions typically affect global system state
	 * or other players' data.
	 * </p>
	 *
	 * <h3>Administrative Actions:</h3>
	 * <ul>
	 *   <li><strong>CREATE:</strong> Creates new system-wide currencies</li>
	 *   <li><strong>DELETE:</strong> Removes currencies affecting all players</li>
	 *   <li><strong>EDIT:</strong> Modifies currency properties affecting all users</li>
	 * </ul>
	 *
	 * <h3>Non-Administrative Actions:</h3>
	 * <ul>
	 *   <li><strong>INFO:</strong> Read-only information access</li>
	 *   <li><strong>HELP:</strong> Documentation and guidance access</li>
	 *   <li><strong>OVERVIEW:</strong> System overview and listing access</li>
	 * </ul>
	 *
	 * <h3>Permission Considerations:</h3>
	 * <ul>
	 *   <li>Administrative actions require specific permission nodes</li>
	 *   <li>Permission checks should be performed before action execution</li>
	 *   <li>Unauthorized access attempts should be logged</li>
	 *   <li>Clear error messages for permission-denied scenarios</li>
	 * </ul>
	 *
	 * @return true if this action requires administrative privileges, false otherwise
	 */
	public boolean requiresAdministrativePrivileges() {
		return this == CREATE ||
		       this == DELETE ||
		       this == EDIT;
	}
}