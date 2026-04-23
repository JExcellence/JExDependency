package de.jexcellence.core.service.reward;

import de.jexcellence.core.api.reward.Reward;
import de.jexcellence.core.api.reward.RewardContext;
import de.jexcellence.core.api.reward.RewardExecutor;
import de.jexcellence.core.api.reward.RewardHandler;
import de.jexcellence.core.api.reward.RewardResult;
import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default {@link RewardExecutor} bundled with JExCore. Built-in handlers
 * cover XP, Item, Command, and Composite. Currency routes through
 * Vault's {@code Economy} service when installed — absent Vault, a
 * {@link RewardResult.Denied} is returned.
 *
 * <p>All Bukkit operations run on the main thread. The async
 * {@link #grant} variant schedules via the Bukkit scheduler.
 */
public final class DefaultRewardExecutor implements RewardExecutor {

    private final JavaPlugin plugin;
    private final JExLogger logger;
    private final ConcurrentMap<String, RewardHandler> customHandlers = new ConcurrentHashMap<>();

    public DefaultRewardExecutor(@NotNull JavaPlugin plugin, @NotNull JExLogger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    @Override
    public @NotNull CompletableFuture<RewardResult> grant(@NotNull Reward reward, @NotNull RewardContext context) {
        final CompletableFuture<RewardResult> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            try {
                future.complete(grantSync(reward, context));
            } catch (final RuntimeException ex) {
                this.logger.error("reward dispatch failed: {}", ex.getMessage());
                future.complete(RewardResult.failed(ex.getClass().getSimpleName() + ": " + ex.getMessage()));
            }
        });
        return future;
    }

    @Override
    public @NotNull RewardResult grantSync(@NotNull Reward reward, @NotNull RewardContext context) {
        return switch (reward) {
            case Reward.Xp xp -> grantXp(xp, context);
            case Reward.Currency c -> grantCurrency(c, context);
            case Reward.Item i -> grantItem(i, context);
            case Reward.Command c -> dispatchCommand(c, context);
            case Reward.Composite c -> grantComposite(c, context);
            case Reward.Custom c -> dispatchCustom(c, context);
        };
    }

    @Override
    public void registerHandler(@NotNull String type, @NotNull RewardHandler handler) {
        this.customHandlers.put(type, handler);
    }

    @Override
    public void unregisterHandler(@NotNull String type) {
        this.customHandlers.remove(type);
    }

    private @NotNull RewardResult grantXp(@NotNull Reward.Xp xp, @NotNull RewardContext ctx) {
        final Player player = Bukkit.getPlayer(ctx.playerUuid());
        if (player == null) return RewardResult.denied("player offline");
        player.giveExp(xp.amount());
        return RewardResult.granted("xp+" + xp.amount());
    }

    private @NotNull RewardResult grantCurrency(@NotNull Reward.Currency c, @NotNull RewardContext ctx) {
        final VaultEconomyBridge bridge = VaultEconomyBridge.tryLoad();
        if (bridge == null) return RewardResult.denied("Vault economy not available");
        final Player player = Bukkit.getPlayer(ctx.playerUuid());
        if (player == null) return RewardResult.denied("player offline");
        return bridge.deposit(player, c.currency(), c.amount());
    }

    private @NotNull RewardResult grantItem(@NotNull Reward.Item item, @NotNull RewardContext ctx) {
        final Player player = Bukkit.getPlayer(ctx.playerUuid());
        if (player == null) return RewardResult.denied("player offline");
        final Material material = resolveMaterial(item.materialKey());
        if (material == null) return RewardResult.failed("unknown material: " + item.materialKey());
        final ItemStack stack = new ItemStack(material, item.amount());
        final var overflow = player.getInventory().addItem(stack);
        if (!overflow.isEmpty()) {
            overflow.values().forEach(overflowed ->
                    player.getWorld().dropItemNaturally(player.getLocation(), overflowed));
            return RewardResult.granted("item " + material.name() + " x" + item.amount() + " (overflow dropped)");
        }
        return RewardResult.granted("item " + material.name() + " x" + item.amount());
    }

    private @NotNull RewardResult dispatchCommand(@NotNull Reward.Command command, @NotNull RewardContext ctx) {
        final Player player = Bukkit.getPlayer(ctx.playerUuid());
        final String name = player != null ? player.getName() : ctx.playerUuid().toString();
        final String expanded = command.command().replace("{player}", name);
        final boolean dispatched = command.asConsole()
                ? Bukkit.dispatchCommand(Bukkit.getConsoleSender(), expanded)
                : player != null && Bukkit.dispatchCommand(player, expanded);
        return dispatched
                ? RewardResult.granted("command: " + expanded)
                : RewardResult.failed("command dispatch rejected: " + expanded);
    }

    private @NotNull RewardResult grantComposite(@NotNull Reward.Composite composite, @NotNull RewardContext ctx) {
        final StringBuilder summary = new StringBuilder("composite(");
        int granted = 0;
        for (final Reward child : composite.children()) {
            final RewardResult result = grantSync(child, ctx);
            if (result instanceof RewardResult.Failed f) {
                return RewardResult.failed("child failed: " + f.error());
            }
            if (result instanceof RewardResult.Granted g) {
                if (granted > 0) summary.append(", ");
                summary.append(g.summary());
                granted++;
            }
        }
        return RewardResult.granted(summary.append(")").toString());
    }

    private @NotNull RewardResult dispatchCustom(@NotNull Reward.Custom custom, @NotNull RewardContext ctx) {
        final RewardHandler handler = this.customHandlers.get(custom.type());
        if (handler == null) return RewardResult.failed("no handler for custom reward type: " + custom.type());
        return handler.grant(custom, ctx);
    }

    private static Material resolveMaterial(@NotNull String key) {
        try {
            final NamespacedKey nsKey = NamespacedKey.fromString(key.toLowerCase(Locale.ROOT));
            if (nsKey != null) {
                final Material byKey = Registry.MATERIAL.get(nsKey);
                if (byKey != null) return byKey;
            }
            return Material.matchMaterial(key);
        } catch (final IllegalArgumentException ex) {
            return null;
        }
    }

    /** Vault bridge isolated so the main class doesn't hard-reference Vault at link time. */
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

        @NotNull RewardResult deposit(@NotNull Player player, @NotNull String currency, double amount) {
            final net.milkbowl.vault.economy.EconomyResponse response = this.economy.depositPlayer(player, amount);
            return response.transactionSuccess()
                    ? RewardResult.granted("currency " + currency + " +" + amount)
                    : RewardResult.failed("vault deposit rejected: " + response.errorMessage);
        }
    }
}
