package de.jexcellence.economy.currency.anvil;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.AbstractAnvilView;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.currency.CurrenciesActionOverviewView;
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

/**
 * Anvil view for configuring a currency's suffix text.
 * <p>
 * This view provides an interactive interface for players to input and validate
 * a new suffix for a currency. The suffix is displayed after currency amounts
 * in user interfaces and provides additional context or unit information.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Length Validation:</strong> Ensures suffixes don't exceed maximum length</li>
 *   <li><strong>Optional Input:</strong> Allows empty suffixes for currencies without suffix formatting</li>
 *   <li><strong>Real-time Feedback:</strong> Provides immediate validation feedback to users</li>
 *   <li><strong>Current Value Display:</strong> Shows the existing suffix for reference</li>
 * </ul>
 *
 * <h3>Validation Rules:</h3>
 * <ul>
 *   <li><strong>Length:</strong> Must not exceed 16 characters</li>
 *   <li><strong>Content:</strong> Any characters are allowed including spaces and symbols</li>
 *   <li><strong>Empty Values:</strong> Empty strings are valid and represent no suffix</li>
 * </ul>
 *
 * <h3>Usage Examples:</h3>
 * <ul>
 *   <li><strong>Unit Indicators:</strong> " coins", " points", " gems" for currency units</li>
 *   <li><strong>Contextual:</strong> " (available)", " remaining", " earned" for status information</li>
 *   <li><strong>Empty:</strong> "" for currencies without suffix formatting</li>
 * </ul>
 *
 * @author JExcellence
 * @see AbstractAnvilView
 * @see Currency
 * @see CurrenciesActionOverviewView
 */
public class CurrencySuffixAnvilView extends AbstractAnvilView {
	
	/**
	 * Maximum allowed length for currency suffixes.
	 */
	private static final int MAXIMUM_SUFFIX_LENGTH = 16;
	
	/**
	 * State holder for the main JExEconomy plugin instance.
	 */
	private final State<JExEconomy> jexEconomy = initialState("plugin");
	
	/**
	 * State holder for the currency being configured.
	 */
	private final State<Currency> targetCurrency = initialState("currency");
	
	/**
	 * Constructs a new {@code CurrencySuffixAnvilView} with the currencies action overview as parent.
	 * <p>
	 * The view will return to the currencies action overview when the suffix configuration
	 * is completed or cancelled.
	 * </p>
	 */
	public CurrencySuffixAnvilView() {
		super(CurrenciesActionOverviewView.class);
	}
	
	/**
	 * Returns the internationalization key for this view.
	 * <p>
	 * This key is used to load localized strings for the currency suffix configuration
	 * interface, including titles, labels, and error messages.
	 * </p>
	 *
	 * @return the i18n key for the currency suffix anvil UI
	 */
	@Override
	protected @NotNull String getKey() {
		return "currency_suffix_anvil_ui";
	}
	
	/**
	 * Processes the user input to set the currency's suffix.
	 * <p>
	 * This method validates the input suffix and updates the currency's
	 * suffix property. If no currency exists, a new currency instance is created
	 * with the specified suffix.
	 * </p>
	 *
	 * <h3>Processing Steps:</h3>
	 * <ol>
	 *   <li>Check if a currency instance exists</li>
	 *   <li>Update existing currency or create new one with the suffix</li>
	 *   <li>Return the updated currency instance</li>
	 * </ol>
	 *
	 * @param userInput the suffix entered by the user, must not be null
	 * @param processingContext the interaction context, must not be null
	 * @return the updated currency instance with the new suffix
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
				userInput,
				"",
				"",
				Material.GOLD_INGOT
			);
		}
		
		existingCurrency.setSuffix(userInput);
		return existingCurrency;
	}
	
	/**
	 * Provides title placeholders for dynamic title generation.
	 * <p>
	 * This method returns placeholders that can be used in the view's title
	 * to display contextual information such as the current suffix
	 * and currency identifier.
	 * </p>
	 *
	 * <h3>Available Placeholders:</h3>
	 * <ul>
	 *   <li><strong>current_suffix:</strong> The current suffix text or empty string</li>
	 *   <li><strong>identifier:</strong> The currency identifier or "New Currency"</li>
	 * </ul>
	 *
	 * @param openContext the context when the view is opened, must not be null
	 * @return a map of placeholder keys to their values, never null
	 */
	@Override
	protected @NotNull Map<String, Object> getTitlePlaceholders(
		final @NotNull OpenContext openContext
	) {
		final Currency existingCurrency = this.targetCurrency.get(openContext);
		
		if (
			existingCurrency != null
		) {
			final String currentSuffix = existingCurrency.getSuffix();
			final String currencyIdentifier = existingCurrency.getIdentifier();
			
			return Map.of(
				"current_suffix",
				currentSuffix,
				"identifier",
				currencyIdentifier
			);
		}
		
		return Map.of(
			"current_suffix",
			"",
			"identifier",
			"New Currency"
		);
	}
	
