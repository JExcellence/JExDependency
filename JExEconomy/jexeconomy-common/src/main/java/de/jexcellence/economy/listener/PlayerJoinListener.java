package de.jexcellence.economy.listener;

import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.service.EconomyService;
import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Ensures an {@link de.jexcellence.economy.database.entity.EconomyPlayer} record
 * and accounts for every registered currency exist before a player fully joins.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class PlayerJoinListener implements Listener {

    private final @NotNull EconomyService economyService;
    private final @NotNull JExLogger logger;

    /**
     * Creates the listener.
     *
     * @param economy the JExEconomy API entry point
     */
    public PlayerJoinListener(@NotNull JExEconomy economy) {
        this.economyService = economy.economyService();
        this.logger = economy.logger();
    }

    /**
     * Ensures player record and accounts exist for every currency.
     *
     * @param event the async pre-login event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        var uuid = event.getUniqueId();
        var name = event.getName();

        var currencies = economyService.getAllCurrencies();
        if (currencies.isEmpty()) {
            return;
        }

        for (var currency : currencies.values()) {
            economyService.getOrCreateAccount(uuid, name, currency).join();
        }

        logger.debug("Ensured accounts for {} across {} currencies", name, currencies.size());
    }
}
