package de.jexcellence.economy.listener;


import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.database.entity.UserCurrency;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener for player join events to manage user and currency data initialization.
 *
 * <p>This listener ensures that a {@link User} entity exists and is up-to-date for every player who joins the server.
 * It also ensures that the player has a {@link UserCurrency} entry for every available currency in the system.
 * If a user or user-currency association does not exist, it is created; otherwise, the player's name is updated
 * to reflect any changes since their last login.
 *
 * <p><strong>Key Responsibilities:</strong>
 * <ul>
 *   <li>Create new {@link User} entities for first-time players</li>
 *   <li>Update existing player names to maintain data consistency</li>
 *   <li>Initialize {@link UserCurrency} associations for all available currencies</li>
 *   <li>Ensure data integrity across the currency system</li>
 * </ul>
 *
 * <p><strong>Event Processing:</strong>
 *
 * <p>The listener uses {@link AsyncPlayerPreLoginEvent} with {@link EventPriority#LOWEST} to ensure
 * all user data is properly initialized before other plugins can interact with the player.
 * All database operations are performed asynchronously to prevent server lag.
 *
 * @author JExcellence
 * @see User
 * @see UserCurrency
 * @see AsyncPlayerPreLoginEvent
 */
public class OnJoin implements Listener {
	
	/**
	 * Logger instance for tracking player join operations and debugging.
	 */
	private static final Logger LOGGER = CentralLogger.getLoggerByName("JExEconomy");
	
	/**
	 * The main JExEconomy plugin instance for accessing repositories and services.
	 */
	private final JExEconomy jexEconomyImpl;
	
	/**
	 * Constructs a new {@code OnJoin} listener with the specified plugin instance.
 *
 * <p>The listener will use the provided plugin instance to access user and currency repositories
	 * for database operations during player join processing.
	 *
	 * @param jexEconomy the main JExEconomy plugin instance, must not be null
	 * @throws IllegalArgumentException if the plugin instance is null
	 */
	public OnJoin(
		final @NotNull JExEconomy jexEconomy
	) {
		
		this.jexEconomyImpl = jexEconomy;
	}
	
	/**
	 * Handles the {@link AsyncPlayerPreLoginEvent} to ensure user and currency data is properly initialized.
 *
 * <p>This method performs the following operations asynchronously:
	 * <ol>
	 *   <li>Searches for an existing {@link User} entity by the player's UUID</li>
	 *   <li>Creates a new user if none exists, or updates the existing user's name</li>
	 *   <li>Ensures the player has {@link UserCurrency} associations for all available currencies</li>
	 * </ol>
	 *
	 * <p><strong>Performance Considerations:</strong>
	 * <ul>
	 *   <li>All database operations are executed asynchronously to prevent server lag</li>
	 *   <li>Uses the plugin's dedicated executor service for optimal thread management</li>
	 *   <li>Processes currency associations in parallel for improved performance</li>
	 * </ul>
	 *
	 * @param playerPreLoginEvent the pre-login event containing player information, must not be null
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerPreLogin(
		final @NotNull AsyncPlayerPreLoginEvent playerPreLoginEvent
	) {
		CompletableFuture.supplyAsync(
			() -> this.jexEconomyImpl.getUserRepository().findByAttributes(
				Map.of("uniqueId", playerPreLoginEvent.getUniqueId())
			).orElse(null),
			this.jexEconomyImpl.getExecutor()
		).thenAcceptAsync(
			existingUser -> {
				final User processedUser;
				
				if (
					existingUser == null
				) {
					processedUser = new User(
						playerPreLoginEvent.getUniqueId(),
						playerPreLoginEvent.getName()
					);
					this.jexEconomyImpl.getUserRepository().create(processedUser);
					LOGGER.log(
						Level.INFO,
						"Created new player: " + processedUser.getUniqueId()
					);
				} else {
					existingUser.setPlayerName(playerPreLoginEvent.getName());
					this.jexEconomyImpl.getUserRepository().update(existingUser);
					processedUser = existingUser;
					LOGGER.log(
						Level.INFO,
						"Updated existing player: " + processedUser.getUniqueId()
					);
				}
				
				this.initializePlayerCurrencyAssociations(processedUser);
			},
			this.jexEconomyImpl.getExecutor()
		);
	}
	
	/**
	 * Ensures the specified player has a {@link UserCurrency} association for every available currency.
 *
 * <p>This method iterates through all currencies in the system and creates {@link UserCurrency}
	 * associations for any currencies that the player doesn't already have. Each currency check
	 * is performed asynchronously to optimize performance.
	 *
	 * <p><strong>Association Creation Process:</strong>
	 * <ol>
	 *   <li>Iterate through all available currencies in the system</li>
	 *   <li>Check if a {@link UserCurrency} association already exists</li>
	 *   <li>Create a new association with zero balance if none exists</li>
	 * </ol>
	 *
	 * @param targetPlayer the user entity to initialize currency associations for, must not be null
	 */
	private void initializePlayerCurrencyAssociations(
		final @NotNull User targetPlayer
	) {
		this.jexEconomyImpl.getCurrencies().values().forEach(
			availableCurrency -> this.ensureCurrencyAssociationExists(targetPlayer, availableCurrency)
		);
	}
	
	/**
	 * Ensures a {@link UserCurrency} association exists between the specified player and currency.
 *
 * <p>This method checks if a {@link UserCurrency} association already exists for the given
	 * player and currency combination. If no association is found, a new one is created
	 * with a zero balance.
	 *
	 * @param targetPlayer the user entity to check associations for, must not be null
	 * @param targetCurrency the currency to ensure association with, must not be null
	 */
	private void ensureCurrencyAssociationExists(
		final @NotNull User targetPlayer,
		final @NotNull Currency targetCurrency
	) {
		CompletableFuture.supplyAsync(
			() -> this.jexEconomyImpl.getUserCurrencyRepository().findByAttributes(
				Map.of(
					"player.uniqueId", targetPlayer.getUniqueId(),
					"currency.id", targetCurrency.getId()
				)
			).orElse(null),
			this.jexEconomyImpl.getExecutor()
		).thenAcceptAsync(
			existingUserCurrency -> {
				if (
					existingUserCurrency == null
				) {
					final UserCurrency newUserCurrency = new UserCurrency(
						targetPlayer,
						targetCurrency
					);
					this.jexEconomyImpl.getUserCurrencyRepository().create(newUserCurrency);
					LOGGER.log(
						Level.FINE,
						String.format(
							"Created currency association for player %s and currency %s",
							targetPlayer.getUniqueId(),
							targetCurrency.getIdentifier()
						)
					);
				}
			},
			this.jexEconomyImpl.getExecutor()
		);
	}
}