	/**
	 * Provides the initial input text for the anvil interface.
	 * <p>
	 * This method returns the current suffix if available,
	 * or falls back to the default behavior if no suffix exists.
	 * </p>
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
			existingCurrency.getSuffix();
			return existingCurrency.getSuffix();
		}
		
		return super.getInitialInputText(openContext);
	}
	
	/**
	 * Validates the user input for suffix length requirements.
	 * <p>
	 * This method ensures the input suffix meets the length requirements.
	 * Empty suffixes are considered valid as they represent currencies
	 * without suffix formatting.
	 * </p>
	 *
	 * <h3>Validation Criteria:</h3>
	 * <ul>
	 *   <li>Length must not exceed the maximum allowed characters</li>
	 *   <li>Empty strings are valid</li>
	 *   <li>All characters and symbols are allowed</li>
	 * </ul>
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
		return userInput.length() <= MAXIMUM_SUFFIX_LENGTH;
	}
	
	/**
	 * Configures the first slot (input slot) with appropriate visual elements.
	 * <p>
	 * This method sets up the input slot with a paper icon and localized
	 * text to guide the user in entering a valid suffix.
	 * </p>
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
			                                .item(Material.PAPER)
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
				                                ).withPlaceholder(
					                                "max_length",
					                                MAXIMUM_SUFFIX_LENGTH
				                                ).build().children()
			                                )
			                                .build();
		
		renderContext.firstSlot(inputSlotItem);
	}
	
	/**
	 * Handles validation failure scenarios with specific error messages.
	 * <p>
	 * This method provides detailed error feedback based on the specific
	 * validation failure, helping users understand what went wrong and
	 * how to correct their input.
	 * </p>
	 *
	 * <h3>Error Categories:</h3>
	 * <ul>
	 *   <li><strong>Too Long:</strong> Suffix exceeds maximum length</li>
	 *   <li><strong>General:</strong> Other validation failures</li>
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
			invalidInput != null &&
			invalidInput.length() > MAXIMUM_SUFFIX_LENGTH
		) {
			specificErrorKey = specificErrorKey + ".too_long";
		} else {
			specificErrorKey = specificErrorKey + ".general";
		}
		
		this.i18n(
			    specificErrorKey,
			    validationContext.getPlayer()
		    )
		    .includePrefix()
		    .withPlaceholders(
			    Map.of(
				    "input",
				    invalidInput != null ? invalidInput : "",
				    "max_length",
				    MAXIMUM_SUFFIX_LENGTH,
				    "current_length",
				    invalidInput != null ? invalidInput.length() : 0
			    )
		    )
		    .build().sendMessage();
	}
	
	/**
	 * Prepares the result data to pass back to the parent view.
	 * <p>
	 * This method extends the base result preparation by adding the plugin
	 * instance and updated currency to the result data, ensuring the parent
	 * view has all necessary information to continue the workflow.
	 * </p>
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
}