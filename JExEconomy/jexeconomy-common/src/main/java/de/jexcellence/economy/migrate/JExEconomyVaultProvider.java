package de.jexcellence.economy.migrate;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.adapter.CurrencyAdapter;
import de.jexcellence.economy.adapter.CurrencyResponse;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.database.entity.UserCurrency;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Vault Economy provider implementation for JExEconomy.
 *
 * This class bridges JExEconomy's advanced multi-currency system with Vault's
 * traditional single-currency API, ensuring compatibility with thousands of
 * existing plugins while providing enhanced functionality.
 *
 * Features:
 * - Full Vault Economy API compatibility
 * - Automatic default currency handling
 * - Enhanced error handling and logging
 * - Support for both online and offline players
 * - Precision handling for decimal currencies
 * - Integration with JExEconomy's CurrencyAdapter
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class JExEconomyVaultProvider implements Economy {
    
    private final JExEconomy jexEconomyImpl;
    private final CurrencyAdapter currencyAdapter;
    private final Logger logger;
    
    // Cache for default currency to avoid repeated database queries
    private volatile Currency defaultCurrency;
    private volatile long lastCurrencyCheck = 0;
    private static final long CURRENCY_CACHE_DURATION = 30000; // 30 seconds
    
    /**
     * Executes JExEconomyVaultProvider.
     */
    public JExEconomyVaultProvider(
            final @NotNull JExEconomy jexEconomyImpl
    ) {
        this.jexEconomyImpl = jexEconomyImpl;
        this.currencyAdapter = this.jexEconomyImpl.getCurrencyAdapter();
        this.logger = CentralLogger.getLoggerByName("JExEconomy");
        
        logger.info("JExEconomy Vault provider initialized");
    }
    
    /**
     * Returns whether enabled.
     */
    @Override
    public boolean isEnabled() {
        return this.jexEconomyImpl.getPlugin().isEnabled() && this.getDefaultCurrency() != null;
    }
    
    /**
     * Gets name.
     */
    @Override
    public String getName() {
        return "JExEconomy";
    }
    
    /**
     * Returns whether bankSupport.
     */
    @Override
    public boolean hasBankSupport() {
        return false;
    }
    
    /**
     * Executes fractionalDigits.
     */
    @Override
    public int fractionalDigits() {
        return 2;
    }
    
    /**
     * Executes format.
     */
    @Override
    public String format(double amount) {
        Currency currency = getDefaultCurrency();
        if (currency == null) {
            return String.format("%.2f", amount);
        }
        
        String formatted = String.format("%." + this.fractionalDigits() + "f", amount);
        
	    if (!currency.getSymbol().isEmpty()) {
            return currency.getSymbol() + formatted;
        }
        
        return formatted;
    }
    
    /**
     * Executes currencyNamePlural.
     */
    @Override
    public String currencyNamePlural() {
        Currency currency = getDefaultCurrency();
        return currency != null ? currency.getIdentifier() + "s" : "not_defined";
    }
    
    /**
     * Executes currencyNameSingular.
     */
    @Override
    public String currencyNameSingular() {
        Currency currency = getDefaultCurrency();
        return currency != null ? currency.getIdentifier() : "not_defined";
    }
    
    /**
     * Returns whether account.
     */
    @Override
    public boolean hasAccount(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return false;
        }
        
        OfflinePlayer player = getOfflinePlayer(playerName);
        return player != null && hasAccount(player);
    }
    
    /**
     * Returns whether account.
     */
    @Override
    public boolean hasAccount(OfflinePlayer player) {
        if (player == null) {
            return false;
        }
        
        Currency currency = getDefaultCurrency();
        if (currency == null) {
            return false;
        }
        
        try {
            UserCurrency userCurrency = currencyAdapter.getUserCurrency(player, currency.getIdentifier()).join();
            return userCurrency != null;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error checking account for player: " + player.getName(), e);
            return false;
        }
    }
    
    /**
     * Returns whether account.
     */
    @Override
    public boolean hasAccount(String playerName, String worldName) {
        // JExEconomy doesn't support per-world economies through Vault
        // (though it could be extended to do so)
        return hasAccount(playerName);
    }
    
    /**
     * Returns whether account.
     */
    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }
    
    /**
     * Gets balance.
     */
    @Override
    public double getBalance(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return 0.0;
        }
        
        OfflinePlayer player = getOfflinePlayer(playerName);
        return player != null ? getBalance(player) : 0.0;
    }
    
    /**
     * Gets balance.
     */
    @Override
    public double getBalance(OfflinePlayer player) {
        if (player == null) {
            return 0.0;
        }
        
        Currency currency = getDefaultCurrency();
        if (currency == null) {
            return 0.0;
        }
        
        try {
            return currencyAdapter.getBalance(player, currency).join();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting balance for player: " + player.getName(), e);
            return 0.0;
        }
    }
    
    /**
     * Gets balance.
     */
    @Override
    public double getBalance(String playerName, String worldName) {
        return getBalance(playerName);
    }
    
    /**
     * Gets balance.
     */
    @Override
    public double getBalance(OfflinePlayer player, String worldName) {
        return getBalance(player);
    }
    
    /**
     * Executes has.
     */
    @Override
    public boolean has(String playerName, double amount) {
        return getBalance(playerName) >= amount;
    }
    
    /**
     * Executes has.
     */
    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }
    
    /**
     * Executes has.
     */
    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }
    
    /**
     * Executes has.
     */
    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }
    
    /**
     * Executes withdrawPlayer.
     */
    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        if (playerName == null || playerName.isEmpty()) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Invalid player name");
        }
        
        OfflinePlayer player = getOfflinePlayer(playerName);
        if (player == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player not found");
        }
        
        return withdrawPlayer(player, amount);
    }
    
    /**
     * Executes withdrawPlayer.
     */
    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (player == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player is null");
        }
        
        if (amount < 0) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Cannot withdraw negative amount");
        }
        
        Currency currency = getDefaultCurrency();
        if (currency == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "No default currency available");
        }
        
        try {
            // Get current balance
            double currentBalance = getBalance(player);
            
            // Check if player has sufficient funds
            if (currentBalance < amount) {
                return new EconomyResponse(amount, currentBalance, EconomyResponse.ResponseType.FAILURE,
                                           "Insufficient funds");
            }
            
            // Perform withdrawal using CurrencyAdapter
            CurrencyResponse withdrawResult = currencyAdapter.withdraw(player, currency, amount).join();
            
            if (withdrawResult.operationStatus().equals(CurrencyResponse.ResponseType.SUCCESS)) {
                double newBalance = withdrawResult.resultingBalance();
                return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
            } else {
                return new EconomyResponse(0, currentBalance, EconomyResponse.ResponseType.FAILURE, withdrawResult.failureMessage());
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error withdrawing from player: " + player.getName(), e);
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE,
                                       "Internal error: " + e.getMessage());
        }
    }
    
    /**
     * Executes withdrawPlayer.
     */
    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }
    
    /**
     * Executes withdrawPlayer.
     */
    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }
    
    /**
     * Executes depositPlayer.
     */
    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        if (playerName == null || playerName.isEmpty()) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Invalid player name");
        }
        
        OfflinePlayer player = getOfflinePlayer(playerName);
        if (player == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player not found");
        }
        
        return depositPlayer(player, amount);
    }
    
    /**
     * Executes depositPlayer.
     */
    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (player == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player is null");
        }
        
        if (amount < 0) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Cannot deposit negative amount");
        }
        
        Currency currency = getDefaultCurrency();
        if (currency == null) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "No default currency available");
        }
        
        try {
            // Get current balance
            double currentBalance = getBalance(player);
            
            // Perform deposit using CurrencyAdapter
            CurrencyResponse depositResult = currencyAdapter.deposit(player, currency, amount).join();
            
            if (depositResult.operationStatus().equals(CurrencyResponse.ResponseType.SUCCESS)) {
                double newBalance = depositResult.resultingBalance();
                return new EconomyResponse(amount, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
            } else {
                return new EconomyResponse(0, currentBalance, EconomyResponse.ResponseType.FAILURE, depositResult.failureMessage());
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error depositing to player: " + player.getName(), e);
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE,
                                       "Internal error: " + e.getMessage());
        }
    }
    
    /**
     * Executes depositPlayer.
     */
    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }
    
    /**
     * Executes depositPlayer.
     */
    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }
    
    /**
     * Executes createPlayerAccount.
     */
    @Override
    public boolean createPlayerAccount(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return false;
        }
        
        OfflinePlayer player = getOfflinePlayer(playerName);
        return player != null && createPlayerAccount(player);
    }
    
    /**
     * Executes createPlayerAccount.
     */
    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        if (player == null) {
            return false;
        }
        
        Currency currency = getDefaultCurrency();
        if (currency == null) {
            return false;
        }
        
        try {
            // Check if account already exists
            if (hasAccount(player)) {
                return true; // Account already exists
            }
            
            // Create player using CurrencyAdapter
            boolean playerCreated = currencyAdapter.createPlayer(player).join();
            if (!playerCreated) {
                return false;
            }
            
            // Get the user entity
            User userEntity = this.jexEconomyImpl.getUserRepository().findByAttributes(Map.of("uniqueId", player.getUniqueId())).orElse(null);
            if (userEntity == null) {
                return false;
            }
            
            // Create player-currency relationship
            boolean relationshipCreated = currencyAdapter.createPlayerCurrency(userEntity, currency).join();
            
            if (relationshipCreated) {
                logger.info("Created new economy account for player: " + player.getName());
            }
            
            return relationshipCreated;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating account for player: " + player.getName(), e);
            return false;
        }
    }
    
    /**
     * Executes createPlayerAccount.
     */
    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }
    
    /**
     * Executes createPlayerAccount.
     */
    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }
    
    // Bank-related methods (not supported)
    /**
     * Executes createBank.
     */
    @Override
    public EconomyResponse createBank(String name, String player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                   "JExEconomy does not support bank accounts");
    }
    
    /**
     * Executes createBank.
     */
    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                   "JExEconomy does not support bank accounts");
    }
    
    /**
     * Executes deleteBank.
     */
    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                   "JExEconomy does not support bank accounts");
    }
    
    /**
     * Executes bankBalance.
     */
    @Override
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                   "JExEconomy does not support bank accounts");
    }
    
    /**
     * Executes bankHas.
     */
    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                   "JExEconomy does not support bank accounts");
    }
    
    /**
     * Executes bankWithdraw.
     */
    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                   "JExEconomy does not support bank accounts");
    }
    
    /**
     * Executes bankDeposit.
     */
    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                   "JExEconomy does not support bank accounts");
    }
    
    /**
     * Returns whether bankOwner.
     */
    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                   "JExEconomy does not support bank accounts");
    }
    
    /**
     * Returns whether bankOwner.
     */
    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                   "JExEconomy does not support bank accounts");
    }
    
    /**
     * Returns whether bankMember.
     */
    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                   "JExEconomy does not support bank accounts");
    }
    
    /**
     * Returns whether bankMember.
     */
    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                   "JExEconomy does not support bank accounts");
    }
    
    /**
     * Gets banks.
     */
    @Override
    public List<String> getBanks() {
        return List.of(); // Empty list - no bank support
    }
    
    // Utility methods
    
    /**
     * Gets the default currency, with caching to improve performance.
     */
    @Nullable
    private Currency getDefaultCurrency() {
        long currentTime = System.currentTimeMillis();
        
        // Check cache validity
        if (defaultCurrency != null && (currentTime - lastCurrencyCheck) < CURRENCY_CACHE_DURATION) {
            return defaultCurrency;
        }
        
        try {
            // Find default currency from the plugin's currency map
/*             for (Currency currency : this.jexEconomyImpl.getCurrencies().values()) {
                if (currency.isDefaultCurrency()) {
                    defaultCurrency = currency;
                    lastCurrencyCheck = currentTime;
                    return defaultCurrency;
                }
            } */
            
            // If no default currency found, try to get the first available currency
            if (!this.jexEconomyImpl.getCurrencies().isEmpty()) {
                defaultCurrency = this.jexEconomyImpl.getCurrencies().values().iterator().next();
                lastCurrencyCheck = currentTime;
                logger.warning("No default currency found, using first available: " + defaultCurrency.getIdentifier());
                return defaultCurrency;
            }
            
            logger.warning("No currencies found! Vault operations will fail.");
            return null;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting default currency", e);
            return null;
        }
    }
    
    /**
     * Gets an OfflinePlayer by name.
     */
    @Nullable
    private OfflinePlayer getOfflinePlayer(@NotNull String playerName) {
        try {
            // Try to get online player first (more accurate)
            if (Bukkit.getPlayer(playerName) != null) {
                return Bukkit.getPlayer(playerName);
            }
            
            // Fall back to offline player
            return Bukkit.getOfflinePlayer(playerName);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting player: " + playerName, e);
            return null;
        }
    }
    
    /**
     * Invalidates the currency cache (useful when currencies are modified).
     */
    public void invalidateCurrencyCache() {
        defaultCurrency = null;
        lastCurrencyCheck = 0;
    }
}
