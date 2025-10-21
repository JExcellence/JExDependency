package de.jexcellence.economy.command.console.deposit;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Command section configuration for the console-based currency deposit command.
 * <p>
 * This section serves as the configuration and registration component for the "cdeposit"
 * console command within the JExEconomyImpl plugin's command evaluation framework. It extends
 * the abstract command section to provide specific configuration for deposit operations
 * that can only be executed from the server console.
 * </p>
 *
 * <h3>Command Integration:</h3>
 * <ul>
 *   <li><strong>Command Name:</strong> "cdeposit" - Console-specific deposit command</li>
 *   <li><strong>Execution Context:</strong> Server console only (no player access)</li>
 *   <li><strong>Framework Integration:</strong> Integrates with the GPEEE evaluation system</li>
 *   <li><strong>Configuration Management:</strong> Handles command-specific settings and permissions</li>
 * </ul>
 *
 * <h3>Security Model:</h3>
 * <ul>
 *   <li><strong>Console Restriction:</strong> Command can only be executed from server console</li>
 *   <li><strong>Administrative Access:</strong> Provides server operators with direct currency control</li>
 *   <li><strong>Audit Integration:</strong> All operations are logged for administrative oversight</li>
 * </ul>
 *
 * <h3>Framework Responsibilities:</h3>
 * <ul>
 *   <li>Registers the command with the evaluation environment</li>
 *   <li>Configures command-specific evaluation parameters</li>
 *   <li>Manages command lifecycle within the plugin system</li>
 *   <li>Provides configuration access to the command implementation</li>
 * </ul>
 *
 * <h3>Usage Context:</h3>
 * <p>
 * This section is instantiated during plugin initialization and provides the configuration
 * foundation for the {@link CDeposit} command implementation. It ensures proper integration
 * with the plugin's command evaluation framework and maintains consistency with other
 * console commands in the system.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 * @see ACommandSection
 * @see CDeposit
 * @see EvaluationEnvironmentBuilder
 */
public class CDepositSection extends ACommandSection {
	
	/**
	 * The unique identifier for the console deposit command within the command framework.
	 * <p>
	 * This constant defines the command name that will be registered with the server's
	 * command system and used by administrators to execute deposit operations. The "c"
	 * prefix indicates this is a console-only command, distinguishing it from player
	 * commands that might have similar functionality.
	 * </p>
	 */
	private static final String CONSOLE_DEPOSIT_COMMAND_NAME = "cdeposit";
	
	/**
	 * Constructs a new console deposit command section with the specified evaluation environment.
	 * <p>
	 * This constructor initializes the command section with the necessary configuration to
	 * integrate with the GPEEE evaluation framework. The evaluation environment builder
	 * provides the context and configuration needed for command processing, including
	 * variable resolution, expression evaluation, and execution context management.
	 * </p>
	 *
	 * <h3>Initialization Process:</h3>
	 * <ul>
	 *   <li>Registers the command name with the parent section</li>
	 *   <li>Configures the evaluation environment for command processing</li>
	 *   <li>Sets up command-specific parameters and constraints</li>
	 *   <li>Establishes integration with the plugin's command framework</li>
	 * </ul>
	 *
	 * <h3>Environment Configuration:</h3>
	 * <p>
	 * The evaluation environment builder provides access to plugin services, configuration
	 * settings, and runtime context that the command implementation will need during
	 * execution. This includes access to repositories, adapters, and other infrastructure
	 * components required for currency operations.
	 * </p>
	 *
	 * @param evaluationEnvironmentBuilder the builder used to configure the evaluation environment for command processing, must not be null
	 * @throws IllegalArgumentException if evaluationEnvironmentBuilder is null
	 */
	public CDepositSection(final @NotNull EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
		super(
			CONSOLE_DEPOSIT_COMMAND_NAME,
			evaluationEnvironmentBuilder
		);
	}
}