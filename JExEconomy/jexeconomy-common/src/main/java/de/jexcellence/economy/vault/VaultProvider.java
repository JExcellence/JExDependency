package de.jexcellence.economy.vault;

import de.jexcellence.economy.database.entity.Account;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.service.EconomyService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Bridges Vault's synchronous single-currency API to
 * {@link EconomyService}'s asynchronous multi-currency backend.
 *
 * <p>All balance, deposit, and withdraw calls block on
 * {@link java.util.concurrent.CompletableFuture#join()} because
 * the Vault contract is inherently synchronous. The first
 * available currency is used as the default and cached with a
 * 30-second TTL.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class VaultProvider implements Economy {

    private static final long CACHE_TTL_MS = 30_000L;
    private static final EconomyResponse BANK_NOT_SUPPORTED = new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "Banks not supported");

    private final @NotNull EconomyService economyService;

    private volatile @Nullable Currency cachedDefault;
    private volatile long cacheTimestamp;

    /**
     * Creates a new Vault provider backed by the given economy service.
     *
     * @param economyService the JExEconomy service layer
     */
    public VaultProvider(@NotNull EconomyService economyService) {
        this.economyService = economyService;
    }

    /**
     * Registers this provider with Bukkit's {@link org.bukkit.plugin.ServicesManager}
     * at {@link ServicePriority#Highest}.
     *
     * @param plugin the owning plugin instance
     */
    public void register(@NotNull JavaPlugin plugin) {
        Bukkit.getServicesManager().register(Economy.class, this, plugin, ServicePriority.Highest);
    }

    // ── Economy metadata ────────────────────────────────────────────────────────

    @Override
    public boolean isEnabled() {
        return !economyService.getAllCurrencies().isEmpty();
    }

    @Override
    public @NotNull String getName() {
        return "JExEconomy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public @NotNull String format(double amount) {
        var currency = getDefaultCurrency();
        if (currency == null) {
            return String.format("%.2f", amount);
        }
        return currency.format(amount);
    }

    @Override
    public @NotNull String currencyNameSingular() {
        var currency = getDefaultCurrency();
        return currency != null ? currency.getIdentifier() : "currency";
    }

    @Override
    public @NotNull String currencyNamePlural() {
        var currency = getDefaultCurrency();
        return currency != null ? currency.getIdentifier() + "s" : "currencies";
    }

    // ── Account queries ─────────────────────────────────────────────────────────

    @Override
    public boolean hasAccount(@NotNull String playerName) {
        var player = resolveOffline(playerName);
        return player != null && hasAccount(player);
    }

    @Override
    public boolean hasAccount(@NotNull OfflinePlayer player) {
        var currency = getDefaultCurrency();
        if (currency == null) {
            return false;
        }
        try {
            return economyService.getAccount(player, currency).join().isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean hasAccount(@NotNull String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(@NotNull OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    // ── Balance ─────────────────────────────────────────────────────────────────

    @Override
    public double getBalance(@NotNull String playerName) {
        var player = resolveOffline(playerName);
        return player != null ? getBalance(player) : 0.0;
    }

    @Override
    public double getBalance(@NotNull OfflinePlayer player) {
        var currency = getDefaultCurrency();
        if (currency == null) {
            return 0.0;
        }
        try {
            return economyService.getAccount(player, currency).join()
                    .map(Account::getBalance).orElse(0.0);
        } catch (Exception e) {
            return 0.0;
        }
    }

    @Override
    public double getBalance(@NotNull String playerName, String worldName) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(@NotNull OfflinePlayer player, String worldName) {
        return getBalance(player);
    }

    // ── Has amount ──────────────────────────────────────────────────────────────

    @Override
    public boolean has(@NotNull String playerName, double amount) {
        return getBalance(playerName) >= amount;
    }

    @Override
    public boolean has(@NotNull OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(@NotNull String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(@NotNull OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    // ── Withdraw ────────────────────────────────────────────────────────────────

    @Override
    public @NotNull EconomyResponse withdrawPlayer(@NotNull String playerName, double amount) {
        var player = resolveOffline(playerName);
        if (player == null) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Player not found");
        }
        return withdrawPlayer(player, amount);
    }

    @Override
    public @NotNull EconomyResponse withdrawPlayer(@NotNull OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), ResponseType.FAILURE,
                    "Cannot withdraw negative amount");
        }
        var currency = getDefaultCurrency();
        if (currency == null) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "No currency available");
        }
        try {
            var result = economyService.withdraw(player, currency, amount, null, null).join();
            return toVaultResponse(result, amount);
        } catch (Exception e) {
            return new EconomyResponse(0, getBalance(player), ResponseType.FAILURE,
                    "Internal error: " + e.getMessage());
        }
    }

    @Override
    public @NotNull EconomyResponse withdrawPlayer(@NotNull String playerName,
                                                    String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public @NotNull EconomyResponse withdrawPlayer(@NotNull OfflinePlayer player,
                                                    String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    // ── Deposit ─────────────────────────────────────────────────────────────────

    @Override
    public @NotNull EconomyResponse depositPlayer(@NotNull String playerName, double amount) {
        var player = resolveOffline(playerName);
        if (player == null) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Player not found");
        }
        return depositPlayer(player, amount);
    }

    @Override
    public @NotNull EconomyResponse depositPlayer(@NotNull OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), ResponseType.FAILURE,
                    "Cannot deposit negative amount");
        }
        var currency = getDefaultCurrency();
        if (currency == null) {
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "No currency available");
        }
        try {
            var result = economyService.deposit(player, currency, amount, null, null).join();
            return toVaultResponse(result, amount);
        } catch (Exception e) {
            return new EconomyResponse(0, getBalance(player), ResponseType.FAILURE,
                    "Internal error: " + e.getMessage());
        }
    }

    @Override
    public @NotNull EconomyResponse depositPlayer(@NotNull String playerName,
                                                   String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public @NotNull EconomyResponse depositPlayer(@NotNull OfflinePlayer player,
                                                   String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    // ── Account creation ────────────────────────────────────────────────────────

    @Override
    public boolean createPlayerAccount(@NotNull String playerName) {
        var player = resolveOffline(playerName);
        return player != null && createPlayerAccount(player);
    }

    @Override
    public boolean createPlayerAccount(@NotNull OfflinePlayer player) {
        var currency = getDefaultCurrency();
        if (currency == null) {
            return false;
        }
        try {
            var account = economyService.getOrCreateAccount(player.getUniqueId(), currency).join();
            return account != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean createPlayerAccount(@NotNull String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(@NotNull OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

    // ── Bank operations (not supported) ─────────────────────────────────────────

    @Override
    public @NotNull EconomyResponse createBank(String name, String player) {
        return BANK_NOT_SUPPORTED;
    }

    @Override
    public @NotNull EconomyResponse createBank(String name, OfflinePlayer player) {
        return BANK_NOT_SUPPORTED;
    }

    @Override
    public @NotNull EconomyResponse deleteBank(String name) {
        return BANK_NOT_SUPPORTED;
    }

    @Override
    public @NotNull EconomyResponse bankBalance(String name) {
        return BANK_NOT_SUPPORTED;
    }

    @Override
    public @NotNull EconomyResponse bankHas(String name, double amount) {
        return BANK_NOT_SUPPORTED;
    }

    @Override
    public @NotNull EconomyResponse bankWithdraw(String name, double amount) {
        return BANK_NOT_SUPPORTED;
    }

    @Override
    public @NotNull EconomyResponse bankDeposit(String name, double amount) {
        return BANK_NOT_SUPPORTED;
    }

    @Override
    public @NotNull EconomyResponse isBankOwner(String name, String playerName) {
        return BANK_NOT_SUPPORTED;
    }

    @Override
    public @NotNull EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return BANK_NOT_SUPPORTED;
    }

    @Override
    public @NotNull EconomyResponse isBankMember(String name, String playerName) {
        return BANK_NOT_SUPPORTED;
    }

    @Override
    public @NotNull EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return BANK_NOT_SUPPORTED;
    }

    @Override
    public @NotNull List<String> getBanks() {
        return List.of();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /**
     * Returns the first available currency, cached for 30 seconds.
     *
     * @return the default currency, or null if none exist
     */
    private @Nullable Currency getDefaultCurrency() {
        var now = System.currentTimeMillis();
        if (cachedDefault != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
            return cachedDefault;
        }
        var currencies = economyService.getAllCurrencies();
        if (currencies.isEmpty()) {
            return null;
        }
        cachedDefault = currencies.values().iterator().next();
        cacheTimestamp = now;
        return cachedDefault;
    }

    /**
     * Maps a {@link de.jexcellence.economy.api.TransactionResult} to a Vault
     * {@link EconomyResponse}.
     *
     * @param result the service-layer result
     * @param amount the requested transaction amount
     * @return the corresponding Vault response
     */
    private static @NotNull EconomyResponse toVaultResponse(
            @NotNull de.jexcellence.economy.api.TransactionResult result,
            double amount) {
        if (result.isSuccess()) {
            return new EconomyResponse(amount, result.balance(), ResponseType.SUCCESS, null);
        }
        return new EconomyResponse(amount, result.balance(), ResponseType.FAILURE,
                result.error());
    }

    /**
     * Resolves a player name to an {@link OfflinePlayer}.
     *
     * @param playerName the player name
     * @return the offline player, or null
     */
    @SuppressWarnings("deprecation")
    private static @Nullable OfflinePlayer resolveOffline(@Nullable String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return null;
        }
        var online = Bukkit.getPlayerExact(playerName);
        return online != null ? online : Bukkit.getOfflinePlayer(playerName);
    }
}
