package de.jexcellence.quests.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Describes what a single quest task asks of a player. Parsed by the
 * runtime's progression listener from the task's {@code objective:}
 * YAML section; attached to the {@link QuestObjective}-shaped entity as a
 * JSON blob so the listener can match against live events
 * ({@code BlockBreakEvent}, {@code EntityDeathEvent}, etc.) without
 * coupling gameplay triggers to the entity model.
 *
 * <p>Discriminated on the {@code type} field — downstream plugins
 * implement their own kinds via {@link Custom}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = QuestObjective.BlockBreak.class,  name = "block-break"),
        @JsonSubTypes.Type(value = QuestObjective.BlockPlace.class,  name = "block-place"),
        @JsonSubTypes.Type(value = QuestObjective.EntityKill.class,  name = "entity-kill"),
        @JsonSubTypes.Type(value = QuestObjective.ItemCraft.class,   name = "item-craft"),
        @JsonSubTypes.Type(value = QuestObjective.ItemPickup.class,  name = "item-pickup"),
        @JsonSubTypes.Type(value = QuestObjective.PlayerJoin.class,  name = "player-join"),
        @JsonSubTypes.Type(value = QuestObjective.Custom.class,      name = "custom")
})
public sealed interface QuestObjective {

    /** Target quantity the player must reach for the task to complete. */
    long target();

    /** Mine {@code target} blocks of {@code material}. */
    record BlockBreak(@NotNull String material, long target) implements QuestObjective {
        public BlockBreak {
            if (target <= 0) throw new IllegalArgumentException("target must be positive");
        }
    }

    /** Place {@code target} blocks of {@code material}. */
    record BlockPlace(@NotNull String material, long target) implements QuestObjective {
        public BlockPlace {
            if (target <= 0) throw new IllegalArgumentException("target must be positive");
        }
    }

    /** Kill {@code target} entities of {@code entityType}. */
    record EntityKill(@NotNull String entityType, long target) implements QuestObjective {
        public EntityKill {
            if (target <= 0) throw new IllegalArgumentException("target must be positive");
        }
    }

    /** Craft {@code target} items of {@code material}. */
    record ItemCraft(@NotNull String material, long target) implements QuestObjective {
        public ItemCraft {
            if (target <= 0) throw new IllegalArgumentException("target must be positive");
        }
    }

    /** Pick up {@code target} items of {@code material}. */
    record ItemPickup(@NotNull String material, long target) implements QuestObjective {
        public ItemPickup {
            if (target <= 0) throw new IllegalArgumentException("target must be positive");
        }
    }

    /** Log in {@code target} distinct days. */
    record PlayerJoin(long target) implements QuestObjective {
        public PlayerJoin {
            if (target <= 0) throw new IllegalArgumentException("target must be positive");
        }
    }

    /** Plugin-defined objective — resolved by a handler keyed on {@code type}. */
    record Custom(@NotNull String type, long target, @NotNull Map<String, Object> data) implements QuestObjective {
        public Custom {
            data = Map.copyOf(data);
            if (target <= 0) throw new IllegalArgumentException("target must be positive");
        }
    }
}
