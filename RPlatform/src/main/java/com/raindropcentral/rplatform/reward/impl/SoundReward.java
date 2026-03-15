package com.raindropcentral.rplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Reward that plays a sound to the player.
 */
public final class SoundReward extends AbstractReward {

    private final Sound sound;
    private final float volume;
    private final float pitch;

    /**
     * Executes SoundReward.
     */
    @JsonCreator
    public SoundReward(
        @JsonProperty("sound") @NotNull Sound sound,
        @JsonProperty("volume") float volume,
        @JsonProperty("pitch") float pitch
    ) {
        this.sound = sound;
        this.volume = Math.max(0.0f, Math.min(2.0f, volume));
        this.pitch = Math.max(0.5f, Math.min(2.0f, pitch));
    }

    /**
     * Gets typeId.
     */
    @Override
    public @NotNull String getTypeId() {
        return "SOUND";
    }

    /**
     * Executes grant.
     */
    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                player.playSound(player.getLocation(), sound, volume, pitch);
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * Gets estimatedValue.
     */
    @Override
    public double getEstimatedValue() {
        return 0.0;
    }

    /**
     * Gets sound.
     */
    public Sound getSound() {
        return sound;
    }

    /**
     * Gets volume.
     */
    public float getVolume() {
        return volume;
    }

    /**
     * Gets pitch.
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * Executes validate.
     */
    @Override
    public void validate() {
        if (sound == null) {
            throw new IllegalArgumentException("Sound cannot be null");
        }
    }
}
