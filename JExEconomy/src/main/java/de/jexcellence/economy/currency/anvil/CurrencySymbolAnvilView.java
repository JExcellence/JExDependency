package de.jexcellence.economy.currency.anvil;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.common.AbstractAnvilView;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.view.currency.CurrenciesActionOverviewView;
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
 * Anvil view for configuring a currency's visual symbol.
 * <p>
 * This view provides an interactive interface for players to input and validate
 * a new symbol for a currency. The symbol serves as the primary visual identifier
 * for the currency in user interfaces and should be concise and recognizable.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Length Validation:</strong> Ensures symbols are concise and don't exceed maximum length</li>
 *   <li><strong>Required Input:</strong> Symbols cannot be empty as they are essential for currency identification</li>
 *   <li><strong>Real-time Feedback:</strong> Provides immediate validation feedback to users</li>
 *   <li><strong>Current Value Display:</strong> Shows the existing symbol for reference</li>
 * </ul>
 *
 * <h3>Validation Rules:</h3>
 * <ul>
 *   <li><strong>Required:</strong> Symbol cannot be empty or whitespace-only</li>
 *   <li><strong>Length:</strong> Must not exceed 5 characters for optimal display</li>
 *   <li><strong>Content:</strong> Any characters are allowed including Unicode symbols</li>
 * </ul>
 *
 * <h3>Usage Examples:</h3>
 * <ul>
 *   <li><strong>Traditional:</strong> "$", "€", "£", "¥" for real-world currency parallels</li>
 *   <li><strong>Gaming:</strong> "★", "♦", "⚡", "💎" for game-specific currencies</li>
 *   <li><strong>Custom:</strong> "G", "C", "P" for abbreviated currency names</li>
 * </ul>
 *
 * @author JExcellence
 * @see AbstractAnvilView
 * @see Currency
 * @see CurrenciesActionOverviewView
 */
public class CurrencySymbolAnvilView extends AbstractAnvilView {
	
	/**
	 * Maximum allowed length for currency symbols.
	 */
	private static final int MAXIMUM_SYMBOL_LENGTH = 5;
	
	/**
	 * State holder for the main JExEconomyImpl plugin instance.
	 */
	private final State<JExEconomyImpl> jexEconomy = initialState("plugin");
	
	/**
	 * State holder for the currency being configured.
	 */
	private final State<Currency> targetCurrency = initialState("currency");
	
	/**
	 * Constructs a new {@code CurrencySymbolAnvilView} with the currencies action overview as parent.
	 * <p>
	 * The view will return to the currencies action overview when the symbol configuration
	 * is completed or cancelled.
	 * </p>
	 */
	public CurrencySymbolAnvilView() {
		super(CurrenciesActionOverviewView.class);
	}
	
	/**
	 * Returns the internationalization key for this view.
	 * <p>
	 * This key is used to load localized strings for the currency symbol configuration
	 * interface, including titles, labels, and error messages.
	 * </p>
	 *
	 * @return the i18n key for the currency symbol anvil UI
	 */
	@Override
	protected @NotNull String getKey() {
		return "currency_symbol_anvil_ui";
	}
	
	/**
	 * Processes the user input to set the currency's symbol.
	 * <p>
	 * This method validates the input symbol and updates the currency's
	 * symbol property. If no currency exists, a new currency instance is created
	 * with the specified symbol.
	 * </p>
	 *
	 * <h3>Processing Steps:</h3>
	 * <ol>
	 *   <li>Check if a currency instance exists</li>
	 *   <li>Update existing currency or create new one with the symbol</li>
	 *   <li>Return the updated currency instance</li>
	 * </ol>
	 *
	 * @param userInput the symbol entered by the user, must not be null
	 * @param processingContext the interaction context, must not be null
	 * @return the updated currency instance with the new symbol
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
				"",
				userInput,
				Material.GOLD_INGOT
			);
		}
		
		existingCurrency.setSymbol(userInput);
		return existingCurrency;
	}
	
	/**
	 * Provides title placeholders for dynamic title generation.
	 * <p>
	 * This method returns placeholders that can be used in the view's title
	 * to display contextual information such as the current symbol
	 * and currency identifier.
	 * </p>
	 *
	 * <h3>Available Placeholders:</h3>
	 * <ul>
	 *   <li><strong>current_symbol:</strong> The current symbol text or empty string</li>
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
			final String currentSymbol = existingCurrency.getSymbol();
			final String currencyIdentifier = existingCurrency.getIdentifier();
			
			return Map.of(
				"current_symbol",
				currentSymbol,
				"identifier",
				currencyIdentifier
			);
		}
		
		return Map.of(
			"current_symbol",
			"",
			"identifier",
			"New Currency"
		);
	}
	
	/**
	 * Provides the initial input text for the anvil interface.
	 * <p>
	 * This method returns the current symbol if available,
	 * or falls back to the default behavior if no symbol exists.
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
			existingCurrency.getSymbol();
			return existingCurrency.getSymbol();
		}
		
		return super.getInitialInputText(openContext);
	}
	
	/**
	 * Validates the user input for symbol requirements.
	 * <p>
	 * This method ensures the input symbol meets all requirements for
	 * a valid currency symbol including non-empty content and length limits.
	 * </p>
	 *
	 * <h3>Validation Criteria:</h3>
	 * <ul>
	 *   <li>Input must not be empty or whitespace-only</li>
	 *   <li>Length must not exceed the maximum allowed characters</li>
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
		return !userInput.trim().isEmpty() && userInput.length() <= MAXIMUM_SYMBOL_LENGTH;
	}
	
	/**
	 * Configures the first slot (input slot) with appropriate visual elements.
	 * <p>
	 * This method sets up the input slot with a gold nugget icon and localized
	 * text to guide the user in entering a valid symbol.
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
			                                .item(Material.GOLD_NUGGET)
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
					                                MAXIMUM_SYMBOL_LENGTH
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
	 *   <li><strong>Empty Input:</strong> No symbol provided</li>
	 *   <li><strong>Too Long:</strong> Symbol exceeds maximum length</li>
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
			invalidInput == null ||
			invalidInput.trim().isEmpty()
		) {
			specificErrorKey = specificErrorKey + ".empty";
		} else if (
			       invalidInput.length() > MAXIMUM_SYMBOL_LENGTH
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
				    MAXIMUM_SYMBOL_LENGTH,
				    "current_length",
				    invalidInput != null ? invalidInput.length() : 0
			    )
		    )
		    .sendMessage();
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