package com.raindropcentral.rdq.rank.notification;

import com.raindropcentral.rdq.rank.Rank;
import com.raindropcentral.rdq.rank.config.RankSystemConfig;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.function.Function;
import java.util.logging.Logger;

public final class RankNotificationService {

    private static final Logger LOGGER = Logger.getLogger(RankNotificationService.class.getName());
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final RankSystemConfig.NotificationConfig config;
    private final Function<String, String> translationProvider;

    public RankNotificationService(
        @NotNull RankSystemConfig.NotificationConfig config,
        @NotNull Function<String, String> translationProvider
    ) {
        this.config = config;
        this.translationProvider = translationProvider;
    }

    public void notifyRankUnlock(@NotNull Player player, @NotNull Rank rank) {
        if (config.titleEnabled()) {
            sendTitle(player, rank);
        }

        if (config.actionbarEnabled()) {
            sendActionBar(player, rank);
        }

        if (config.soundEnabled()) {
            playSound(player);
        }

        if (config.broadcastEnabled()) {
            broadcastUnlock(player, rank);
        }
    }

    private void sendTitle(@NotNull Player player, @NotNull Rank rank) {
        var titleText = translate("rank.unlock.title", rank);
        var subtitleText = config.subtitleEnabled()
            ? translate("rank.unlock.subtitle", rank)
            : Component.empty();

        var title = Title.title(
            titleText,
            subtitleText,
            Title.Times.times(
                Duration.ofMillis(500),
                Duration.ofSeconds(3),
                Duration.ofMillis(500)
            )
        );

        player.showTitle(title);
    }


    private void sendActionBar(@NotNull Player player, @NotNull Rank rank) {
        var message = translate("rank.unlock.actionbar", rank);
        player.sendActionBar(message);
    }

    private void playSound(@NotNull Player player) {
        try {
            var soundKey = org.bukkit.Sound.valueOf(config.unlockSound());
            var sound = Sound.sound(
                soundKey.key(),
                Sound.Source.MASTER,
                1.0f,
                1.0f
            );
            player.playSound(sound);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid sound configured: " + config.unlockSound());
        }
    }

    private void broadcastUnlock(@NotNull Player player, @NotNull Rank rank) {
        var message = translate("rank.unlock.broadcast", rank)
            .replaceText(builder -> builder
                .matchLiteral("{player}")
                .replacement(player.getName())
            );

        for (var onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
        }
    }

    private Component translate(@NotNull String key, @NotNull Rank rank) {
        var template = translationProvider.apply(key);
        if (template == null || template.isBlank()) {
            template = key;
        }

        var formatted = template
            .replace("{rank}", rank.displayNameKey())
            .replace("{tier}", String.valueOf(rank.tier()))
            .replace("{tree}", rank.treeId());

        return MINI_MESSAGE.deserialize(formatted);
    }

    public static RankNotificationService withDefaults(@NotNull Function<String, String> translationProvider) {
        return new RankNotificationService(
            RankSystemConfig.NotificationConfig.defaults(),
            translationProvider
        );
    }
}
