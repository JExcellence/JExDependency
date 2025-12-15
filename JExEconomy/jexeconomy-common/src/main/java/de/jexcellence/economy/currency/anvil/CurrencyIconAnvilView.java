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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Anvil view for configuring a currency's icon material.
 * <p>
 * This view provides an interactive interface for players to input and validate
 * a new icon material for a currency. The icon material serves as the visual
 * representation of the currency in various UI elements throughout the system.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Material Validation:</strong> Ensures entered materials are valid Minecraft items</li>
 *   <li><strong>Real-time Feedback:</strong> Provides immediate validation feedback to users</li>
 *   <li><strong>Current Value Display:</strong> Shows the existing icon material for reference</li>
 *   <li><strong>Error Handling:</strong> Comprehensive error messages for different validation failures</li>
 * </ul>
 *
 * <h3>Input Requirements:</h3>
 * <ul>
 *   <li>Must be a valid Minecraft material name (case-insensitive)</li>
 *   <li>Material must represent an actual item (not air or blocks)</li>
 *   <li>Examples: GOLD_INGOT, DIAMOND, EMERALD, IRON_INGOT</li>
 * </ul>
 *
 * <h3>Validation Process:</h3>
 * <ol>
 *   <li>Check if input is not empty</li>
 *   <li>Verify material exists in Minecraft</li>
 *   <li>Ensure material represents an item</li>
 *   <li>Confirm material is not air</li>
 * </ol>
 *
 * @author JExcellence
 * @see AbstractAnvilView
 * @see Currency
 * @see CurrenciesCreatingView
 */
public class CurrencyIconAnvilView extends AbstractAnvilView {
	
	/**
	 * State holder for the main JExEconomy plugin instance.
	 */
	private final State<JExEconomy> jexEconomy = initialState("plugin");
	
	/**
	 * State holder for the currency being configured.
	 */
	private final State<Currency> targetCurrency = initialState("currency");
	
	/**
	 * Constructs a new {@code CurrencyIconAnvilView} with the currencies creating view as parent.
	 * <p>
	 * The view will return to the currencies creating view when the icon configuration
	 * is completed or cancelled.
	 * </p>
	 */
	public CurrencyIconAnvilView() {
		super(CurrenciesCreatingView.class);
	}
	
	/**
	 * Returns the internationalization key for this view.
	 * <p>
	 * This key is used to load localized strings for the currency icon configuration
	 * interface, including titles, labels, and error messages.
	 * </p>
	 *
	 * @return the i18n key for the currency icon anvil UI
	 */
	@Override
	protected @NotNull String getKey() {
		return "currency_icon_anvil_ui";
	}
	
	/**
	 * Processes the user input to set the currency's icon material.
	 * <p>
	 * This method validates the input material name and updates the currency's
	 * icon property. If no currency exists, a new currency instance is created
	 * with the specified icon material.
	 * </p>
	 *
	 * <h3>Processing Steps:</h3>
	 * <ol>
	 *   <li>Parse the input string to a Material enum value</li>
	 *   <li>Check if a currency instance exists</li>
	 *   <li>Update existing currency or create new one with the icon</li>
	 *   <li>Return the updated currency instance</li>
	 * </ol>
	 *
	 * @param userInput the material name entered by the user, must not be null
	 * @param processingContext the interaction context, must not be null
	 * @return the updated currency instance with the new icon material
	 * @throws IllegalArgumentException if the material name is invalid
	 */
	@Override
	protected @NotNull Object processInput(
		final @NotNull String userInput,
		final @NotNull Context processingContext
	) {
		final Currency existingCurrency = this.targetCurrency.get(processingContext);
		final Material parsedMaterial = Material.valueOf(userInput.toUpperCase().trim());
		
		if (
			existingCurrency == null
		) {
			return new Currency("");
		} else {
			existingCurrency.setIcon(parsedMaterial);
			return existingCurrency;
		}
	}
	
	/**
	 * Provides title placeholders for dynamic title generation.
	 * <p>
	 * This method returns placeholders that can be used in the view's title
	 * to display contextual information such as the current icon material
	 * and currency identifier.
	 * </p>
	 *
	 * <h3>Available Placeholders:</h3>
	 * <ul>
	 *   <li><strong>current_icon:</strong> The current icon material name or default</li>
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
			final Material currentIconMaterial = existingCurrency.getIcon();
			final String currencyIdentifier = existingCurrency.getIdentifier();
			
			return Map.of(
				"current_icon",
				currentIconMaterial.name(),
				"identifier",
				currencyIdentifier
			);
		}
		
		return Map.of(
			"current_icon",
			"GOLD_INGOT",
			"identifier",
			"New Currency"
		);
	}
	
	/**
	 * Provides the initial input text for the anvil interface.
	 * <p>
	 * This method returns the current icon material name if available,
	 * or a default material name to pre-populate the input field.
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
			return existingCurrency.getIcon().name();
		}
		
		return "GOLD_INGOT";
	}
	
	/**
	 * Validates the user input for material name correctness.
	 * <p>
	 * This method performs comprehensive validation to ensure the input
	 * represents a valid, usable Minecraft material for currency icons.
	 * </p>
	 *
	 * <h3>Validation Criteria:</h3>
	 * <ul>
	 *   <li>Input must not be empty or whitespace-only</li>
	 *   <li>Material must exist in the Minecraft Material enum</li>
	 *   <li>Material must represent an actual item</li>
	 *   <li>Material must not be air</li>
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
		if (
			userInput.trim().isEmpty()
		) {
			return false;
		}
		
		try {
			final Material parsedMaterial = Material.valueOf(userInput.toUpperCase().trim());
			return parsedMaterial.isItem() && !parsedMaterial.isAir();
		} catch (
			  final IllegalArgumentException materialParsingException
		) {
			return false;
		}
	}
	
	/**
	 * Configures the first slot (input slot) with appropriate visual elements.
	 * <p>
	 * This method sets up the input slot with an item frame icon and localized
	 * text to guide the user in entering a valid material name.
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
		renderContext.firstSlot(
			UnifiedBuilderFactory
				.item(Material.ITEM_FRAME)
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
					).build().children()
				)
				.build()
		);
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
	 *   <li><strong>Empty Input:</strong> No material name provided</li>
	 *   <li><strong>Invalid Material:</strong> Material name doesn't exist</li>
	 *   <li><strong>Air Material:</strong> Material represents air</li>
	 *   <li><strong>Not Item:</strong> Material is not an item</li>
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
		} else {
			try {
				final Material parsedMaterial = Material.valueOf(invalidInput.toUpperCase().trim());
				
				if (
					parsedMaterial.isAir()
				) {
					specificErrorKey = specificErrorKey + ".air_material";
				} else if (
					       !parsedMaterial.isItem()
				) {
					specificErrorKey = specificErrorKey + ".not_item";
				} else {
					specificErrorKey = specificErrorKey + ".general";
				}
			} catch (
				  final IllegalArgumentException materialParsingException
			) {
				specificErrorKey = specificErrorKey + ".invalid_material";
			}
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
				    "example_materials",
				    "GOLD_INGOT, DIAMOND, EMERALD, IRON_INGOT"
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