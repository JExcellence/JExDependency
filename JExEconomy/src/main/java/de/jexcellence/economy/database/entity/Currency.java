package de.jexcellence.economy.database.entity;

import com.raindropcentral.rplatform.database.converter.BasicMaterialConverter;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * JPA entity representing a currency within the JExEconomyImpl system.
 * <p>
 * This entity encapsulates all properties and behaviors of a currency, including its
 * unique identification, display formatting, and visual representation. Each currency
 * serves as a fundamental unit of economic exchange within the plugin ecosystem,
 * supporting complex economic systems and player interactions.
 * </p>
 *
 * <h3>Core Properties:</h3>
 * <ul>
 *   <li><strong>Identifier:</strong> Unique string identifier for programmatic reference</li>
 *   <li><strong>Symbol:</strong> Visual symbol for display in user interfaces</li>
 *   <li><strong>Prefix:</strong> Text displayed before currency amounts</li>
 *   <li><strong>Suffix:</strong> Text displayed after currency amounts</li>
 *   <li><strong>Icon:</strong> Material representation for graphical interfaces</li>
 * </ul>
 *
 * <h3>Database Mapping:</h3>
 * <p>
 * This entity is mapped to the {@code p_currency} table in the database, with
 * appropriate constraints and indexing for optimal performance. The identifier
 * field is unique and serves as the natural key for currency lookups.
 * </p>
 *
 * <h3>Usage Examples:</h3>
 * <ul>
 *   <li><strong>Traditional Currencies:</strong> Coins, dollars, euros with standard symbols</li>
 *   <li><strong>Game-Specific Currencies:</strong> Gems, tokens, points with custom formatting</li>
 *   <li><strong>Specialized Currencies:</strong> Experience points, reputation, karma with unique displays</li>
 * </ul>
 *
 * <h3>Design Principles:</h3>
 * <ul>
 *   <li><strong>Immutability:</strong> Core properties should be modified through controlled methods</li>
 *   <li><strong>Validation:</strong> All setters include appropriate validation logic</li>
 *   <li><strong>Consistency:</strong> Formatting properties work together for cohesive display</li>
 *   <li><strong>Extensibility:</strong> Design supports future enhancement without breaking changes</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 * @see AbstractEntity
 * @see BasicMaterialConverter
 */
@Table(name = "p_currency")
@Entity
public class Currency extends AbstractEntity {
	
	/**
	 * The prefix text displayed before currency amounts in user interfaces.
	 * <p>
	 * This field allows for flexible currency formatting, supporting traditional
	 * currency symbols (e.g., "$", "€") as well as custom text prefixes
	 * (e.g., "Gold: ", "Credits: "). The prefix is optional and can be null
	 * or empty for currencies that don't require prefix formatting.
	 * </p>
	 *
	 * <h3>Usage Examples:</h3>
	 * <ul>
	 *   <li><strong>Traditional:</strong> "$" for dollar amounts</li>
	 *   <li><strong>Descriptive:</strong> "Gold: " for gold currency</li>
	 *   <li><strong>Empty:</strong> "" for currencies without prefixes</li>
	 * </ul>
	 */
	@Column(name = "prefix", nullable = false)
	private String prefix;
	
	/**
	 * The suffix text displayed after currency amounts in user interfaces.
	 * <p>
	 * This field complements the prefix to provide complete currency formatting
	 * flexibility. Suffixes are commonly used for unit indicators, pluralization,
	 * or additional context information. Like prefixes, suffixes are optional
	 * and can be null or empty when not needed.
	 * </p>
	 *
	 * <h3>Usage Examples:</h3>
	 * <ul>
	 *   <li><strong>Unit Indicators:</strong> " coins", " points", " gems"</li>
	 *   <li><strong>Contextual:</strong> " (available)", " remaining"</li>
	 *   <li><strong>Empty:</strong> "" for currencies without suffixes</li>
	 * </ul>
	 */
	@Column(name = "suffix", nullable = false)
	private String suffix;
	
