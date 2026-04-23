package de.jexcellence.quests.view;

import de.jexcellence.jexplatform.view.PaginatedView;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.PlayerQuestProgress;
import de.jexcellence.quests.database.entity.Quest;
import de.jexcellence.quests.database.entity.QuestStatus;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated quest catalogue. Each tile shows a <b>status-aware</b>
 * icon + a category-themed icon when no status applies:
 *
 * <ul>
 *   <li><b>ACTIVE</b> → emerald block with a live progress line</li>
 *   <li><b>COMPLETED</b> → nether star, struck-through name</li>
 *   <li><b>ABANDONED</b> → barrier, dimmed name</li>
 *   <li><b>Available</b> (no row yet) → themed material per category
 *       (combat → sword, miner → pickaxe, builder → bricks, …) so the
 *       overview stops looking like "three random coloured concrete
 *       blocks" and instead reads as a categorised library</li>
 * </ul>
 *
 * <p>Sort order: category (alphabetical) → orderIndex / difficulty →
 * identifier. Keeps combat quests adjacent to each other, tutorial
 * quests first, etc.
 */
public class QuestOverviewView extends PaginatedView<QuestOverviewView.Entry> {

    private final State<JExQuests> plugin = initialState("plugin");
    /** Optional filter — "all" / "active" / "completed" / "abandoned". null ≡ "all". */
    private final State<String> filter = initialState("filter");

    public QuestOverviewView() {
        super(de.jexcellence.quests.view.QuestMainView.class);
    }

    @Override
    protected String translationKey() {
        return "quest_overview_ui";
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
    protected @NotNull CompletableFuture<List<Entry>> loadData(@NotNull Context ctx) {
        final JExQuests quests = this.plugin.get(ctx);
        final String filter = this.filter.get(ctx);
        final var viewerUuid = ctx.getPlayer().getUniqueId();

        // Join the catalogue with the viewer's progress rows so every
        // tile can render status + live progress without a second query
        // per entry.
        final var allQuests = quests.questService().quests().findEnabledAsync();
        final var progressRows = quests.questService().questProgress().findByPlayerAsync(viewerUuid);

        return allQuests.thenCombine(progressRows, (quests1, rows) -> {
            final Map<String, PlayerQuestProgress> progressById = new HashMap<>();
            for (final var row : rows) progressById.put(row.getQuestIdentifier(), row);

            final String mode = filter == null ? "all" : filter.toLowerCase(Locale.ROOT);
            return quests1.stream()
                    .map(q -> new Entry(q, progressById.get(q.getIdentifier())))
                    .filter(entry -> matchesFilter(entry, mode))
                    .sorted(Comparator
                            .comparing((Entry e) -> e.quest.getCategory(), Comparator.nullsLast(String::compareTo))
                            .thenComparing(e -> e.quest.getDifficulty().ordinal())
                            .thenComparing(e -> e.quest.getIdentifier()))
                    .toList();
        }).exceptionally(ex -> List.of());
    }

    private static boolean matchesFilter(@NotNull Entry entry, @NotNull String mode) {
        return switch (mode) {
            case "active" -> entry.progress != null && entry.progress.getStatus() == QuestStatus.ACTIVE;
            case "completed" -> entry.progress != null && entry.progress.getStatus() == QuestStatus.COMPLETED;
            case "abandoned" -> entry.progress != null && entry.progress.getStatus() == QuestStatus.ABANDONED;
            default -> true; // all / blank / unknown → pass-through
        };
    }

    @Override
    protected void renderItem(
            @NotNull Context ctx,
            @NotNull BukkitItemComponentBuilder builder,
            int index,
            @NotNull Entry entry
    ) {
        final var player = ctx.getPlayer();
        final Quest q = entry.quest();
        final PlayerQuestProgress p = entry.progress();
        final QuestStatus status = p != null ? p.getStatus() : null;

        builder.withItem(createItem(
                iconFor(q, status),
                i18n("entry.name", player)
                        .withPlaceholder("quest_display_name", q.getDisplayName())
                        .withPlaceholder("state", statusTag(status))
                        .build().component(),
                i18n("entry.lore", player)
                        .withPlaceholders(Map.of(
                                "quest_identifier", q.getIdentifier(),
                                "quest_category", categoryLabel(q.getCategory()),
                                "quest_difficulty", q.getDifficulty().name(),
                                "state", statusTag(status),
                                "completion_count", p != null ? String.valueOf(p.getCompletionCount()) : "0",
                                "action_hint", hintFor(status)
                        ))
                        .build().children()
        )).onClick(click -> click.openForPlayer(
                QuestDetailView.class,
                Map.of(
                        "plugin", this.plugin.get(click),
                        "quest", q
                )
        ));
    }

    // ── icon + tag + hint ──────────────────────────────────────────────────

    /**
     * Status wins over category — an active quest always shows emerald,
     * a completed quest always shows nether star, etc. Only untouched
     * quests fall through to the category-themed fallback.
     */
    private static @NotNull Material iconFor(@NotNull Quest quest, @Nullable QuestStatus status) {
        if (status != null) {
            return switch (status) {
                case ACTIVE -> Material.EMERALD_BLOCK;
                case COMPLETED -> Material.NETHER_STAR;
                case ABANDONED -> Material.BARRIER;
                case FAILED -> Material.REDSTONE_BLOCK;
                case EXPIRED -> Material.CLOCK;
                case AVAILABLE -> materialForCategory(quest.getCategory());
            };
        }
        return materialForCategory(quest.getCategory());
    }

    /**
     * Category → themed material. Matches the content-pack categories
     * (tutorial, combat, miner, farmer, builder, daily, challenge,
     * etc.). Unknown categories fall back to a written book so the
     * icon says "quest" instead of stone concrete.
     */
    private static @NotNull Material materialForCategory(@NotNull String category) {
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "tutorial" -> Material.WRITTEN_BOOK;
            case "combat" -> Material.IRON_SWORD;
            case "miner", "mining" -> Material.IRON_PICKAXE;
            case "farmer", "farming", "baker" -> Material.WHEAT;
            case "builder", "building" -> Material.BRICKS;
            case "hunter" -> Material.BOW;
            case "enchanter", "mage" -> Material.ENCHANTING_TABLE;
            case "explorer" -> Material.COMPASS;
            case "trader", "merchant" -> Material.EMERALD;
            case "challenge" -> Material.NETHERITE_SWORD;
            case "daily" -> Material.CHEST;
            default -> Material.WRITABLE_BOOK;
        };
    }

