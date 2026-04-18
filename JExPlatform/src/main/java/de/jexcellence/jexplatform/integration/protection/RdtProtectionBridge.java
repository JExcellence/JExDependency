package de.jexcellence.jexplatform.integration.protection;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * RDTowns reflection-based protection bridge.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class RdtProtectionBridge extends AbstractReflectionProtectionBridge
        implements ProtectionBridge {

    /**
     * Creates the RDTowns bridge.
     *
     * @param logger the platform logger
     */
    RdtProtectionBridge(@NotNull JExLogger logger) {
        super(logger);
    }

    @Override
    public @NotNull String pluginName() {
        return "RDTowns";
    }

    @Override
    public boolean isAvailable() {
        return findClass("com.raindropcentral.rdtowns.api.RDTownsAPI") != null;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> isTownMember(@NotNull Player player) {
        return getTownName(player).thenApply(Optional::isPresent);
    }

    @Override
    public @NotNull CompletableFuture<Optional<String>> getTownName(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var apiClass = Class.forName("com.raindropcentral.rdtowns.api.RDTownsAPI");
                var api = apiClass.getMethod("getInstance").invoke(null);
                var result = apiClass.getMethod("getTownName", Player.class)
                        .invoke(api, player);
                if (result instanceof String s && !s.isEmpty()) {
                    return Optional.of(s);
                }
                return Optional.empty();
            } catch (Exception e) {
                logger.debug("RDTowns getTownName failed: {}", e.getMessage());
                return Optional.empty();
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> isTownMayor(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var apiClass = Class.forName("com.raindropcentral.rdtowns.api.RDTownsAPI");
                var api = apiClass.getMethod("getInstance").invoke(null);
                var result = apiClass.getMethod("isMayor", Player.class)
                        .invoke(api, player);
                return result instanceof Boolean b && b;
            } catch (Exception e) {
                logger.debug("RDTowns isTownMayor failed: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> depositToTownBank(@NotNull Player player,
                                                                  double amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var apiClass = Class.forName("com.raindropcentral.rdtowns.api.RDTownsAPI");
                var api = apiClass.getMethod("getInstance").invoke(null);
                var result = apiClass.getMethod("depositToBank", Player.class, double.class)
                        .invoke(api, player, amount);
                return result instanceof Boolean b && b;
            } catch (Exception e) {
                logger.debug("RDTowns depositToTownBank failed: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> withdrawFromTownBank(@NotNull Player player,
                                                                     double amount) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var apiClass = Class.forName("com.raindropcentral.rdtowns.api.RDTownsAPI");
                var api = apiClass.getMethod("getInstance").invoke(null);
                var result = apiClass.getMethod("withdrawFromBank", Player.class, double.class)
                        .invoke(api, player, amount);
                return result instanceof Boolean b && b;
            } catch (Exception e) {
                logger.debug("RDTowns withdrawFromTownBank failed: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Integer> getTownLevel(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var apiClass = Class.forName("com.raindropcentral.rdtowns.api.RDTownsAPI");
                var api = apiClass.getMethod("getInstance").invoke(null);
                var result = apiClass.getMethod("getTownLevel", Player.class)
                        .invoke(api, player);
                return result instanceof Number n ? n.intValue() : 0;
            } catch (Exception e) {
                logger.debug("RDTowns getTownLevel failed: {}", e.getMessage());
                return 0;
            }
        });
    }
}
