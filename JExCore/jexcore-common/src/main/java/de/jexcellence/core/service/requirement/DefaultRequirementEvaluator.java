package de.jexcellence.core.service.requirement;

import de.jexcellence.core.api.requirement.Requirement;
import de.jexcellence.core.api.requirement.RequirementContext;
import de.jexcellence.core.api.requirement.RequirementEvaluator;
import de.jexcellence.core.api.requirement.RequirementHandler;
import de.jexcellence.core.api.requirement.RequirementResult;
import de.jexcellence.jexplatform.logging.JExLogger;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default {@link RequirementEvaluator} bundled with JExCore. Built-in
 * handlers cover Permission, Currency (via Vault), Placeholder (via
 * PlaceholderAPI), and Composite. Statistic / QuestCompleted / Rank
 * degrade gracefully when the originating plugin isn't installed —
 * the result is {@link RequirementResult.Error} with a diagnostic
 * message.
 */
public final class DefaultRequirementEvaluator implements RequirementEvaluator {

    private final JavaPlugin plugin;
    private final JExLogger logger;
    private final ConcurrentMap<String, RequirementHandler> customHandlers = new ConcurrentHashMap<>();

    public DefaultRequirementEvaluator(@NotNull JavaPlugin plugin, @NotNull JExLogger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    @Override
    public @NotNull CompletableFuture<RequirementResult> evaluate(
            @NotNull Requirement requirement,
            @NotNull RequirementContext context
    ) {
        final CompletableFuture<RequirementResult> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            try {
                future.complete(evaluateSync(requirement, context));
            } catch (final RuntimeException ex) {
                this.logger.error("requirement evaluate failed: {}", ex.getMessage());
                future.complete(RequirementResult.error(ex.getClass().getSimpleName() + ": " + ex.getMessage()));
            }
        });
        return future;
    }

    @Override
    public @NotNull RequirementResult evaluateSync(
            @NotNull Requirement requirement,
            @NotNull RequirementContext context
    ) {
        return switch (requirement) {
            case Requirement.Permission p -> evaluatePermission(p, context);
            case Requirement.Currency c -> evaluateCurrency(c, context);
            case Requirement.Statistic s -> evaluateStatistic(s, context);
            case Requirement.QuestCompleted q -> evaluateQuestCompleted(q, context);
            case Requirement.Rank r -> evaluateRank(r, context);
            case Requirement.Placeholder p -> evaluatePlaceholder(p, context);
            case Requirement.Composite c -> evaluateComposite(c, context);
            case Requirement.Custom c -> dispatchCustom(c, context);
        };
    }

    @Override
    public void registerHandler(@NotNull String type, @NotNull RequirementHandler handler) {
        this.customHandlers.put(type, handler);
    }

    @Override
    public void unregisterHandler(@NotNull String type) {
        this.customHandlers.remove(type);
    }

    private @NotNull RequirementResult evaluatePermission(@NotNull Requirement.Permission p, @NotNull RequirementContext ctx) {
        final Player player = Bukkit.getPlayer(ctx.playerUuid());
        if (player == null) return RequirementResult.notMet("player offline");
        return player.hasPermission(p.node())
                ? RequirementResult.met()
                : RequirementResult.notMet("missing permission " + p.node());
    }

    private @NotNull RequirementResult evaluateCurrency(@NotNull Requirement.Currency c, @NotNull RequirementContext ctx) {
        final VaultEconomyBridge bridge = VaultEconomyBridge.tryLoad();
        if (bridge == null) return RequirementResult.error("Vault economy not available");
        final OfflinePlayer player = Bukkit.getOfflinePlayer(ctx.playerUuid());
        final double balance = bridge.balance(player);
        return c.op().compare(balance, c.amount())
                ? RequirementResult.met()
                : RequirementResult.notMet("balance " + balance + " " + c.op().name() + " " + c.amount() + " failed");
    }

    private @NotNull RequirementResult evaluateStatistic(@NotNull Requirement.Statistic s, @NotNull RequirementContext ctx) {
        // Statistic lookup requires a StatisticQueryService hook provided by the plugin owning the stats.
        // Absent such a hook, return Error so the caller can decide policy.
        final RequirementHandler override = this.customHandlers.get("statistic:" + s.plugin());
        if (override != null) return override.evaluate(s, ctx);
        return RequirementResult.error("no statistic provider for " + s.plugin() + "/" + s.identifier());
    }

    private @NotNull RequirementResult evaluateQuestCompleted(@NotNull Requirement.QuestCompleted q, @NotNull RequirementContext ctx) {
        final RequirementHandler override = this.customHandlers.get("quest-completed");
        if (override != null) return override.evaluate(q, ctx);
        return RequirementResult.error("no quest provider registered");
    }

    private @NotNull RequirementResult evaluateRank(@NotNull Requirement.Rank r, @NotNull RequirementContext ctx) {
        final RequirementHandler override = this.customHandlers.get("rank");
        if (override != null) return override.evaluate(r, ctx);
        return RequirementResult.error("no rank provider registered");
    }

    private @NotNull RequirementResult evaluatePlaceholder(@NotNull Requirement.Placeholder p, @NotNull RequirementContext ctx) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return RequirementResult.error("PlaceholderAPI not installed");
        }
        final Player player = Bukkit.getPlayer(ctx.playerUuid());
        if (player == null) return RequirementResult.notMet("player offline");
        final String raw = PlaceholderAPI.setPlaceholders(player, "%" + p.expansion() + "%");
        try {
            final double left = Double.parseDouble(raw);
            final double right = Double.parseDouble(p.value());
            return p.op().compare(left, right)
                    ? RequirementResult.met()
                    : RequirementResult.notMet(raw + " " + p.op().name() + " " + p.value() + " failed");
        } catch (final NumberFormatException nfe) {
            return p.op().compare(raw, p.value())
                    ? RequirementResult.met()
                    : RequirementResult.notMet("'" + raw + "' " + p.op().name() + " '" + p.value() + "' failed");
        }
    }

    private @NotNull RequirementResult evaluateComposite(@NotNull Requirement.Composite c, @NotNull RequirementContext ctx) {
        final var children = c.children();
        int met = 0;
        for (final Requirement child : children) {
            final RequirementResult result = evaluateSync(child, ctx);
            if (result instanceof RequirementResult.Error err) return err;
            if (result.isMet()) met++;
        }
        return switch (c.op()) {
            case AND -> met == children.size() ? RequirementResult.met() : RequirementResult.notMet(met + "/" + children.size() + " conditions met (AND)");
            case OR -> met > 0 ? RequirementResult.met() : RequirementResult.notMet("0/" + children.size() + " conditions met (OR)");
            case XOR -> met == 1 ? RequirementResult.met() : RequirementResult.notMet(met + " conditions met (XOR requires exactly 1)");
            case NONE_OF -> met == 0 ? RequirementResult.met() : RequirementResult.notMet(met + " conditions met (NONE_OF requires 0)");
        };
    }

    private @NotNull RequirementResult dispatchCustom(@NotNull Requirement.Custom custom, @NotNull RequirementContext ctx) {
        final RequirementHandler handler = this.customHandlers.get(custom.type());
        if (handler == null) return RequirementResult.error("no handler for custom requirement type: " + custom.type());
        return handler.evaluate(custom, ctx);
    }

    private static final class VaultEconomyBridge {
        private final net.milkbowl.vault.economy.Economy economy;

        private VaultEconomyBridge(net.milkbowl.vault.economy.Economy economy) {
            this.economy = economy;
        }

        static VaultEconomyBridge tryLoad() {
            try {
                final RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                        Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                return rsp != null ? new VaultEconomyBridge(rsp.getProvider()) : null;
            } catch (final NoClassDefFoundError ex) {
                return null;
            }
        }

        double balance(@NotNull OfflinePlayer player) {
            return this.economy.getBalance(player);
        }
    }
}
