package de.jexcellence.economy.command.player.currency;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enumeration defining the available actions for individual currency management operations.
 * <p>
 * This enum serves as a comprehensive catalog of operations that can be performed on
 * individual currencies through the player currency command system. Unlike the broader
 * currencies management system, this enum focuses on single-currency operations and
 * informational commands that players can execute.
 * </p>
 *
 * <h3>Action Categories:</h3>
 * <ul>
 *   <li><strong>Information Operations:</strong> Actions for retrieving currency data and help</li>
 *   <li><strong>Utility Operations:</strong> Actions for system navigation and user guidance</li>
 * </ul>
 *
 * <h3>Design Philosophy:</h3>
 * <p>
 * This enum is designed to be extensible, allowing for future addition of currency-specific
 * operations such as balance checking, transaction history, or currency-specific settings.
 * The current implementation focuses on essential informational operations while providing
 * a foundation for expanded functionality.
 * </p>
 *
 * <h3>Command Integration:</h3>
 * <ul>
 *   <li>Integrates with player command parsing systems</li>
 *   <li>Provides type-safe action identification</li>
 *   <li>Supports tab completion and validation</li>
 *   <li>Enables structured command processing</li>
 * </ul>
 *
 * <h3>Usage Patterns:</h3>
 * <ul>
 *   <li>Command parsers use this enum to validate and categorize user input</li>
 *   <li>Command handlers dispatch operations based on identified actions</li>
 *   <li>Help systems reference these actions for user guidance</li>
 *   <li>Tab completion systems provide action suggestions</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public enum ECurrencyAction {
	
	/**
	 * Action for displaying comprehensive help information about currency commands.
	 * <p>
	 * This action provides players with detailed guidance on how to use currency-related
	 * commands, including syntax explanations, parameter descriptions, usage examples,
	 * and troubleshooting information. The help system is designed to be accessible
	 * to all players regardless of their permission level.
	 * </p>
	 *
	 * <h3>Help Content Features:</h3>
	 * <ul>
	 *   <li><strong>Command Syntax:</strong> Proper format and structure for currency commands</li>
	 *   <li><strong>Parameter Descriptions:</strong> Detailed explanation of required and optional parameters</li>
	 *   <li><strong>Usage Examples:</strong> Practical examples demonstrating common operations</li>
	 *   <li><strong>Permission Information:</strong> Explanation of required permissions for different operations</li>
	 * </ul>
	 *
	 * <h3>Accessibility:</h3>
	 * <ul>
	 *   <li>Available to all players without special permissions</li>
	 *   <li>Provides localized content based on player language preferences</li>
	 *   <li>Formatted for optimal readability in game chat interface</li>
	 *   <li>Includes cross-references to related commands and concepts</li>
	 * </ul>
	 *
	 * <h3>Content Organization:</h3>
	 * <ul>
	 *   <li>Structured by operation complexity from basic to advanced</li>
	 *   <li>Categorized by functional area (viewing, management, administration)</li>
	 *   <li>Includes quick reference section for experienced users</li>
	 *   <li>Provides troubleshooting guidance for common issues</li>
	 * </ul>
	 */
	HELP;
	
	/**
	 * Attempts to resolve a currency action from a string representation.
	 * <p>
	 * This method provides case-insensitive lookup of currency actions based on
	 * string input, typically from command arguments. It returns an Optional
	 * containing the matching action if found, or empty if no match exists.
	 * This method is essential for command parsing and validation.
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
	 * Optional<ECurrencyAction> action = ECurrencyAction.fromString("help");
	 * if (action.isPresent()) {
	 *     // Handle the HELP action
	 * }
	 *
	 * // Case-insensitive matching
	 * ECurrencyAction.fromString("HELP");  // Returns Optional.of(HELP)
	 * ECurrencyAction.fromString("Help");  // Returns Optional.of(HELP)
	 * ECurrencyAction.fromString("invalid"); // Returns Optional.empty()
	 * }</pre>
	 *
	 * <h3>Error Handling:</h3>
	 * <ul>
	 *   <li>Null input returns empty Optional without throwing exceptions</li>
	 *   <li>Empty or whitespace-only strings return empty Optional</li>
	 *   <li>Invalid action names return empty Optional for graceful handling</li>
	 * </ul>
	 *
	 * @param actionString the string representation of the action to resolve, may be null
	 * @return an Optional containing the matching ECurrencyAction if found, empty otherwise
	 */
	public static @NotNull Optional<ECurrencyAction> fromString(final @Nullable String actionString) {
		if (
			actionString == null ||
			actionString.trim().isEmpty()
		) {
			return Optional.empty();
		}
		
		return Arrays.stream(ECurrencyAction.values())
		             .filter(currencyAction -> currencyAction.name().equalsIgnoreCase(actionString.trim()))
		             .findFirst();
	}
	
	/**
	 * Retrieves the lowercase string representation of this action.
	 * <p>
	 * This method provides a standardized lowercase string representation of the
	 * action, suitable for display purposes, command parsing, and user interfaces.
	 * The lowercase format is consistent with common command-line conventions
	 * and provides a user-friendly representation of the action.
	 * </p>
	 *
	 * <h3>Usage Examples:</h3>
	 * <pre>{@code
	 * ECurrencyAction.HELP.getActionName();   // Returns "help"
	 * }</pre>
	 *
	 * <h3>Common Use Cases:</h3>
	 * <ul>
	 *   <li>Display in user interfaces and help messages</li>
	 *   <li>Command parsing and validation feedback</li>
	 *   <li>Logging and debugging output</li>
	 *   <li>Configuration file generation and documentation</li>
	 * </ul>
	 *
	 * <h3>Consistency:</h3>
	 * <p>
	 * The returned string is guaranteed to be lowercase and consistent across
	 * all invocations, making it suitable for use in case-sensitive contexts
	 * such as configuration files or external system integration.
	 * </p>
	 *
	 * @return the lowercase string representation of this action, never null
	 */
	public @NotNull String getActionName() {
		return this.name().toLowerCase();
	}
	
	/**
	 * Determines whether this action requires special permissions to execute.
	 * <p>
	 * This method identifies actions that should be restricted to users with
	 * specific permissions due to their functionality or security implications.
	 * Currently, all defined actions are accessible to general users without
	 * special permissions, but this method provides a framework for future
	 * permission-restricted actions.
	 * </p>
	 *
	 * <h3>Permission Philosophy:</h3>
	 * <ul>
	 *   <li><strong>Open Access:</strong> Help and informational actions are freely accessible</li>
	 *   <li><strong>Future Extensibility:</strong> Framework ready for permission-restricted actions</li>
	 *   <li><strong>Security Awareness:</strong> Provides foundation for access control</li>
	 * </ul>
	 *
	 * <h3>Current Implementation:</h3>
	 * <ul>
	 *   <li><strong>HELP:</strong> No special permissions required - accessible to all players</li>
	 * </ul>
	 *
	 * <h3>Future Considerations:</h3>
	 * <p>
	 * As new actions are added to this enum, this method should be updated to
	 * reflect the appropriate permission requirements. Actions that modify
	 * currency data, access sensitive information, or perform administrative
	 * functions should return true from this method.
	 * </p>
	 *
	 * @return true if this action requires special permissions, false if accessible to all players
	 */
	public boolean requiresSpecialPermissions() {
		return switch (this) {
			case HELP -> false;
		};
	}
	
	/**
	 * Determines whether this action is purely informational without side effects.
	 * <p>
	 * This method identifies actions that only retrieve and display information
	 * without modifying any system state or player data. Informational actions
	 * are typically safe to execute frequently and can be used for monitoring
	 * and debugging purposes without concern for system impact.
	 * </p>
	 *
	 * <h3>Informational Action Characteristics:</h3>
	 * <ul>
	 *   <li><strong>Read-Only:</strong> No modification of system or player data</li>
	 *   <li><strong>Safe Execution:</strong> Can be executed repeatedly without side effects</li>
	 *   <li><strong>Low Impact:</strong> Minimal system resource usage</li>
	 *   <li><strong>Monitoring Friendly:</strong> Suitable for automated monitoring and debugging</li>
	 * </ul>
	 *
	 * <h3>Current Implementation:</h3>
	 * <ul>
	 *   <li><strong>HELP:</strong> Purely informational - displays help content without modifications</li>
	 * </ul>
	 *
	 * <h3>Usage in System Design:</h3>
	 * <ul>
	 *   <li>Caching strategies can be more aggressive for informational actions</li>
	 *   <li>Rate limiting can be more lenient for read-only operations</li>
	 *   <li>Logging levels can be reduced for informational actions</li>
	 *   <li>Permission checks can be simplified for non-modifying operations</li>
	 * </ul>
	 *
	 * @return true if this action is purely informational, false if it modifies system state
	 */
	public boolean isInformationalAction() {
		return switch (this) {
			case HELP -> true;
		};
	}
}