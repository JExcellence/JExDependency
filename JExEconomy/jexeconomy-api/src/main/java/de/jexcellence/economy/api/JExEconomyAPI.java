package de.jexcellence.economy.api;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Static accessor for the JExEconomy API.
 *
 * <p>Provides convenient one-liner access to the {@link EconomyProvider}
 * registered by the JExEconomy plugin via Bukkit's
 * {@link org.bukkit.plugin.ServicesManager}.
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * // In your plugin's onEnable():
 * EconomyProvider economy = JExEconomyAPI.get();
 * if (economy == null) {
 *     getLogger().warning("JExEconomy not found!");
 *     return;
 * }
 *
 * // Use the API
 * economy.getBalance(player, "coins").thenAccept(balance ->
 *     getLogger().info("Balance: " + balance));
 * }</pre>
 *
 * @author JExcellence
 * @since 3.0.0
 */
public final class JExEconomyAPI {

    private JExEconomyAPI() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Retrieves the registered {@link EconomyProvider} from Bukkit's service manager.
     *
     * @return the economy provider, or {@code null} if JExEconomy is not loaded
     */
    public static @Nullable EconomyProvider get() {
        var registration = Bukkit.getServicesManager().getRegistration(EconomyProvider.class);
        return registration != null ? registration.getProvider() : null;
    }

    /**
     * Retrieves the registered {@link EconomyProvider}, throwing if unavailable.
     *
     * @return the economy provider
     * @throws IllegalStateException if JExEconomy is not loaded or not yet enabled
     */
    public static @NotNull EconomyProvider require() {
        var provider = get();
        if (provider == null) {
            throw new IllegalStateException(
                    "JExEconomy is not available. Ensure JExEconomy is installed "
                            + "and your plugin depends on it in paper-plugin.yml or plugin.yml");
        }
        return provider;
    }

    /**
     * Checks whether JExEconomy is currently available.
     *
     * @return {@code true} if the economy provider is registered
     */
    public static boolean isAvailable() {
        return get() != null;
    }
}
