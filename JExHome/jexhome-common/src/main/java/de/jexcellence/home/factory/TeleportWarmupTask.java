package de.jexcellence.home.factory;

import de.jexcellence.home.config.HomeSystemConfig;
import de.jexcellence.home.database.entity.Home;
import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles teleport warmup with movement and damage cancellation.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class TeleportWarmupTask implements Listener {

    private final JavaPlugin plugin;
    private final Player player;
    private final Home home;
    private final HomeSystemConfig config;
    private final Runnable onComplete;
    private final Runnable onCancel;
    private final Location startLocation;

    private BukkitTask countdownTask;
    private int remainingSeconds;
    private boolean cancelled = false;

    public TeleportWarmupTask(
        @NotNull JavaPlugin plugin,
        @NotNull Player player,
        @NotNull Home home,
        @NotNull HomeSystemConfig config,
        @NotNull Runnable onComplete,
        @Nullable Runnable onCancel
    ) {
        this.plugin = plugin;
        this.player = player;
        this.home = home;
        this.config = config;
        this.onComplete = onComplete;
        this.onCancel = onCancel;
        this.startLocation = player.getLocation().clone();
    }

    /**
     * Starts the warmup countdown.
     *
     * @param seconds the warmup duration in seconds
     */
    public void start(int seconds) {
        this.remainingSeconds = seconds;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        countdownTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (cancelled) {
                cleanup();
                return;
            }

            if (remainingSeconds <= 0) {
                cleanup();
                onComplete.run();
                return;
            }

            showCountdown();
            remainingSeconds--;
        }, 0L, 20L);
    }

    /**
     * Cancels the warmup.
     */
    public void cancel() {
        if (cancelled) return;
        cancelled = true;
        cleanup();
        if (onCancel != null) {
            onCancel.run();
        }
    }

    private void cleanup() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        HandlerList.unregisterAll(this);
    }

    private void showCountdown() {
        if (!config.isShowCountdown()) return;
        
        player.sendActionBar((Component) new I18n.Builder("teleport.warmup", player)
                .withPlaceholder("seconds", String.valueOf(remainingSeconds))
                .build()
                .component());

        if (config.isPlaySounds()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f + (0.1f * (config.getTeleportDelay() - remainingSeconds)));
        }
    }

    private void cancelWithMessage(@NotNull String messageKey) {
        cancel();
        new I18n.Builder(messageKey, player)
            .includePrefix()
            .build()
            .sendMessage();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        if (!event.getPlayer().equals(player)) return;
        if (!config.isCancelOnMove()) return;

        var from = event.getFrom();
        var to = event.getTo();
        if (to == null) return;

        if (from.getBlockX() != to.getBlockX() ||
            from.getBlockY() != to.getBlockY() ||
            from.getBlockZ() != to.getBlockZ()) {
            cancelWithMessage("teleport.cancelled.moved");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(@NotNull EntityDamageEvent event) {
        if (!event.getEntity().equals(player)) return;
        if (!config.isCancelOnDamage()) return;

        cancelWithMessage("teleport.cancelled.damaged");
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        if (!event.getPlayer().equals(player)) return;
        cancel();
    }
}
