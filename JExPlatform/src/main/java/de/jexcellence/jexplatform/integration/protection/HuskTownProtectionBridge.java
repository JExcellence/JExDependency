package de.jexcellence.jexplatform.integration.protection;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * HuskTowns reflection-based protection bridge.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class HuskTownProtectionBridge extends AbstractReflectionProtectionBridge
        implements ProtectionBridge {

    /**
     * Creates the HuskTowns bridge.
     *
     * @param logger the platform logger
     */
    HuskTownProtectionBridge(@NotNull JExLogger logger) {
        super(logger);
    }

    @Override
    public @NotNull String pluginName() {
        return "HuskTowns";
    }

    @Override
    public boolean isAvailable() {
        return findClass("net.william278.husktowns.api.HuskTownsAPI") != null;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> isTownMember(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var apiClass = Class.forName("net.william278.husktowns.api.HuskTownsAPI");
                var api = apiClass.getMethod("getInstance").invoke(null);
                var result = apiClass.getMethod("getUserTown", Player.class)
                        .invoke(api, player);
                if (result instanceof Optional<?> opt) {
                    return opt.isPresent();
                }
                return false;
            } catch (Exception e) {
                logger.debug("HuskTowns isTownMember failed: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Optional<String>> getTownName(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var apiClass = Class.forName("net.william278.husktowns.api.HuskTownsAPI");
                var api = apiClass.getMethod("getInstance").invoke(null);
                var result = apiClass.getMethod("getUserTown", Player.class)
                        .invoke(api, player);
                if (result instanceof Optional<?> opt && opt.isPresent()) {
                    var member = opt.get();
                    var town = member.getClass().getMethod("town").invoke(member);
                    var name = town.getClass().getMethod("getName").invoke(town);
                    return Optional.of(name.toString());
                }
                return Optional.empty();
            } catch (Exception e) {
                logger.debug("HuskTowns getTownName failed: {}", e.getMessage());
                return Optional.empty();
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> isTownMayor(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var apiClass = Class.forName("net.william278.husktowns.api.HuskTownsAPI");
                var api = apiClass.getMethod("getInstance").invoke(null);
                var result = apiClass.getMethod("getUserTown", Player.class)
                        .invoke(api, player);
                if (result instanceof Optional<?> opt && opt.isPresent()) {
                    var member = opt.get();
                    var role = member.getClass().getMethod("role").invoke(member);
                    return role.toString().equalsIgnoreCase("MAYOR");
                }
                return false;
            } catch (Exception e) {
                logger.debug("HuskTowns isTownMayor failed: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> depositToTownBank(@NotNull Player player,
                                                                  double amount) {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> withdrawFromTownBank(@NotNull Player player,
                                                                     double amount) {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public @NotNull CompletableFuture<Integer> getTownLevel(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var apiClass = Class.forName("net.william278.husktowns.api.HuskTownsAPI");
                var api = apiClass.getMethod("getInstance").invoke(null);
                var result = apiClass.getMethod("getUserTown", Player.class)
                        .invoke(api, player);
                if (result instanceof Optional<?> opt && opt.isPresent()) {
                    var member = opt.get();
                    var town = member.getClass().getMethod("town").invoke(member);
                    var level = town.getClass().getMethod("getLevel").invoke(town);
                    return level instanceof Number n ? n.intValue() : 0;
                }
                return 0;
            } catch (Exception e) {
                logger.debug("HuskTowns getTownLevel failed: {}", e.getMessage());
                return 0;
            }
        });
    }
}