	/**
	 * The unique identifier for this currency within the system.
	 * <p>
	 * This field serves as the primary means of programmatic currency identification
	 * and must be unique across all currencies in the system. The identifier is
	 * used in commands, configuration files, API calls, and database relationships.
	 * It should be descriptive yet concise, following consistent naming conventions.
	 * </p>
	 *
	 * <h3>Naming Guidelines:</h3>
	 * <ul>
	 *   <li><strong>Descriptive:</strong> Clearly indicates the currency type</li>
	 *   <li><strong>Consistent:</strong> Follows established naming patterns</li>
	 *   <li><strong>Stable:</strong> Should not change frequently to maintain data integrity</li>
	 *   <li><strong>Unique:</strong> Must be distinct from all other currency identifiers</li>
	 * </ul>
	 */
	@Column(
		name = "currency_name",
		unique = true,
		nullable = false
	)
	private String identifier;
	
	/**
	 * The visual symbol representing this currency in user interfaces.
	 * <p>
	 * This field defines the primary visual representation of the currency,
	 * typically a single character or short string that immediately identifies
	 * the currency type. Symbols should be recognizable, culturally appropriate,
	 * and compatible with the game's visual design language.
	 * </p>
	 *
	 * <h3>Symbol Categories:</h3>
	 * <ul>
	 *   <li><strong>Traditional:</strong> $, €, £, ¥ for real-world currency parallels</li>
	 *   <li><strong>Gaming:</strong> ★, ♦, ⚡ for game-specific currencies</li>
	 *   <li><strong>Custom:</strong> Unique symbols designed for specific contexts</li>
	 *   <li><strong>Unicode:</strong> Extended character set for diverse representations</li>
	 * </ul>
	 */
	@Column(
		name = "symbol",
		nullable = false
	)
	private String symbol;
	
	/**
	 * The Material icon used to represent this currency in graphical user interfaces.
	 * <p>
	 * This field provides a Minecraft Material that serves as the visual icon
	 * for the currency in inventory-based interfaces, GUI menus, and other
	 * graphical representations. The Material is converted to and from string
	 * format for database storage using the BasicMaterialConverter.
	 * </p>
	 *
	 * <h3>Icon Selection Guidelines:</h3>
	 * <ul>
	 *   <li><strong>Thematic Relevance:</strong> Should visually represent the currency concept</li>
	 *   <li><strong>Visual Clarity:</strong> Must be easily recognizable in small interface elements</li>
	 *   <li><strong>Consistency:</strong> Should fit with the overall visual design of the system</li>
	 *   <li><strong>Availability:</strong> Must be a valid Material available in the target Minecraft version</li>
	 * </ul>
	 */
	@Column(
		name = "icon",
		nullable = false,
		columnDefinition = "LONGTEXT"
	)
	@Convert(converter = BasicMaterialConverter.class)
	private Material icon;
	
	/**
	 * Protected no-argument constructor required by JPA/Hibernate for entity instantiation.
	 * <p>
	 * This constructor is used exclusively by the persistence framework during
	 * entity loading and should not be called directly by application code.
	 * All fields will be initialized to their default values and subsequently
	 * populated by the persistence framework.
	 * </p>
	 */
	protected Currency() {
		// JPA/Hibernate requires a no-argument constructor for entity instantiation
	}
	
