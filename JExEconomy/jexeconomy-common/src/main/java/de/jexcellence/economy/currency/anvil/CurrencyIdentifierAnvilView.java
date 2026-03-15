package de.jexcellence.economy.currency.anvil;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.AbstractAnvilView;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.currency.CurrenciesCreatingView;
import de.jexcellence.economy.database.entity.Currency;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Anvil view for configuring a currency's unique identifier.
 *
 * <p>This view provides an interactive interface for players to input and validate
 * a new identifier for a currency. The identifier serves as the primary key
 * for currency identification throughout the system and must meet strict
 * validation criteria to ensure system integrity.
 *
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li><strong>Pattern Validation:</strong> Ensures identifiers contain only allowed characters</li>
 *   <li><strong>Length Validation:</strong> Enforces minimum and maximum length requirements</li>
 *   <li><strong>Uniqueness Checking:</strong> Prevents duplicate identifiers in the system</li>
 *   <li><strong>Real-time Feedback:</strong> Provides immediate validation feedback to users</li>
 *   <li><strong>Current Value Display:</strong> Shows the existing identifier for reference</li>
 * </ul>
 *
 * <p><strong>Validation Rules:</strong>
 * <ul>
 *   <li><strong>Characters:</strong> Only alphanumeric characters, underscores, and hyphens allowed</li>
 *   <li><strong>Length:</strong> Must be between 2 and 16 characters inclusive</li>
 *   <li><strong>Uniqueness:</strong> Must not match any existing currency identifier</li>
 *   <li><strong>Pattern:</strong> Matches regex pattern ^[a-zA-Z0-9_-]+$</li>
 * </ul>
 *
 * <p><strong>Examples:</strong>
 * <ul>
 *   <li><strong>Valid:</strong> gold, coins, player_points, server-tokens, USD123</li>
 *   <li><strong>Invalid:</strong> a (too short), spaces not allowed, special@chars, toolongidentifier</li>
 * </ul>
 *
 * @author JExcellence
 * @see AbstractAnvilView
 * @see Currency
 * @see CurrenciesCreatingView
 */
public class CurrencyIdentifierAnvilView extends AbstractAnvilView {
	
	/**
	 * Regular expression pattern for valid identifier characters.
	 * Only allows alphanumeric characters, underscores, and hyphens.
	 */
	private static final Pattern IDENTIFIER_VALIDATION_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
	
	/**
	 * Minimum allowed length for currency identifiers.
	 */
	private static final int MINIMUM_IDENTIFIER_LENGTH = 2;
	
	/**
	 * Maximum allowed length for currency identifiers.
	 */
	private static final int MAXIMUM_IDENTIFIER_LENGTH = 16;
	
	/**
	 * State holder for the main JExEconomy plugin instance.
	 */
	private final State<JExEconomy> jexEconomy = initialState("plugin");
	
	/**
	 * State holder for the currency being configured.
	 */
	private final State<Currency> targetCurrency = initialState("currency");
	
	/**
	 * Constructs a new {@code CurrencyIdentifierAnvilView} with the currencies creating view as parent.
 *
 * <p>The view will return to the currencies creating view when the identifier configuration
	 * is completed or cancelled.
	 */
	public CurrencyIdentifierAnvilView() {
		super(CurrenciesCreatingView.class);
	}
	
	/**
	 * Returns the internationalization key for this view.
 *
 * <p>This key is used to load localized strings for the currency identifier configuration
	 * interface, including titles, labels, and error messages.
	 *
	 * @return the i18n key for the currency identifier anvil UI
	 */
	@Override
	protected @NotNull String getKey() {
		return "currency_identifier_anvil_ui";
	}
	
