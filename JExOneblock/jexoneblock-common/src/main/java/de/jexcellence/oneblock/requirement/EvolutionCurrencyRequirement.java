package de.jexcellence.oneblock.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

/**
 * Evolution Currency Requirement - requires currency for evolution advancement.
 * Extends RPlatform's AbstractRequirement for unified requirement handling.
 *
 * @author JExcellence
 * @version 2.1.0
 * @since 2.0.0
 */
@JsonTypeName("EVOLUTION_CURRENCY")
public class EvolutionCurrencyRequirement extends AbstractRequirement {

    private static final Logger LOGGER = Logger.getLogger("JExOneblock");

    public enum CurrencyType {
        ISLAND_COINS("Island Coins"),
        VAULT_ECONOMY("Vault Economy");

        private final String displayName;

        CurrencyType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @JsonProperty("requiredAmount")
    private final long requiredAmount;

    @JsonProperty("currencyType")
    private final CurrencyType currencyType;

    @JsonProperty("evolutionName")
    @Nullable
    private final String evolutionName;

    @JsonCreator
    public EvolutionCurrencyRequirement(
            @JsonProperty("requiredAmount") long requiredAmount,
            @JsonProperty("currencyType") @Nullable CurrencyType currencyType,
            @JsonProperty("evolutionName") @Nullable String evolutionName,
            @JsonProperty("consumeOnComplete") @Nullable Boolean consumeOnComplete
    ) {
        super("CURRENCY", consumeOnComplete != null ? consumeOnComplete : true);
        this.requiredAmount = requiredAmount;
        this.currencyType = currencyType != null ? currencyType : CurrencyType.ISLAND_COINS;
        this.evolutionName = evolutionName;
    }

    public EvolutionCurrencyRequirement(long requiredAmount, CurrencyType currencyType) {
        this(requiredAmount, currencyType, null, true);
    }

    public EvolutionCurrencyRequirement(long requiredAmount) {
        this(requiredAmount, CurrencyType.ISLAND_COINS, null, true);
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        return getCurrentAmount(player) >= requiredAmount;
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        if (requiredAmount <= 0) return 1.0;
        return Math.min(1.0, (double) getCurrentAmount(player) / requiredAmount);
    }

    @Override
    public void consume(@NotNull Player player) {
        if (!shouldConsume()) return;

        long current = getCurrentAmount(player);
        if (current < requiredAmount) {
            LOGGER.warning("Cannot consume currency - player " + player.getName() +
                    " doesn't have enough " + currencyType.getDisplayName());
            return;
        }

        switch (currencyType) {
            case ISLAND_COINS -> consumeIslandCoins(player, requiredAmount);
            case VAULT_ECONOMY -> consumeVaultEconomy(player, requiredAmount);
        }
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "evolution.requirement.currency";
    }

    private long getCurrentAmount(@NotNull Player player) {
        return switch (currencyType) {
            case ISLAND_COINS -> getIslandCoins(player);
            case VAULT_ECONOMY -> getVaultBalance(player);
        };
    }

    private long getIslandCoins(@NotNull Player player) {
        try {
            var provider = Bukkit.getServicesManager()
                    .getRegistration(Class.forName("de.jexcellence.oneblock.repository.OneblockIslandRepository"));
            if (provider != null) {
                var repo = provider.getProvider();
                var method = repo.getClass().getMethod("findByOwnerUuid", java.util.UUID.class);
                @SuppressWarnings("unchecked")
                var result = (java.util.Optional<?>) method.invoke(repo, player.getUniqueId());
                if (result.isPresent()) {
                    var island = result.get();
                    var coinsMethod = island.getClass().getMethod("getIslandCoins");
                    return (Long) coinsMethod.invoke(island);
                }
            }
        } catch (Exception e) {
            LOGGER.fine("Could not get island coins: " + e.getMessage());
        }
        return 0;
    }

    private long getVaultBalance(@NotNull Player player) {
        try {
            var provider = Bukkit.getServicesManager()
                    .getRegistration(Class.forName("net.milkbowl.vault.economy.Economy"));
            if (provider != null) {
                var economy = provider.getProvider();
                var method = economy.getClass().getMethod("getBalance", org.bukkit.OfflinePlayer.class);
                return ((Number) method.invoke(economy, player)).longValue();
            }
        } catch (Exception e) {
            LOGGER.fine("Could not get vault balance: " + e.getMessage());
        }
        return 0;
    }

    private void consumeIslandCoins(@NotNull Player player, long amount) {
        try {
            var provider = Bukkit.getServicesManager()
                    .getRegistration(Class.forName("de.jexcellence.oneblock.repository.OneblockIslandRepository"));
            if (provider != null) {
                var repo = provider.getProvider();
                var method = repo.getClass().getMethod("findByOwnerUuid", java.util.UUID.class);
                @SuppressWarnings("unchecked")
                var result = (java.util.Optional<?>) method.invoke(repo, player.getUniqueId());
                if (result.isPresent()) {
                    var island = result.get();
                    var removeMethod = island.getClass().getMethod("removeCoins", long.class);
                    removeMethod.invoke(island, amount);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to consume island coins: " + e.getMessage());
        }
    }

    private void consumeVaultEconomy(@NotNull Player player, long amount) {
        try {
            var provider = Bukkit.getServicesManager()
                    .getRegistration(Class.forName("net.milkbowl.vault.economy.Economy"));
            if (provider != null) {
                var economy = provider.getProvider();
                var method = economy.getClass().getMethod("withdrawPlayer", org.bukkit.OfflinePlayer.class, double.class);
                method.invoke(economy, player, (double) amount);
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to consume vault economy: " + e.getMessage());
        }
    }

    public long getRequiredAmount() {
        return requiredAmount;
    }

    @NotNull
    public CurrencyType getCurrencyType() {
        return currencyType;
    }

    @Nullable
    public String getEvolutionName() {
        return evolutionName;
    }

    @Override
    public String toString() {
        return "EvolutionCurrencyRequirement{amount=" + requiredAmount +
                ", type=" + currencyType + ", evolution='" + evolutionName + "'}";
    }
}