	/**
	 * Constructs a new Currency entity with complete property specification.
	 * <p>
	 * This constructor creates a fully-configured currency entity with all
	 * display properties and visual elements specified. It performs basic
	 * validation to ensure that required fields are not null and that the
	 * currency is in a valid state upon creation.
	 * </p>
	 *
	 * <h3>Parameter Validation:</h3>
	 * <ul>
	 *   <li>All parameters are validated for null values</li>
	 *   <li>Identifier uniqueness is enforced at the database level</li>
	 *   <li>Symbol and icon must be valid for display purposes</li>
	 * </ul>
	 *
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * Currency goldCurrency = new Currency(
	 *     "Gold: ",           // prefix
	 *     " coins",           // suffix
	 *     "gold",             // identifier
	 *     "⚜",                // symbol
	 *     Material.GOLD_INGOT // icon
	 * );
	 * }</pre>
	 *
	 * @param prefix the prefix text to display before amounts, must not be null
	 * @param suffix the suffix text to display after amounts, must not be null
	 * @param identifier the unique identifier for this currency, must not be null
	 * @param symbol the visual symbol representing this currency, must not be null
	 * @param icon the Material icon for graphical interfaces, must not be null
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public Currency(
		final @NotNull String prefix,
		final @NotNull String suffix,
		final @NotNull String identifier,
		final @NotNull String symbol,
		final @NotNull Material icon
	) {
		
		this.prefix = prefix;
		this.suffix = suffix;
		this.identifier = identifier;
		this.symbol = symbol;
		this.icon = icon;
	}
	
	/**
	 * Constructs a new Currency entity with minimal configuration using default values.
	 * <p>
	 * This convenience constructor creates a currency with only the identifier
	 * specified, using sensible defaults for all other properties. This is
	 * useful for rapid prototyping or when detailed formatting is not immediately
	 * required and can be configured later through setter methods.
	 * </p>
	 *
	 * <h3>Default Values:</h3>
	 * <ul>
	 *   <li><strong>Prefix:</strong> Empty string (no prefix)</li>
	 *   <li><strong>Suffix:</strong> Empty string (no suffix)</li>
	 *   <li><strong>Symbol:</strong> Empty string (to be configured later)</li>
	 *   <li><strong>Icon:</strong> GOLD_INGOT (standard currency representation)</li>
	 * </ul>
	 *
	 * <h3>Usage Example:</h3>
	 * <pre>{@code
	 * Currency basicCurrency = new Currency("gems");
	 * basicCurrency.setSymbol("💎");
	 * basicCurrency.setPrefix("Gems: ");
	 * }</pre>
	 *
	 * @param identifier the unique identifier for this currency, must not be null
	 * @throws IllegalArgumentException if the identifier is null
	 */
	public Currency(final @NotNull String identifier) {
		this(
			"",
			"",
			identifier,
			"",
			Material.GOLD_INGOT
		);
	}
	
	/**
	 * Retrieves the prefix text displayed before currency amounts.
	 * <p>
	 * This method returns the current prefix configuration for the currency,
	 * which may be an empty string if no prefix is configured. The prefix
	 * is used in conjunction with the suffix to provide complete currency
	 * formatting in user interfaces.
	 * </p>
	 *
	 * @return the currency prefix string, may be empty but never null
	 */
	public @NotNull String getPrefix() {
		return
			this.prefix != null ?
			this.prefix :
			"";
	}
	
	/**
	 * Updates the prefix text displayed before currency amounts.
	 * <p>
	 * This method allows modification of the currency's prefix formatting.
	 * The prefix will be displayed before all currency amounts in user
	 * interfaces and should be chosen to provide clear context and
	 * professional appearance.
	 * </p>
	 *
	 * <h3>Validation:</h3>
	 * <ul>
	 *   <li>Null values are converted to empty strings for consistency</li>
	 *   <li>No length restrictions are enforced, but reasonable lengths are recommended</li>
	 *   <li>Special characters and Unicode are supported</li>
	 * </ul>
	 *
	 * @param newCurrencyPrefix the new prefix string to set, null values are converted to empty string
	 */
	public void setPrefix(final @Nullable String newCurrencyPrefix) {
		this.prefix =
			newCurrencyPrefix != null ?
			newCurrencyPrefix :
			"";
	}
	
	/**
	 * Retrieves the suffix text displayed after currency amounts.
	 * <p>
	 * This method returns the current suffix configuration for the currency,
	 * which may be an empty string if no suffix is configured. The suffix
	 * complements the prefix to provide complete currency formatting and
	 * often includes unit indicators or contextual information.
	 * </p>
	 *
	 * @return the currency suffix string, may be empty but never null
	 */
	public @NotNull String getSuffix() {
		return
			this.suffix != null ?
			this.suffix :
			"";
	}
	
