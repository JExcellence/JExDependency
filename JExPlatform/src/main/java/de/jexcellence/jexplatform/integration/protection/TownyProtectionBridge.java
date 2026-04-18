package de.jexcellence.jexplatform.integration.protection;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Towny reflection-based protection bridge.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class TownyProtectionBridge extends AbstractReflectionProtectionBridge
        implements ProtectionBridge {

    /**
     * Creates the Towny bridge.
     *
     * @param logger the platform logger
     */
    TownyProtectionBridge(@NotNull JExLogger logger) {
        super(logger);
    }

    @Override
    public @NotNull String pluginName() {
        return "Towny";
    }

    @Override
    public boolean isAvailable() {
        return findClass("com.palmergames.bukkit.towny.TownyAPI") != null;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> isTownMember(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var apiClass = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
                var api = apiClass.getMethod("getInstance").invoke(null);
                var resident = apiClass.getMethod("getResident", Player.class)
                        .invoke(api, player);
                if (resident == null) return false;
                var hasTown = resident.getClass().getMethod("hasTown").invoke(resident);
                return hasTown instanceof Boolean b && b;
            } catch (Exception e) {
                logger.debug("Towny isTownMember failed: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Optional<String>> getTownName(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var apiClass = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
                var api = apiClass.getMethod("getInstance").invoke(null);
                var resident = apiClass.getMethod("getResident", Player.class)
                        .invoke(api, player);
                if (resident == null) return Optional.<String>empty();
                var hasTown = resident.getClass().getMethod("hasTown").invoke(resident);
                if (!(hasTown instanceof Boolean b) || !b) return Optional.<String>empty();
                var town = resident.getClass().getMethod("getTown").invoke(resident);
                var name = town.getClass().getMethod("getName").invoke(town);
                return Optional.of(name.toString());
            } catch (Exception e) {
                logger.debug("Towny getTownName failed: {}", e.getMessage());
                return Optional.empty();
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> isTownMayor(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var apiClass = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
                var api = apiClass.getMethod("getInstance").invoke(null);
                var resident = apiClass.getMethod("getResident", Player.class)
                        .invoke(api, player);
                if (resident == null) return false;
                var isMayor = resident.getClass().getMethod("isMayor").invoke(resident);
                return isMayor instanceof Boolean b && b;
            } catch (Exception e) {
                logger.debug("Towny isTownMayor failed: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> depositToTownBank(@NotNull Player player,
                                                                  double amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var apiClass = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
                var api = apiClass.getMethod("getInstance").invoke(null);
                var resident = apiClass.getMethod("getResident", Player.class)
                        .invoke(api, player);
                if (resident == null) return false;
                var town = resident.getClass().getMethod("getTown").invoke(resident);
                var account = town.getClass().getMethod("getAccount").invoke(town);
                account.getClass().getMethod("deposit", double.class, String.class)
                        .invoke(account, amount, "JExPlatform deposit");
                return true;
            } catch (Exception e) {
                logger.debug("Towny depositToTownBank failed: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> withdrawFromTownBank(@NotNull Player player,
                                                                     double amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var apiClass = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
                var api = apiClass.getMethod("getInstance").invoke(null);
                var resident = apiClass.getMethod("getResident", Player.class)
                        .invoke(api, player);
                if (resident == null) return false;
                var town = resident.getClass().getMethod("getTown").invoke(resident);
                var account = town.getClass().getMethod("getAccount").invoke(town);
                account.getClass().getMethod("withdraw", double.class, String.class)
                        .invoke(account, amount, "JExPlatform withdrawal");
                return true;
            } catch (Exception e) {
                logger.debug("Towny withdrawFromTownBank failed: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Integer> getTownLevel(@NotNull Player player) {
        return CompletableFuture.completedFuture(0);
    }
}
