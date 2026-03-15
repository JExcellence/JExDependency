package com.raindropcentral.rplatform.reward.config;

import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.impl.*;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the RewardBuilder API type.
 */
public final class RewardBuilder {

    private RewardBuilder() {}

    /**
     * Executes item.
     */
    public static ItemBuilder item() {
        return new ItemBuilder();
    }

    /**
     * Executes currency.
     */
    public static CurrencyBuilder currency() {
        return new CurrencyBuilder();
    }

    /**
     * Executes experience.
     */
    public static ExperienceBuilder experience() {
        return new ExperienceBuilder();
    }

    /**
     * Executes command.
     */
    public static CommandBuilder command() {
        return new CommandBuilder();
    }

    /**
     * Executes composite.
     */
    public static CompositeBuilder composite() {
        return new CompositeBuilder();
    }

    /**
     * Executes choice.
     */
    public static ChoiceBuilder choice() {
        return new ChoiceBuilder();
    }

    /**
     * Executes permission.
     */
    public static PermissionBuilder permission() {
        return new PermissionBuilder();
    }

    /**
     * Executes sound.
     */
    public static SoundBuilder sound() {
        return new SoundBuilder();
    }

    /**
     * Executes particle.
     */
    public static ParticleBuilder particle() {
        return new ParticleBuilder();
    }

    /**
     * Executes teleport.
     */
    public static TeleportBuilder teleport() {
        return new TeleportBuilder();
    }

    /**
     * Executes vanishingChest.
     */
    public static VanishingChestBuilder vanishingChest() {
        return new VanishingChestBuilder();
    }

    /**
     * Represents the ItemBuilder API type.
     */
    public static class ItemBuilder {
        private ItemStack item;
        private Integer amount;

        /**
         * Executes item.
         */
        public ItemBuilder item(@NotNull ItemStack item) {
            this.item = item;
            return this;
        }

        /**
         * Executes amount.
         */
        public ItemBuilder amount(int amount) {
            this.amount = amount;
            return this;
        }

        /**
         * Executes build.
         */
        public ItemReward build() {
            if (item == null) {
                throw new IllegalStateException("Item must be set");
            }
            if (amount != null) {
                return new ItemReward(item, amount);
            }
            return new ItemReward(item);
        }
    }

    /**
     * Represents the CurrencyBuilder API type.
     */
    public static class CurrencyBuilder {
        private String currencyId = "vault";
        private double amount;

        /**
         * Executes currency.
         */
        public CurrencyBuilder currency(@NotNull String id, double amount) {
            this.currencyId = id;
            this.amount = amount;
            return this;
        }

        /**
         * Executes vault.
         */
        public CurrencyBuilder vault(double amount) {
            return currency("vault", amount);
        }

        /**
         * Executes money.
         */
        public CurrencyBuilder money(double amount) {
            return currency("money", amount);
        }

        /**
         * Executes build.
         */
        public CurrencyReward build() {
            return new CurrencyReward(currencyId, amount);
        }
    }

    /**
     * Represents the ExperienceBuilder API type.
     */
    public static class ExperienceBuilder {
        private int amount;
        private ExperienceReward.ExperienceType type = ExperienceReward.ExperienceType.POINTS;

        /**
         * Executes amount.
         */
        public ExperienceBuilder amount(int amount) {
            this.amount = amount;
            return this;
        }

        /**
         * Executes points.
         */
        public ExperienceBuilder points(int points) {
            this.amount = points;
            this.type = ExperienceReward.ExperienceType.POINTS;
            return this;
        }

        /**
         * Executes levels.
         */
        public ExperienceBuilder levels(int levels) {
            this.amount = levels;
            this.type = ExperienceReward.ExperienceType.LEVELS;
            return this;
        }

        /**
         * Executes type.
         */
        public ExperienceBuilder type(@NotNull ExperienceReward.ExperienceType type) {
            this.type = type;
            return this;
        }

        /**
         * Executes build.
         */
        public ExperienceReward build() {
            return new ExperienceReward(amount, type);
        }
    }

    /**
     * Represents the CommandBuilder API type.
     */
    public static class CommandBuilder {
        private String command;
        private boolean executeAsPlayer = false;
        private long delayTicks = 0;

