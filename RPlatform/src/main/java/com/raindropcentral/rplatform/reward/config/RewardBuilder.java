package com.raindropcentral.rplatform.reward.config;

import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.impl.*;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class RewardBuilder {

    private RewardBuilder() {}

    public static ItemBuilder item() {
        return new ItemBuilder();
    }

    public static CurrencyBuilder currency() {
        return new CurrencyBuilder();
    }

    public static ExperienceBuilder experience() {
        return new ExperienceBuilder();
    }

    public static CommandBuilder command() {
        return new CommandBuilder();
    }

    public static CompositeBuilder composite() {
        return new CompositeBuilder();
    }

    public static ChoiceBuilder choice() {
        return new ChoiceBuilder();
    }

    public static PermissionBuilder permission() {
        return new PermissionBuilder();
    }

    public static SoundBuilder sound() {
        return new SoundBuilder();
    }

    public static ParticleBuilder particle() {
        return new ParticleBuilder();
    }

    public static TeleportBuilder teleport() {
        return new TeleportBuilder();
    }

    public static VanishingChestBuilder vanishingChest() {
        return new VanishingChestBuilder();
    }

    public static class ItemBuilder {
        private ItemStack item;
        private Integer amount;

        public ItemBuilder item(@NotNull ItemStack item) {
            this.item = item;
            return this;
        }

        public ItemBuilder amount(int amount) {
            this.amount = amount;
            return this;
        }

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

    public static class CurrencyBuilder {
        private String currencyId = "vault";
        private double amount;

        public CurrencyBuilder currency(@NotNull String id, double amount) {
            this.currencyId = id;
            this.amount = amount;
            return this;
        }

        public CurrencyBuilder vault(double amount) {
            return currency("vault", amount);
        }

        public CurrencyBuilder money(double amount) {
            return currency("money", amount);
        }

        public CurrencyReward build() {
            return new CurrencyReward(currencyId, amount);
        }
    }

    public static class ExperienceBuilder {
        private int amount;
        private ExperienceReward.ExperienceType type = ExperienceReward.ExperienceType.POINTS;

        public ExperienceBuilder amount(int amount) {
            this.amount = amount;
            return this;
        }

        public ExperienceBuilder points(int points) {
            this.amount = points;
            this.type = ExperienceReward.ExperienceType.POINTS;
            return this;
        }

        public ExperienceBuilder levels(int levels) {
            this.amount = levels;
            this.type = ExperienceReward.ExperienceType.LEVELS;
            return this;
        }

        public ExperienceBuilder type(@NotNull ExperienceReward.ExperienceType type) {
            this.type = type;
            return this;
        }

        public ExperienceReward build() {
            return new ExperienceReward(amount, type);
        }
    }

    public static class CommandBuilder {
        private String command;
        private boolean executeAsPlayer = false;
        private long delayTicks = 0;

        public CommandBuilder command(@NotNull String command) {
            this.command = command;
            return this;
        }

        public CommandBuilder asPlayer() {
            this.executeAsPlayer = true;
            return this;
        }

        public CommandBuilder asConsole() {
            this.executeAsPlayer = false;
            return this;
        }

        public CommandBuilder delay(long ticks) {
            this.delayTicks = ticks;
            return this;
        }

        public CommandReward build() {
            if (command == null) {
                throw new IllegalStateException("Command must be set");
            }
            return new CommandReward(command, executeAsPlayer, delayTicks);
        }
    }

    public static class CompositeBuilder {
        private final List<AbstractReward> rewards = new ArrayList<>();
        private boolean continueOnError = false;

        public CompositeBuilder add(@NotNull AbstractReward reward) {
            rewards.add(reward);
            return this;
        }

        public CompositeBuilder addAll(@NotNull List<AbstractReward> rewards) {
            this.rewards.addAll(rewards);
            return this;
        }

        public CompositeBuilder continueOnError(boolean continueOnError) {
            this.continueOnError = continueOnError;
            return this;
        }

        public CompositeReward build() {
            return new CompositeReward(rewards, continueOnError);
        }
    }

    public static class ChoiceBuilder {
        private final List<AbstractReward> choices = new ArrayList<>();
        private int minimumRequired = 1;
        private Integer maximumRequired = null;
        private boolean allowMultipleSelections = false;

        public ChoiceBuilder add(@NotNull AbstractReward choice) {
            choices.add(choice);
            return this;
        }

        public ChoiceBuilder addAll(@NotNull List<AbstractReward> choices) {
            this.choices.addAll(choices);
            return this;
        }

        public ChoiceBuilder minimumRequired(int min) {
            this.minimumRequired = min;
            return this;
        }

        public ChoiceBuilder maximumRequired(int max) {
            this.maximumRequired = max;
            return this;
        }

        public ChoiceBuilder allowMultipleSelections(boolean allow) {
            this.allowMultipleSelections = allow;
            return this;
        }

        public ChoiceBuilder singleChoice() {
            this.minimumRequired = 1;
            this.maximumRequired = 1;
            return this;
        }

        public ChoiceReward build() {
            return new ChoiceReward(choices, minimumRequired, maximumRequired, allowMultipleSelections);
        }
    }

    public static class PermissionBuilder {
        private final List<String> permissions = new ArrayList<>();
        private Long durationSeconds = null;
        private boolean temporary = false;

        public PermissionBuilder permission(@NotNull String permission) {
            permissions.add(permission);
            return this;
        }

        public PermissionBuilder permissions(@NotNull List<String> permissions) {
            this.permissions.addAll(permissions);
            return this;
        }

        public PermissionBuilder temporary(long seconds) {
            this.durationSeconds = seconds;
            this.temporary = true;
            return this;
        }

        public PermissionBuilder permanent() {
            this.durationSeconds = null;
            this.temporary = false;
            return this;
        }

        public PermissionReward build() {
            return new PermissionReward(permissions, durationSeconds, temporary);
        }
    }

    public static class SoundBuilder {
        private Sound sound;
        private float volume = 1.0f;
        private float pitch = 1.0f;

        public SoundBuilder sound(@NotNull Sound sound) {
            this.sound = sound;
            return this;
        }

        public SoundBuilder volume(float volume) {
            this.volume = volume;
            return this;
        }

        public SoundBuilder pitch(float pitch) {
            this.pitch = pitch;
            return this;
        }

        public SoundReward build() {
            if (sound == null) {
                throw new IllegalStateException("Sound must be set");
            }
            return new SoundReward(sound, volume, pitch);
        }
    }

    public static class ParticleBuilder {
        private Particle particle;
        private int count = 10;
        private double offsetX = 0.5;
        private double offsetY = 0.5;
        private double offsetZ = 0.5;
        private double extra = 0.0;

        public ParticleBuilder particle(@NotNull Particle particle) {
            this.particle = particle;
            return this;
        }

        public ParticleBuilder count(int count) {
            this.count = count;
            return this;
        }

        public ParticleBuilder offset(double x, double y, double z) {
            this.offsetX = x;
            this.offsetY = y;
            this.offsetZ = z;
            return this;
        }

        public ParticleBuilder extra(double extra) {
            this.extra = extra;
            return this;
        }

        public ParticleReward build() {
            if (particle == null) {
                throw new IllegalStateException("Particle must be set");
            }
            return new ParticleReward(particle, count, offsetX, offsetY, offsetZ, extra);
        }
    }

    public static class TeleportBuilder {
        private String worldName;
        private double x;
        private double y;
        private double z;
        private float yaw = 0.0f;
        private float pitch = 0.0f;

        public TeleportBuilder world(@NotNull String worldName) {
            this.worldName = worldName;
            return this;
        }

        public TeleportBuilder location(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        public TeleportBuilder rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
            return this;
        }

        public TeleportReward build() {
            if (worldName == null) {
                throw new IllegalStateException("World name must be set");
            }
            return new TeleportReward(worldName, x, y, z, yaw, pitch);
        }
    }

    public static class VanishingChestBuilder {
        private final List<ItemStack> items = new ArrayList<>();
        private long durationTicks = 6000; // 5 minutes default
        private boolean dropItemsOnVanish = true;

        public VanishingChestBuilder addItem(@NotNull ItemStack item) {
            items.add(item);
            return this;
        }

        public VanishingChestBuilder items(@NotNull List<ItemStack> items) {
            this.items.addAll(items);
            return this;
        }

        public VanishingChestBuilder duration(long ticks) {
            this.durationTicks = ticks;
            return this;
        }

        public VanishingChestBuilder durationSeconds(int seconds) {
            this.durationTicks = seconds * 20L;
            return this;
        }

        public VanishingChestBuilder dropItemsOnVanish(boolean drop) {
            this.dropItemsOnVanish = drop;
            return this;
        }

        public VanishingChestReward build() {
            if (items.isEmpty()) {
                throw new IllegalStateException("Vanishing chest must have at least one item");
            }
            return new VanishingChestReward(items, durationTicks, dropItemsOnVanish);
        }
    }
}