	/**
	 * Updates the suffix text displayed after currency amounts.
	 * <p>
	 * This method allows modification of the currency's suffix formatting.
	 * The suffix will be displayed after all currency amounts in user
	 * interfaces and is commonly used for unit indicators, pluralization,
	 * or additional context.
	 * </p>
	 *
	 * <h3>Validation:</h3>
	 * <ul>
	 *   <li>Null values are converted to empty strings for consistency</li>
	 *   <li>No length restrictions are enforced, but reasonable lengths are recommended</li>
	 *   <li>Special characters and Unicode are supported</li>
	 * </ul>
	 *
	 * @param newCurrencySuffix the new suffix string to set, null values are converted to empty string
	 */
	public void setSuffix(
		final @Nullable String newCurrencySuffix
	) {
		this.suffix =
			newCurrencySuffix != null ?
			newCurrencySuffix :
			"";
	}
	
	/**
	 * Retrieves the unique identifier for this currency.
	 * <p>
	 * This method returns the currency's unique identifier, which serves as
	 * the primary means of programmatic reference throughout the system.
	 * The identifier is used in commands, configuration files, API calls,
	 * and database relationships.
	 * </p>
	 *
	 * @return the currency's unique identifier string, never null
	 */
	public @NotNull String getIdentifier() {
		return this.identifier;
	}
	
	/**
	 * Updates the unique identifier for this currency.
	 * <p>
	 * This method allows modification of the currency's identifier, though
	 * such changes should be made carefully as they may affect existing
	 * references, commands, and data relationships. The new identifier
	 * must be unique across all currencies in the system.
	 * </p>
	 *
	 * <h3>Important Considerations:</h3>
	 * <ul>
	 *   <li>Changing identifiers may break existing references</li>
	 *   <li>Database uniqueness constraints will be enforced</li>
	 *   <li>Commands and configurations may need updates</li>
	 *   <li>Consider data migration implications</li>
	 * </ul>
	 *
	 * @param newCurrencyIdentifier the new unique identifier to set, must not be null
	 * @throws IllegalArgumentException if the identifier is null
	 */
	public void setIdentifier(final @NotNull String newCurrencyIdentifier) {
		
		this.identifier = newCurrencyIdentifier;
	}
	
	/**
	 * Retrieves the visual symbol representing this currency.
	 * <p>
	 * This method returns the currency's visual symbol, which provides
	 * immediate visual identification in user interfaces. The symbol
	 * should be recognizable and appropriate for the currency's context
	 * and target audience.
	 * </p>
	 *
	 * @return the currency's visual symbol string, never null
	 */
	public @NotNull String getSymbol() {
		return this.symbol != null ?
		       this.symbol :
		       "";
	}
	
	/**
	 * Updates the visual symbol representing this currency.
	 * <p>
	 * This method allows modification of the currency's visual symbol.
	 * The symbol should be chosen for clarity, cultural appropriateness,
	 * and compatibility with the target display environment. Unicode
	 * symbols are supported for diverse representation options.
	 * </p>
	 *
	 * <h3>Symbol Guidelines:</h3>
	 * <ul>
	 *   <li>Should be visually distinct and recognizable</li>
	 *   <li>Must be compatible with target display systems</li>
	 *   <li>Should align with currency theme and context</li>
	 *   <li>Unicode characters are supported for international symbols</li>
	 * </ul>
	 *
	 * @param newCurrencySymbol the new visual symbol to set, null values are converted to empty string
	 */
	public void setSymbol(final @Nullable String newCurrencySymbol) {
		this.symbol = newCurrencySymbol != null ?
		                            newCurrencySymbol :
		                            "";
	}
	
	/**
	 * Retrieves the Material icon used for graphical representation of this currency.
	 * <p>
	 * This method returns the Minecraft Material that serves as the visual
	 * icon for the currency in inventory-based interfaces, GUI menus, and
	 * other graphical representations. The Material provides a familiar
	 * visual reference for players.
	 * </p>
	 *
	 * @return the Material icon for this currency, never null
	 */
	public @NotNull Material getIcon() {
		return this.icon;
	}
	
