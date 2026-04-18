package de.jexcellence.jexplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.reward.AbstractReward;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Plays a sound for the player.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class SoundReward extends AbstractReward {

    @JsonProperty("sound") private final String sound;
    @JsonProperty("volume") private final float volume;
    @JsonProperty("pitch") private final float pitch;

    public SoundReward(@JsonProperty("sound") @NotNull String sound,
                       @JsonProperty("volume") float volume,
                       @JsonProperty("pitch") float pitch) {
        super("SOUND");
        this.sound = sound;
        this.volume = volume > 0 ? volume : 1.0f;
        this.pitch = pitch > 0 ? pitch : 1.0f;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        try {
            var s = Sound.valueOf(sound.toUpperCase());
            player.playSound(player.getLocation(), s, volume, pitch);
            return CompletableFuture.completedFuture(true);
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override public @NotNull String descriptionKey() { return "reward.sound"; }
}
