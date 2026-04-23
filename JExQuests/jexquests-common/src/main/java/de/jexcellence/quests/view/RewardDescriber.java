package de.jexcellence.quests.view;

import de.jexcellence.core.api.reward.Reward;
import de.jexcellence.quests.service.RewardRequirementCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a JSON-encoded {@link Reward} blob into a short MiniMessage
 * preview string — used by views to show "what you'll get" on quest
 * / rank / perk detail screens without re-implementing reward
 * introspection each time.
 *
 * <p>Output is a single line, comma-separated, truncated past four
 * entries with an ellipsis. Stateless; thread-safe.
 */
public final class RewardDescriber {

    private RewardDescriber() {
    }

    /** {@code " — "} placeholder for absent / undecodable rewards. */
    public static final String NONE = "—";

    /**
     * Decodes + formats the given JSON blob. Returns {@link #NONE}
     * for null/blank input or malformed JSON (never throws).
     */
    public static @NotNull String describe(@Nullable String json) {
        if (json == null || json.isBlank()) return NONE;
        try {
            final Reward reward = RewardRequirementCodec.decodeReward(json);
            if (reward == null) return NONE;
            return render(reward);
        } catch (final RuntimeException ex) {
            return NONE;
        }
    }

    /** Render a {@link Reward} as a short preview line. */
    public static @NotNull String render(@NotNull Reward reward) {
        final List<String> parts = new ArrayList<>();
        collect(reward, parts);
        if (parts.isEmpty()) return NONE;
        if (parts.size() > 4) {
            final List<String> truncated = new ArrayList<>(parts.subList(0, 4));
            truncated.add("<gray>+" + (parts.size() - 4) + " more</gray>");
            return String.join("<dark_gray>, </dark_gray>", truncated);
        }
        return String.join("<dark_gray>, </dark_gray>", parts);
    }

    /**
     * Flattens a composite reward tree into a list of atomic
     * (non-composite) leaves. Used by {@link RewardListView} so the
     * drill-down shows one icon per actual grant (xp / currency / item
     * / command / custom) rather than a single "composite" entry.
     */
    public static @NotNull List<Reward> flatten(@Nullable String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            final Reward reward = RewardRequirementCodec.decodeReward(json);
            if (reward == null) return List.of();
            final List<Reward> leaves = new ArrayList<>();
            flattenInto(reward, leaves);
            return leaves;
        } catch (final RuntimeException ex) {
            return List.of();
        }
    }

    private static void flattenInto(@NotNull Reward reward, @NotNull List<Reward> out) {
        if (reward instanceof Reward.Composite c) {
            for (final Reward child : c.children()) flattenInto(child, out);
        } else {
            out.add(reward);
        }
    }

    private static void collect(@NotNull Reward reward, @NotNull List<String> out) {
        switch (reward) {
            case Reward.Xp xp ->
                    out.add("<gradient:#86efac:#16a34a>+" + xp.amount() + " XP</gradient>");
            case Reward.Currency c ->
                    out.add("<gradient:#fde047:#f59e0b>+" + formatAmount(c.amount()) + " " + c.currency() + "</gradient>");
            case Reward.Item i ->
                    out.add("<gradient:#93c5fd:#2563eb>" + i.amount() + "x " + i.materialKey() + "</gradient>");
            case Reward.Command ignored ->
                    out.add("<gradient:#d8b4fe:#9333ea>script</gradient>");
            case Reward.Composite composite -> {
                for (final Reward child : composite.children()) collect(child, out);
            }
            case Reward.Custom custom ->
                    out.add("<gradient:#d8b4fe:#9333ea>" + custom.type() + "</gradient>");
        }
    }

    private static @NotNull String formatAmount(double amount) {
        if (amount == Math.floor(amount)) return Long.toString((long) amount);
        return String.format(java.util.Locale.ROOT, "%.2f", amount);
    }
}
