package de.jexcellence.quests.view;

import de.jexcellence.jexplatform.view.PaginatedView;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.Perk;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Paginated catalogue of every enabled perk, with per-entry
 * ownership state. Clicking an unlocked perk calls unlockAsync; the
 * service enforces requirement gating so ineligible clicks are a
 * silent no-op at the UI.
 */
public class PerkBrowserView extends PaginatedView<Perk> {

    private final State<JExQuests> plugin = initialState("plugin");
    private final State<Set<String>> owned = initialState("owned");

    public PerkBrowserView() {
        super();
    }

    @Override
    protected String translationKey() {
        return "perk_browser_ui";
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
    protected @NotNull CompletableFuture<List<Perk>> loadData(@NotNull Context ctx) {
        return this.plugin.get(ctx).perkService()
                .perks()
                .findEnabledAsync()
                .exceptionally(ex -> List.of());
    }

    @Override
    protected void renderItem(
            @NotNull Context ctx,
            @NotNull BukkitItemComponentBuilder builder,
            int index,
            @NotNull Perk entry
    ) {
        final var player = ctx.getPlayer();
        final Set<String> ownedSet = this.owned.get(ctx);
        final boolean isOwned = ownedSet != null && ownedSet.contains(entry.getIdentifier());

        final Material icon = isOwned ? Material.LIME_CANDLE : Material.GRAY_CANDLE;
        final String state = isOwned
                ? "<gradient:#86efac:#16a34a>owned</gradient>"
                : "<gradient:#fca5a5:#dc2626>locked</gradient>";

        builder.withItem(createItem(
                icon,
                i18n("entry.name", player)
                        .withPlaceholder("perk", entry.getDisplayName())
                        .build().component(),
                i18n("entry.lore", player)
                        .withPlaceholders(Map.of(
                                "identifier", entry.getIdentifier(),
                                "category", entry.getCategory(),
                                "kind", entry.getKind().name(),
                                "description", entry.getDescription() != null ? entry.getDescription() : "—",
                                "state", state,
                                "requirements", RequirementDescriber.describe(entry.getRequirementData()),
                                "rewards", RewardDescriber.describe(entry.getRewardData())
                        ))
                        .build().children()
        )).onClick(click -> {
            if (isOwned) {
                click.closeForPlayer();
                return;
            }
            final var jq = this.plugin.get(click);
            jq.perkService().unlockAsync(player.getUniqueId(), entry.getIdentifier())
                    .thenRun(() -> jq.perkRuntime().refreshAsync(player.getUniqueId()));
            click.closeForPlayer();
        });
    }

    /** Convenience — precomputes the owned identifier set for the {@code "owned"} state. */
    public static @NotNull CompletableFuture<Set<String>> loadOwnedIdentifiers(
            @NotNull JExQuests quests, @NotNull java.util.UUID playerUuid
    ) {
        return quests.perkService().ownedAsync(playerUuid)
                .thenApply(list -> list.stream()
                        .map(r -> r.getPerkIdentifier())
                        .collect(Collectors.toUnmodifiableSet()))
                .exceptionally(ex -> Set.of());
    }
}
