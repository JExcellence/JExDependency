package de.jexcellence.quests.view;

import de.jexcellence.core.api.reward.Reward;
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
 * Drill-down reward breakdown — one icon per atomic reward (xp,
 * currency, item, command, custom). Composite wrappers are flattened
 * via {@link RewardDescriber#flatten} so a complex nested composite
 * renders as individual clickable tiles rather than one opaque
 * "composite reward" entry.
 *
 * <p>Opened from {@link RankDetailView} and {@link QuestDetailView}
 * when the player clicks the rewards pane. Carries the raw JSON blob
 * (not a decoded object) through initial state so the caller doesn't
 * have to hold a reference to a sealed type instance.
 *
 * <p>Paginated because a single rank or quest can in principle have
 * an arbitrarily long reward chain; the view scales to whatever the
 * YAML declares.
 */
public class RewardListView extends PaginatedView<Reward> {

    private final State<JExQuests> plugin = initialState("plugin");
    /** Raw JSON blob; passed in by the opener so we don't have to share decoded records. */
    private final State<String> rewardData = initialState("rewardData");
    /** Optional title override — e.g. "Soldier Rewards" vs a generic "Rewards". */
    private final State<String> titleContext = initialState("titleContext");

    public RewardListView() {
        super();
    }

    @Override
    protected String translationKey() {
        return "reward_list_ui";
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
    protected @NotNull CompletableFuture<List<Reward>> loadData(@NotNull Context ctx) {
        return CompletableFuture.completedFuture(RewardDescriber.flatten(this.rewardData.get(ctx)));
    }

    @Override
    protected void renderItem(
            @NotNull Context ctx,
            @NotNull BukkitItemComponentBuilder builder,
            int index,
            @NotNull Reward entry
    ) {
        final var player = ctx.getPlayer();
        final RewardView view = asView(entry);
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

    /** Map every sealed reward variant to an (icon, kind, placeholder-map) triple. */
    private static @NotNull RewardView asView(@NotNull Reward reward) {
        return switch (reward) {
            case Reward.Xp xp -> new RewardView(
                    Material.EXPERIENCE_BOTTLE, "xp",
                    Map.of("amount", String.valueOf(xp.amount())));
            case Reward.Currency c -> new RewardView(
                    Material.GOLD_INGOT, "currency",
                    Map.of(
                            "amount", formatAmount(c.amount()),
                            "currency", c.currency()
                    ));
            case Reward.Item i -> new RewardView(
                    safeMaterial(i.materialKey()), "item",
                    Map.of(
                            "amount", String.valueOf(i.amount()),
                            "material", i.materialKey(),
                            "nbt", i.nbt() != null && !i.nbt().isBlank() && !i.nbt().equals("{}") ? i.nbt() : "—"
                    ));
            case Reward.Command cmd -> new RewardView(
                    Material.COMMAND_BLOCK, "command",
                    Map.of(
                            "command", cmd.command(),
                            "as", cmd.asConsole() ? "console" : "player"
                    ));
            case Reward.Composite ignored -> new RewardView(
                    Material.CHEST, "composite", Map.of());
            case Reward.Custom custom -> new RewardView(
                    Material.PAPER, "custom",
                    Map.of(
                            "type", custom.type(),
                            "data", custom.data() != null ? custom.data().toString() : "—"
                    ));
        };
    }

    /** Best-effort material lookup — falls back to PAPER for unknown keys. */
    private static @NotNull Material safeMaterial(@NotNull String key) {
        try {
            return Material.valueOf(key.toUpperCase(java.util.Locale.ROOT));
        } catch (final IllegalArgumentException ex) {
            return Material.PAPER;
        }
    }

    private static @NotNull String formatAmount(double amount) {
        if (amount == Math.floor(amount)) return Long.toString((long) amount);
        return String.format(java.util.Locale.ROOT, "%.2f", amount);
    }

    private record RewardView(@NotNull Material icon, @NotNull String kind, @NotNull Map<String, Object> placeholders) {
    }
}
