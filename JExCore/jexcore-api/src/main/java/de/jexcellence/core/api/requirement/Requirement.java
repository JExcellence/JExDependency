package de.jexcellence.core.api.requirement;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Plugin-agnostic requirement description.
 *
 * <p>Sealed hierarchy listing built-in requirement types; {@link Custom}
 * is an escape hatch so third-party plugins plug their own kinds into
 * the same {@link RequirementEvaluator} pipeline.
 *
 * <p>Jackson-serialisable on the {@code type} discriminator. Composite
 * requirements aggregate children with a {@link Logic} operator.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Requirement.Permission.class,     name = "permission"),
        @JsonSubTypes.Type(value = Requirement.Currency.class,       name = "currency"),
        @JsonSubTypes.Type(value = Requirement.Statistic.class,      name = "statistic"),
        @JsonSubTypes.Type(value = Requirement.QuestCompleted.class, name = "quest-completed"),
        @JsonSubTypes.Type(value = Requirement.Rank.class,           name = "rank"),
        @JsonSubTypes.Type(value = Requirement.Placeholder.class,    name = "placeholder"),
        @JsonSubTypes.Type(value = Requirement.Composite.class,      name = "composite"),
        @JsonSubTypes.Type(value = Requirement.Custom.class,         name = "custom")
})
public sealed interface Requirement {

    /** Player must hold a Bukkit permission node. */
    record Permission(@NotNull String node) implements Requirement {
    }

    /** Player's balance for the named currency must satisfy {@code op} {@code amount}. */
    record Currency(
            @NotNull String currency,
            @NotNull Comparator op,
            double amount
    ) implements Requirement {
    }

    /** Stat lookup — plugin+identifier pair compared to {@code value}. */
    record Statistic(
            @NotNull String plugin,
            @NotNull String identifier,
            @NotNull Comparator op,
            double value
    ) implements Requirement {
    }

    /** Player must have completed the named quest at least {@code minCompletions} times. */
    record QuestCompleted(@NotNull String questIdentifier, int minCompletions) implements Requirement {
        public QuestCompleted {
            if (minCompletions < 1) throw new IllegalArgumentException("minCompletions must be >= 1");
        }
    }

    /** Player must be at or above {@code minRankIdentifier} within {@code tree}. */
    record Rank(@NotNull String tree, @NotNull String minRankIdentifier) implements Requirement {
    }

    /** PlaceholderAPI value {@code op} string {@code value}. Numeric comparators coerce both sides to double. */
    record Placeholder(
            @NotNull String expansion,
            @NotNull Comparator op,
            @NotNull String value
    ) implements Requirement {
    }

    /** Aggregate requirement — all children combined with {@link Logic}. */
    record Composite(
            @NotNull Logic op,
            @NotNull List<Requirement> children
    ) implements Requirement {
        public Composite {
            children = List.copyOf(children);
            if (children.isEmpty()) throw new IllegalArgumentException("composite requires at least one child");
        }
    }

    /** Plugin-defined. Evaluated by a handler registered for {@link #type()}. */
    record Custom(@NotNull String type, @NotNull Map<String, Object> data) implements Requirement {
        public Custom {
            data = Map.copyOf(data);
        }
    }

    /** Comparison operator for numeric/string requirements. */
    enum Comparator {
        LT, LE, EQ, NE, GE, GT;

        public boolean compare(double left, double right) {
            return switch (this) {
                case LT -> left < right;
                case LE -> left <= right;
                case EQ -> left == right;
                case NE -> left != right;
                case GE -> left >= right;
                case GT -> left > right;
            };
        }

        public boolean compare(@NotNull String left, @NotNull String right) {
            final int c = left.compareTo(right);
            return switch (this) {
                case LT -> c < 0;
                case LE -> c <= 0;
                case EQ -> c == 0;
                case NE -> c != 0;
                case GE -> c >= 0;
                case GT -> c > 0;
            };
        }
    }

    /** Logic operator for composite requirements. */
    enum Logic {
        AND, OR, XOR, NONE_OF
    }
}