        /**
         * Executes command.
         */
        public CommandBuilder command(@NotNull String command) {
            this.command = command;
            return this;
        }

        /**
         * Executes asPlayer.
         */
        public CommandBuilder asPlayer() {
            this.executeAsPlayer = true;
            return this;
        }

        /**
         * Executes asConsole.
         */
        public CommandBuilder asConsole() {
            this.executeAsPlayer = false;
            return this;
        }

        /**
         * Executes delay.
         */
        public CommandBuilder delay(long ticks) {
            this.delayTicks = ticks;
            return this;
        }

        /**
         * Executes build.
         */
        public CommandReward build() {
            if (command == null) {
                throw new IllegalStateException("Command must be set");
            }
            return new CommandReward(command, executeAsPlayer, delayTicks);
        }
    }

    /**
     * Represents the CompositeBuilder API type.
     */
    public static class CompositeBuilder {
        private final List<AbstractReward> rewards = new ArrayList<>();
        private boolean continueOnError = false;

        /**
         * Executes add.
         */
        public CompositeBuilder add(@NotNull AbstractReward reward) {
            rewards.add(reward);
            return this;
        }

        /**
         * Executes addAll.
         */
        public CompositeBuilder addAll(@NotNull List<AbstractReward> rewards) {
            this.rewards.addAll(rewards);
            return this;
        }

        /**
         * Executes continueOnError.
         */
        public CompositeBuilder continueOnError(boolean continueOnError) {
            this.continueOnError = continueOnError;
            return this;
        }

        /**
         * Executes build.
         */
        public CompositeReward build() {
            return new CompositeReward(rewards, continueOnError);
        }
    }

    /**
     * Represents the ChoiceBuilder API type.
     */
    public static class ChoiceBuilder {
        private final List<AbstractReward> choices = new ArrayList<>();
        private int minimumRequired = 1;
        private Integer maximumRequired = null;
        private boolean allowMultipleSelections = false;

        /**
         * Executes add.
         */
        public ChoiceBuilder add(@NotNull AbstractReward choice) {
            choices.add(choice);
            return this;
        }

        /**
         * Executes addAll.
         */
        public ChoiceBuilder addAll(@NotNull List<AbstractReward> choices) {
            this.choices.addAll(choices);
            return this;
        }

        /**
         * Executes minimumRequired.
         */
        public ChoiceBuilder minimumRequired(int min) {
            this.minimumRequired = min;
            return this;
        }

        /**
         * Executes maximumRequired.
         */
        public ChoiceBuilder maximumRequired(int max) {
            this.maximumRequired = max;
            return this;
        }

        /**
         * Executes allowMultipleSelections.
         */
        public ChoiceBuilder allowMultipleSelections(boolean allow) {
            this.allowMultipleSelections = allow;
            return this;
        }

        /**
         * Executes singleChoice.
         */
        public ChoiceBuilder singleChoice() {
            this.minimumRequired = 1;
            this.maximumRequired = 1;
            return this;
        }

        /**
         * Executes build.
         */
        public ChoiceReward build() {
            return new ChoiceReward(choices, minimumRequired, maximumRequired, allowMultipleSelections);
        }
    }

    /**
     * Represents the PermissionBuilder API type.
     */
    public static class PermissionBuilder {
        private final List<String> permissions = new ArrayList<>();
        private Long durationSeconds = null;
        private boolean temporary = false;

        /**
         * Executes permission.
         */
        public PermissionBuilder permission(@NotNull String permission) {
            permissions.add(permission);
            return this;
        }

        /**
         * Executes permissions.
         */
        public PermissionBuilder permissions(@NotNull List<String> permissions) {
            this.permissions.addAll(permissions);
            return this;
        }

        /**
         * Executes temporary.
         */
        public PermissionBuilder temporary(long seconds) {
            this.durationSeconds = seconds;
            this.temporary = true;
            return this;
        }

        /**
         * Executes permanent.
         */
        public PermissionBuilder permanent() {
            this.durationSeconds = null;
            this.temporary = false;
            return this;
        }

        /**
         * Executes build.
         */
        public PermissionReward build() {
            return new PermissionReward(permissions, durationSeconds, temporary);
        }
    }