	/**
	 * Processes the user input to set the currency's identifier.
 *
 * <p>This method validates the input identifier and updates the currency's
	 * identifier property. If no currency exists, a new currency instance is created
	 * with the specified identifier.
	 *
	 * <p><strong>Processing Steps:</strong>
	 * <ol>
	 *   <li>Check if a currency instance exists</li>
	 *   <li>Update existing currency or create new one with the identifier</li>
	 *   <li>Return the updated currency instance</li>
	 * </ol>
	 *
	 * @param userInput the identifier entered by the user, must not be null
	 * @param processingContext the interaction context, must not be null
	 * @return the updated currency instance with the new identifier
	 */
	@Override
	protected @NotNull Object processInput(
		final @NotNull String userInput,
		final @NotNull Context processingContext
	) {
		final Currency existingCurrency = this.targetCurrency.get(processingContext);
		
		if (
			existingCurrency == null
		) {
			return new Currency(
				"",
				"",
				userInput,
				"",
				Material.GOLD_INGOT
			);
		}
		
		existingCurrency.setIdentifier(userInput);
		return existingCurrency;
	}
	
	/**
	 * Provides the initial input text for the anvil interface.
 *
 * <p>This method returns the current identifier if available,
	 * or falls back to the default behavior if no identifier exists.
	 *
	 * @param openContext the context when the view is opened, must not be null
	 * @return the initial input text, never null
	 */
	@Override
	protected @NotNull String getInitialInputText(
		final @NotNull OpenContext openContext
	) {
		final Currency existingCurrency = this.targetCurrency.get(openContext);
		
		if (
			existingCurrency != null
		) {
			return existingCurrency.getIdentifier();
		}
		
		return super.getInitialInputText(openContext);
	}
	
	/**
	 * Validates the user input for identifier correctness and uniqueness.
 *
 * <p>This method performs comprehensive validation to ensure the input
	 * meets all requirements for a valid currency identifier.
	 *
	 * <p><strong>Validation Steps:</strong>
	 * <ol>
	 *   <li>Check basic input validity (not empty)</li>
	 *   <li>Verify pattern matches allowed characters</li>
	 *   <li>Validate length requirements</li>
	 *   <li>Ensure identifier uniqueness in the system</li>
	 * </ol>
	 *
	 * @param userInput the input string to validate, must not be null
	 * @param validationContext the validation context, must not be null
	 * @return true if the input is valid, false otherwise
	 */
	@Override
	protected boolean isValidInput(
		final @NotNull String userInput,
		final @NotNull Context validationContext
	) {
		if (
			!super.isValidInput(userInput, validationContext)
		) {
			return false;
		}
		
		if (
			!IDENTIFIER_VALIDATION_PATTERN.matcher(userInput).matches()
		) {
			return false;
		}
		
		if (
			userInput.length() < MINIMUM_IDENTIFIER_LENGTH ||
			userInput.length() > MAXIMUM_IDENTIFIER_LENGTH
		) {
			return false;
		}
		
		return !this.isIdentifierAlreadyTaken(userInput, validationContext);
	}
	
