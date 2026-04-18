package de.jexcellence.jexplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.reward.AbstractReward;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Shows a title and subtitle to the player using Adventure API.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class TitleReward extends AbstractReward {

    @JsonProperty("title") private final String title;
    @JsonProperty("subtitle") private final String subtitle;
    @JsonProperty("fadeIn") private final int fadeIn;
    @JsonProperty("stay") private final int stay;
    @JsonProperty("fadeOut") private final int fadeOut;

    public TitleReward(@JsonProperty("title") String title,
                       @JsonProperty("subtitle") String subtitle,
                       @JsonProperty("fadeIn") int fadeIn,
                       @JsonProperty("stay") int stay,
                       @JsonProperty("fadeOut") int fadeOut) {
        super("TITLE");
        this.title = title != null ? title : "";
        this.subtitle = subtitle != null ? subtitle : "";
        this.fadeIn = fadeIn > 0 ? fadeIn : 10;
        this.stay = stay > 0 ? stay : 70;
        this.fadeOut = fadeOut > 0 ? fadeOut : 20;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        var mm = MiniMessage.miniMessage();
        var titleComponent = title.isEmpty() ? Component.empty() : mm.deserialize(title);
        var subtitleComponent = subtitle.isEmpty() ? Component.empty() : mm.deserialize(subtitle);

        var times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L));

        player.showTitle(Title.title(titleComponent, subtitleComponent, times));
        return CompletableFuture.completedFuture(true);
    }

    @Override public @NotNull String descriptionKey() { return "reward.title"; }
}
