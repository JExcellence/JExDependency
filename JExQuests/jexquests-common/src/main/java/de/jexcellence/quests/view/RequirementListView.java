package de.jexcellence.quests.view;

import de.jexcellence.core.api.requirement.Requirement;
import de.jexcellence.jexplatform.view.PaginatedView;
import de.jexcellence.quests.JExQuests;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Drill-down requirement breakdown — one icon per atomic predicate
 * (permission, currency, statistic, quest-completed, rank, placeholder,
 * custom). Composite wrappers (AND/OR/XOR/NONE_OF) are flattened via
 * {@link RequirementDescriber#flatten} so the view lists every
 * condition as its own tile.
 *
 * <p>Opened from {@link RankDetailView} and {@link QuestDetailView}
 * when the player clicks the requirements pane. The raw JSON blob
 * is what we carry, not a decoded sealed-type instance — same
 * pattern as {@link RewardListView}.
 *
 * <p>Each entry's lore shows the specific gate shape (e.g.
 * "coins >= 2000", "complete zombie_slayer_ii at least 3 times") so
 * players can see what's still blocking their promotion / quest
 * acceptance without memorising the sealed interface.
 */
public class RequirementListView extends PaginatedView<Requirement> {

    private final State<JExQuests> plugin = initialState("plugin");
    private final State<String> requirementData = initialState("requirementData");
    private final State<String> titleContext = initialState("titleContext");

    public RequirementListView() {
        super();
    }

    @Override
    protected String translationKey() {
        return "requirement_list_ui";
    }

    @Override
    protected String[] layout() {
        return new String[]{
                "         ",
                "OOOOOOOOO",
                "OOOOOOOOO",
                "OOOOOOOOO",
                "   <p>   "
        };
    }

    @Override
    protected @NotNull Map<String, Object> titlePlaceholders(@NotNull me.devnatan.inventoryframework.context.OpenContext open) {
        final String ctx = this.titleContext.get(open);
        return Map.of("context", ctx != null ? ctx : "");
    }

    @Override
    protected @NotNull CompletableFuture<List<Requirement>> loadData(@NotNull Context ctx) {
        return CompletableFuture.completedFuture(RequirementDescriber.flatten(this.requirementData.get(ctx)));
    }

    @Override
    protected void renderItem(
            @NotNull Context ctx,
            @NotNull BukkitItemComponentBuilder builder,
            int index,
            @NotNull Requirement entry
    ) {
        final var player = ctx.getPlayer();
        final ReqView view = asView(entry);
        builder.withItem(createItem(
                view.icon(),
                i18n("entry." + view.kind() + ".name", player)
                        .withPlaceholders(view.placeholders())
                        .build().component(),
                i18n("entry." + view.kind() + ".lore", player)
                        .withPlaceholders(view.placeholders())
                        .build().children()
        ));
    }

    /**
     * Map every sealed requirement variant to an (icon, kind,
     * placeholders) triple. Comparator / logic enum names are included
     * verbatim so the lore reads like "balance GE 2000 coins" or
     * "complete zombie_slayer at least 3 times".
     */
    private static @NotNull ReqView asView(@NotNull Requirement requirement) {
        return switch (requirement) {
            case Requirement.Permission p -> new ReqView(
                    Material.REDSTONE_TORCH, "permission",
                    Map.of("node", p.node()));
            case Requirement.Currency c -> new ReqView(
                    Material.GOLD_INGOT, "currency",
                    Map.of(
                            "op", c.op().name(),
                            "amount", formatAmount(c.amount()),
                            "currency", c.currency()
                    ));
            case Requirement.Statistic s -> new ReqView(
                    Material.BOOK, "statistic",
                    Map.of(
                            "plugin", s.plugin(),
                            "identifier", s.identifier(),
                            "op", s.op().name(),
                            "value", formatAmount(s.value())
                    ));
            case Requirement.QuestCompleted q -> new ReqView(
                    Material.WRITABLE_BOOK, "quest_completed",
                    Map.of(
                            "quest", q.questIdentifier(),
                            "minimum", String.valueOf(q.minCompletions())
                    ));
            case Requirement.Rank r -> new ReqView(
                    Material.ENCHANTED_BOOK, "rank",
                    Map.of(
                            "tree", r.tree(),
                            "rank", r.minRankIdentifier()
                    ));
            case Requirement.Placeholder ph -> new ReqView(
                    Material.NAME_TAG, "placeholder",
                    Map.of(
                            "expansion", ph.expansion(),
                            "op", ph.op().name(),
                            "value", ph.value()
                    ));
            case Requirement.Composite ignored -> new ReqView(
                    Material.BEACON, "composite", Map.of());
            case Requirement.Custom custom -> new ReqView(
                    Material.PAPER, "custom",
                    Map.of(
                            "type", custom.type(),
                            "data", custom.data() != null ? custom.data().toString() : "—"
                    ));
        };
    }

    private static @NotNull String formatAmount(double amount) {
        if (amount == Math.floor(amount)) return Long.toString((long) amount);
        return String.format(java.util.Locale.ROOT, "%.2f", amount);
    }

    private record ReqView(@NotNull Material icon, @NotNull String kind, @NotNull Map<String, Object> placeholders) {
    }
}
