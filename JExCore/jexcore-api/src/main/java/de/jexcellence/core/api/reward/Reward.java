package de.jexcellence.core.api.reward;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Plugin-agnostic reward description.
 *
 * <p>The sealed hierarchy lists built-in reward types; {@link Custom}
 * is an escape hatch for third-party plugins to persist and dispatch
 * their own reward kinds through the same {@link RewardExecutor}
 * pipeline.
 *
 * <p>Jackson-serialisable: the {@code type} field discriminates the
 * concrete record. {@code Xp} → {@code {"type":"xp","amount":50}},
 * {@code Composite} → {@code {"type":"composite","children":[...]}}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Reward.Xp.class,        name = "xp"),
        @JsonSubTypes.Type(value = Reward.Currency.class,  name = "currency"),
        @JsonSubTypes.Type(value = Reward.Item.class,      name = "item"),
        @JsonSubTypes.Type(value = Reward.Command.class,   name = "command"),
        @JsonSubTypes.Type(value = Reward.Composite.class, name = "composite"),
        @JsonSubTypes.Type(value = Reward.Custom.class,    name = "custom")
})
public sealed interface Reward {

    /** Player experience (vanilla XP points, not levels). */
    record Xp(int amount) implements Reward {
        public Xp {
            if (amount < 0) throw new IllegalArgumentException("negative xp amount");
        }
    }

    /** Currency payout — resolved via JExEconomy's {@code EconomyProvider} if installed. */
    record Currency(@NotNull String currency, double amount) implements Reward {
        public Currency {
            if (amount < 0) throw new IllegalArgumentException("negative currency amount");
        }
    }

    /**
     * Item grant. {@code nbt} is an optional serialised item blob
     * (Base64 / BukkitObjectOutputStream). When blank, a plain
     * {@code ItemStack(Material, amount)} is granted.
     */
    record Item(@NotNull String materialKey, int amount, @NotNull String nbt) implements Reward {
        public Item {
            if (amount <= 0) throw new IllegalArgumentException("item amount must be positive");
        }

        public static @NotNull Item plain(@NotNull String materialKey, int amount) {
            return new Item(materialKey, amount, "");
        }
    }

    /**
     * Execute a command. Placeholders {@code {player}} are substituted
     * by the {@link RewardContext#playerUuid()} owner name at dispatch
     * time.
     */
    record Command(@NotNull String command, boolean asConsole) implements Reward {
    }

    /** Chain of rewards — granted in order. Failure short-circuits. */
    record Composite(@NotNull List<Reward> children) implements Reward {
        public Composite {
            children = List.copyOf(children);
        }
    }

    /**
     * Plugin-defined reward. The {@code type} key maps to a
     * {@link RewardHandler} registered on {@link RewardExecutor}.
     */
    record Custom(@NotNull String type, @NotNull Map<String, Object> data) implements Reward {
        public Custom {
            data = Map.copyOf(data);
        }
    }
}