    /**
     * Represents the SoundBuilder API type.
     */
    public static class SoundBuilder {
        private Sound sound;
        private float volume = 1.0f;
        private float pitch = 1.0f;

        /**
         * Executes sound.
         */
        public SoundBuilder sound(@NotNull Sound sound) {
            this.sound = sound;
            return this;
        }

        /**
         * Executes volume.
         */
        public SoundBuilder volume(float volume) {
            this.volume = volume;
            return this;
        }

        /**
         * Executes pitch.
         */
        public SoundBuilder pitch(float pitch) {
            this.pitch = pitch;
            return this;
        }

        /**
         * Executes build.
         */
        public SoundReward build() {
            if (sound == null) {
                throw new IllegalStateException("Sound must be set");
            }
            return new SoundReward(sound, volume, pitch);
        }
    }

    /**
     * Represents the ParticleBuilder API type.
     */
    public static class ParticleBuilder {
        private Particle particle;
        private int count = 10;
        private double offsetX = 0.5;
        private double offsetY = 0.5;
        private double offsetZ = 0.5;
        private double extra = 0.0;

        /**
         * Executes particle.
         */
        public ParticleBuilder particle(@NotNull Particle particle) {
            this.particle = particle;
            return this;
        }

        /**
         * Executes count.
         */
        public ParticleBuilder count(int count) {
            this.count = count;
            return this;
        }

        /**
         * Executes offset.
         */
        public ParticleBuilder offset(double x, double y, double z) {
            this.offsetX = x;
            this.offsetY = y;
            this.offsetZ = z;
            return this;
        }

        /**
         * Executes extra.
         */
        public ParticleBuilder extra(double extra) {
            this.extra = extra;
            return this;
        }

        /**
         * Executes build.
         */
        public ParticleReward build() {
            if (particle == null) {
                throw new IllegalStateException("Particle must be set");
            }
            return new ParticleReward(particle, count, offsetX, offsetY, offsetZ, extra);
        }
    }

    /**
     * Represents the TeleportBuilder API type.
     */
    public static class TeleportBuilder {
        private String worldName;
        private double x;
        private double y;
        private double z;
        private float yaw = 0.0f;
        private float pitch = 0.0f;

        /**
         * Executes world.
         */
        public TeleportBuilder world(@NotNull String worldName) {
            this.worldName = worldName;
            return this;
        }

        /**
         * Executes location.
         */
        public TeleportBuilder location(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        /**
         * Executes rotation.
         */
        public TeleportBuilder rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
            return this;
        }

        /**
         * Executes build.
         */
        public TeleportReward build() {
            if (worldName == null) {
                throw new IllegalStateException("World name must be set");
            }
            return new TeleportReward(worldName, x, y, z, yaw, pitch);
        }
    }

    /**
     * Represents the VanishingChestBuilder API type.
     */
    public static class VanishingChestBuilder {
        private final List<ItemStack> items = new ArrayList<>();
        private long durationTicks = 6000; // 5 minutes default
        private boolean dropItemsOnVanish = true;

        /**
         * Executes addItem.
         */
        public VanishingChestBuilder addItem(@NotNull ItemStack item) {
            items.add(item);
            return this;
        }

        /**
         * Executes items.
         */
        public VanishingChestBuilder items(@NotNull List<ItemStack> items) {
            this.items.addAll(items);
            return this;
        }

        /**
         * Executes duration.
         */
        public VanishingChestBuilder duration(long ticks) {
            this.durationTicks = ticks;
            return this;
        }

        /**
         * Executes durationSeconds.
         */
        public VanishingChestBuilder durationSeconds(int seconds) {
            this.durationTicks = seconds * 20L;
            return this;
        }

        /**
         * Executes dropItemsOnVanish.
         */
        public VanishingChestBuilder dropItemsOnVanish(boolean drop) {
            this.dropItemsOnVanish = drop;
            return this;
        }

        /**
         * Executes build.
         */
        public VanishingChestReward build() {
            if (items.isEmpty()) {
                throw new IllegalStateException("Vanishing chest must have at least one item");
            }
            return new VanishingChestReward(items, durationTicks, dropItemsOnVanish);
        }
    }
}