	/**
	 * Configures the first slot (input slot) with appropriate visual elements.
 *
 * <p>This method sets up the input slot with a name tag icon and localized
	 * text to guide the user in entering a valid identifier.
	 *
	 * @param renderContext the rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	@Override
	protected void setupFirstSlot(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		final ItemStack inputSlotItem = UnifiedBuilderFactory
			                                .item(Material.NAME_TAG)
			                                .setName(
				                                this.i18n(
					                                "input.name",
					                                contextPlayer
				                                ).build().component()
			                                )
			                                .setLore(
				                                this.i18n(
					                                "input.lore",
					                                contextPlayer
				                                ).withPlaceholders(
					                                Map.of(
						                                "min_length",
						                                MINIMUM_IDENTIFIER_LENGTH,
						                                "max_length",
						                                MAXIMUM_IDENTIFIER_LENGTH,
						                                "pattern",
						                                "a-z, A-Z, 0-9, _, -"
					                                )
				                                ).build().children()
			                                )
			                                .build();
		
		renderContext.firstSlot(inputSlotItem);
	}
	
	/**
	 * Handles validation failure scenarios with specific error messages.
 *
 * <p>This method provides detailed error feedback based on the specific
	 * validation failure, helping users understand what went wrong and
	 * how to correct their input.
	 *
	 * <p><strong>Error Categories:</strong>
	 * <ul>
	 *   <li><strong>Empty Input:</strong> No identifier provided</li>
	 *   <li><strong>Invalid Characters:</strong> Contains disallowed characters</li>
	 *   <li><strong>Too Short:</strong> Below minimum length requirement</li>
	 *   <li><strong>Too Long:</strong> Exceeds maximum length requirement</li>
	 *   <li><strong>Already Defined:</strong> Identifier already exists in system</li>
	 * </ul>
	 *
	 * @param invalidInput the input that failed validation, can be null
	 * @param validationContext the validation context, must not be null
	 */
	@Override
	protected void onValidationFailed(
		final @Nullable String invalidInput,
		final @NotNull Context validationContext
	) {
		String specificErrorKey = this.getValidationErrorKey();
		
		if (
			invalidInput == null ||
			invalidInput.trim().isEmpty()
		) {
			specificErrorKey = specificErrorKey + ".empty";
		} else if (
			       !IDENTIFIER_VALIDATION_PATTERN.matcher(invalidInput).matches()
		) {
			specificErrorKey = specificErrorKey + ".invalid_characters";
		} else if (
			       invalidInput.length() < MINIMUM_IDENTIFIER_LENGTH
		) {
			specificErrorKey = specificErrorKey + ".too_short";
		} else if (
			       invalidInput.length() > MAXIMUM_IDENTIFIER_LENGTH
		) {
			specificErrorKey = specificErrorKey + ".too_long";
		} else if (
			       this.isIdentifierAlreadyTaken(invalidInput, validationContext)
		) {
			specificErrorKey = specificErrorKey + ".already_defined";
		}
		
		this.i18n(
			    specificErrorKey,
			    validationContext.getPlayer()
		    )
		    .includePrefix()
		    .withPlaceholders(
			    Map.of(
				    "input",
				    invalidInput != null ? invalidInput : "not_defined",
				    "min_length",
				    MINIMUM_IDENTIFIER_LENGTH,
				    "max_length",
				    MAXIMUM_IDENTIFIER_LENGTH,
				    "pattern",
				    "a-z, A-Z, 0-9, _, -"
			    )
		    )
		    .build().sendMessage();
	}
	
	/**
	 * Prepares the result data to pass back to the parent view.
 *
 * <p>This method extends the base result preparation by adding the plugin
	 * instance and updated currency to the result data, ensuring the parent
	 * view has all necessary information to continue the workflow.
	 *
	 * @param processingResult the result from input processing, can be null
	 * @param originalInput the user's original input, must not be null
	 * @param resultContext the processing context, must not be null
	 * @return a map containing all result data for the parent view, never null
	 */
	@Override
	protected @NotNull Map<String, Object> prepareResultData(
		final @Nullable Object processingResult,
		final @NotNull String originalInput,
		final @NotNull Context resultContext
	) {
		final Map<String, Object> resultData = super.prepareResultData(
			processingResult,
			originalInput,
			resultContext
		);
		
		resultData.put(
			"plugin",
			this.jexEconomy.get(resultContext)
		);
		resultData.put(
			"currency",
			processingResult
		);
		
		return resultData;
	}
	
	/**
	 * Checks if the given identifier is already taken by another currency.
 *
 * <p>This method searches through all existing currencies to determine
	 * if the proposed identifier conflicts with an existing one.
	 *
	 * @param proposedIdentifier the identifier to check for uniqueness, must not be null
	 * @param checkingContext the context for accessing currency data, must not be null
	 * @return true if the identifier is already taken, false if it's unique
	 */
	private boolean isIdentifierAlreadyTaken(
		final @NotNull String proposedIdentifier,
		final @NotNull Context checkingContext
	) {
		return this.jexEconomy.get(checkingContext)
		                              .getCurrencies()
		                              .values()
		                              .stream()
		                              .anyMatch(existingCurrency ->
			                                        existingCurrency.getIdentifier().equals(proposedIdentifier)
		                              );
	}
}