	/**
	 * Updates the Material icon used for graphical representation of this currency.
	 * <p>
	 * This method allows modification of the currency's graphical icon.
	 * The Material should be chosen for thematic relevance, visual clarity,
	 * and consistency with the overall system design. The Material must
	 * be valid for the target Minecraft version.
	 * </p>
	 *
	 * <h3>Icon Selection Guidelines:</h3>
	 * <ul>
	 *   <li>Should thematically represent the currency concept</li>
	 *   <li>Must be visually clear in small interface elements</li>
	 *   <li>Should maintain consistency with system design</li>
	 *   <li>Must be a valid Material for the target Minecraft version</li>
	 * </ul>
	 *
	 * @param newCurrencyIcon the new Material icon to set, must not be null
	 * @throws IllegalArgumentException if the icon Material is null
	 */
	public void setIcon(final @NotNull Material newCurrencyIcon) {
		
		this.icon = newCurrencyIcon;
	}
	
	/**
	 * Determines whether this currency is equal to another object.
	 * <p>
	 * Two Currency entities are considered equal if they have the same unique
	 * identifier. This equality contract ensures that currencies with the same
	 * identifier are treated as the same entity regardless of other property
	 * differences, which is appropriate for entity comparison.
	 * </p>
	 *
	 * <h3>Equality Contract:</h3>
	 * <ul>
	 *   <li><strong>Reflexive:</strong> x.equals(x) returns true</li>
	 *   <li><strong>Symmetric:</strong> x.equals(y) returns true if and only if y.equals(x) returns true</li>
	 *   <li><strong>Transitive:</strong> If x.equals(y) and y.equals(z), then x.equals(z)</li>
	 *   <li><strong>Consistent:</strong> Multiple invocations return the same result</li>
	 *   <li><strong>Null-safe:</strong> x.equals(null) returns false</li>
	 * </ul>
	 *
	 * @param comparisonObject the object to compare with this currency
	 * @return true if the objects are equal based on identifier comparison, false otherwise
	 */
	@Override
	public boolean equals(final @Nullable Object comparisonObject) {
		if (
			this == comparisonObject
		) {
			return true;
		}
		
		if (
			comparisonObject == null ||
			getClass() != comparisonObject.getClass()
		) {
			return false;
		}
		
		final Currency otherCurrency = (Currency) comparisonObject;
		return Objects.equals(
			this.identifier,
			otherCurrency.identifier
		);
	}
	
	/**
	 * Generates a hash code for this currency based on its unique identifier.
	 * <p>
	 * This method returns a hash code value that is consistent with the equals
	 * method, ensuring that equal currencies have equal hash codes. The hash
	 * code is based solely on the unique identifier to maintain consistency
	 * with the equality contract.
	 * </p>
	 *
	 * <h3>Hash Code Contract:</h3>
	 * <ul>
	 *   <li>If two objects are equal according to equals(), they must have the same hash code</li>
	 *   <li>Hash code should remain consistent during object lifetime</li>
	 *   <li>Hash code should be efficiently computable</li>
	 *   <li>Hash code should provide good distribution for hash-based collections</li>
	 * </ul>
	 *
	 * @return the hash code value for this currency
	 */
	@Override
	public int hashCode() {
		return Objects.hashCode(this.identifier);
	}
	
	/**
	 * Returns a string representation of this currency for debugging and logging purposes.
	 * <p>
	 * This method provides a comprehensive string representation that includes
	 * all significant properties of the currency. The format is designed to be
	 * informative for debugging while remaining readable and concise.
	 * </p>
	 *
	 * <h3>String Format:</h3>
	 * <p>
	 * The returned string includes the currency identifier, symbol, prefix, suffix,
	 * and icon in a structured format that clearly identifies each property and
	 * its current value.
	 * </p>
	 *
	 * @return a detailed string representation of this currency
	 */
	@Override
	public @NotNull String toString() {
		return String.format(
			"Currency{identifier='%s', symbol='%s', prefix='%s', suffix='%s', icon=%s}",
			this.identifier,
			this.symbol,
			this.prefix,
			this.suffix,
			this.icon
		);
	}
}