    private static @NotNull String statusTag(@Nullable QuestStatus status) {
        if (status == null) return "<gradient:#a5f3fc:#06b6d4>○ available</gradient>";
        return switch (status) {
            case ACTIVE -> "<gradient:#86efac:#16a34a>▸ active</gradient>";
            case COMPLETED -> "<gradient:#d8b4fe:#9333ea>✔ completed</gradient>";
            case ABANDONED -> "<gradient:#fca5a5:#dc2626>✘ abandoned</gradient>";
            case FAILED -> "<gradient:#fca5a5:#dc2626>✘ failed</gradient>";
            case EXPIRED -> "<gradient:#fca5a5:#dc2626>⏱ expired</gradient>";
            case AVAILABLE -> "<gradient:#a5f3fc:#06b6d4>○ available</gradient>";
        };
    }

    private static @NotNull String hintFor(@Nullable QuestStatus status) {
        if (status == null) return "<gradient:#a5f3fc:#06b6d4>▸ Click to accept</gradient>";
        return switch (status) {
            case ACTIVE -> "<gradient:#a5f3fc:#06b6d4>▸ Click to view progress</gradient>";
            case COMPLETED -> "<gradient:#a5f3fc:#06b6d4>▸ Click to review</gradient>";
            case ABANDONED, EXPIRED, FAILED -> "<gradient:#a5f3fc:#06b6d4>▸ Click to re-accept</gradient>";
            case AVAILABLE -> "<gradient:#a5f3fc:#06b6d4>▸ Click to accept</gradient>";
        };
    }

    /** Capitalise the first letter of the category for the lore line. */
    private static @NotNull String categoryLabel(@NotNull String category) {
        if (category.isEmpty()) return category;
        return Character.toUpperCase(category.charAt(0)) + category.substring(1).toLowerCase(Locale.ROOT);
    }

    /** Carries the tuple (quest, viewer's progress row) through pagination. */
    public record Entry(@NotNull Quest quest, @Nullable PlayerQuestProgress progress) {
    }
}
