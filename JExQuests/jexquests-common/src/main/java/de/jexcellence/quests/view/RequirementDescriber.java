package de.jexcellence.quests.view;

import de.jexcellence.core.api.requirement.Requirement;
import de.jexcellence.quests.service.RewardRequirementCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Sibling of {@link RewardDescriber} for {@link Requirement} blobs.
 * Turns a JSON-encoded requirement into a short MiniMessage preview
 * line — "permission.X, balance ≥ 100 coins, +2 more".
 *
 * <p>Stateless; thread-safe; never throws out of the render path.
 * Used by detail views to surface gate conditions to players before
 * they try an action that will fail.
 */
public final class RequirementDescriber {

    public static final String NONE = "—";

    private RequirementDescriber() {
    }

    public static @NotNull String describe(@Nullable String json) {
        if (json == null || json.isBlank()) return NONE;
        try {
            final Requirement requirement = RewardRequirementCodec.decodeRequirement(json);
            if (requirement == null) return NONE;
            return render(requirement);
        } catch (final RuntimeException ex) {
            return NONE;
        }
    }

    public static @NotNull String render(@NotNull Requirement requirement) {
        final List<String> parts = new ArrayList<>();
        collect(requirement, parts);
        if (parts.isEmpty()) return NONE;
        if (parts.size() > 4) {
            final List<String> truncated = new ArrayList<>(parts.subList(0, 4));
            truncated.add("<gray>+" + (parts.size() - 4) + " more</gray>");
            return String.join("<dark_gray>, </dark_gray>", truncated);
        }
        return String.join("<dark_gray>, </dark_gray>", parts);
    }

    /**
     * Flattens a composite requirement tree into a list of atomic
     * (non-composite) leaves. Used by {@link RequirementListView} so
     * the drill-down shows one icon per predicate — permission /
     * currency / statistic / quest-completed / rank / placeholder /
     * custom — rather than a single "composite" entry.
     */
    public static @NotNull List<Requirement> flatten(@Nullable String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            final Requirement requirement = RewardRequirementCodec.decodeRequirement(json);
            if (requirement == null) return List.of();
            final List<Requirement> leaves = new ArrayList<>();
            flattenInto(requirement, leaves);
            return leaves;
        } catch (final RuntimeException ex) {
            return List.of();
        }
    }

    private static void flattenInto(@NotNull Requirement requirement, @NotNull List<Requirement> out) {
        if (requirement instanceof Requirement.Composite c) {
            for (final Requirement child : c.children()) flattenInto(child, out);
        } else {
            out.add(requirement);
        }
    }

    private static void collect(@NotNull Requirement requirement, @NotNull List<String> out) {
        switch (requirement) {
            case Requirement.Permission p ->
                    out.add("<gradient:#93c5fd:#2563eb>perm " + p.node() + "</gradient>");
            case Requirement.Currency c ->
                    out.add("<gradient:#fde047:#f59e0b>" + c.currency() + " "
                            + symbol(c.op()) + " " + formatAmount(c.amount()) + "</gradient>");
            case Requirement.Statistic s ->
                    out.add("<gradient:#86efac:#16a34a>" + s.plugin() + ":" + s.identifier() + " "
                            + symbol(s.op()) + " " + formatAmount(s.value()) + "</gradient>");
            case Requirement.QuestCompleted q -> {
                if (q.minCompletions() > 1) {
                    out.add("<gradient:#fde047:#f59e0b>" + q.questIdentifier() + " x" + q.minCompletions() + "</gradient>");
                } else {
                    out.add("<gradient:#fde047:#f59e0b>done " + q.questIdentifier() + "</gradient>");
                }
            }
            case Requirement.Rank r ->
                    out.add("<gradient:#d8b4fe:#9333ea>rank " + r.tree() + ":" + r.minRankIdentifier() + "</gradient>");
            case Requirement.Placeholder p ->
                    out.add("<gradient:#a5f3fc:#06b6d4>%" + p.expansion() + "% "
                            + symbol(p.op()) + " " + p.value() + "</gradient>");
            case Requirement.Composite c -> {
                for (final Requirement child : c.children()) collect(child, out);
            }
            case Requirement.Custom custom ->
                    out.add("<gradient:#d8b4fe:#9333ea>" + custom.type() + "</gradient>");
        }
    }

    private static @NotNull String symbol(@NotNull Requirement.Comparator op) {
        return switch (op) {
            case LT -> "&lt;";
            case LE -> "≤";
            case EQ -> "=";
            case NE -> "≠";
            case GE -> "≥";
            case GT -> "&gt;";
        };
    }

    private static @NotNull String formatAmount(double amount) {
        if (amount == Math.floor(amount)) return Long.toString((long) amount);
        return String.format(java.util.Locale.ROOT, "%.2f", amount);
    }
}
