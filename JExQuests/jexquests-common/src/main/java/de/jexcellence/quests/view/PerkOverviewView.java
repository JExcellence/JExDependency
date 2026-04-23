package de.jexcellence.quests.view;

import de.jexcellence.jexplatform.view.PaginatedView;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.PerkKind;
import de.jexcellence.quests.database.entity.PlayerPerk;
import de.jexcellence.quests.service.PerkService;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated view of the viewer's owned perks. Click toggles TOGGLE-kind
 * perks and activates ACTIVE-kind perks — the service layer enforces
 * cooldowns and kind validation, so clicks that don't apply are
 * silent no-ops here (chat commands surface those messages).
 *
 * <p>For ACTIVE-kind perks with an outstanding cooldown, the icon
 * switches to a red candle and the lore shows the seconds remaining.
 */
public class PerkOverviewView extends PaginatedView<PerkService.OwnedPerk> {

    private final State<JExQuests> plugin = initialState("plugin");

    public PerkOverviewView() {
        super();
    }

    @Override
    protected String translationKey() {
        return "perk_overview_ui";
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
    protected @NotNull CompletableFuture<List<PerkService.OwnedPerk>> loadData(@NotNull Context ctx) {
        final var player = ctx.getPlayer();
        return this.plugin.get(ctx).perkService().ownedWithDefinitionsAsync(player.getUniqueId());
    }

    @Override
    protected void renderItem(
            @NotNull Context ctx,
            @NotNull BukkitItemComponentBuilder builder,
            int index,
            @NotNull PerkService.OwnedPerk entry
    ) {
        final var player = ctx.getPlayer();
        final PlayerPerk ownership = entry.ownership();
        final PerkKind kind = entry.definition().getKind();
        final long cooldownRemaining = entry.cooldownRemainingSeconds();

        final Material icon = iconFor(kind, ownership.isEnabled(), cooldownRemaining);
        final String stateTag = stateTag(kind, ownership.isEnabled(), cooldownRemaining);

        builder.withItem(createItem(
                icon,
                i18n("entry.name", player)
                        .withPlaceholder("perk", entry.definition().getDisplayName())
                        .build().component(),
                i18n("entry.lore", player)
                        .withPlaceholders(Map.of(
                                "perk", entry.identifier(),
                                "kind", kind.name(),
                                "state", stateTag,
                                "count", String.valueOf(ownership.getActivationCount()),
                                "cooldown", cooldownRemaining > 0L ? (cooldownRemaining + "s") : "—"
                        ))
                        .build().children()
        )).onClick(click -> {
            final var quests = this.plugin.get(click);
            if (kind == PerkKind.TOGGLE) {
                quests.perkService().toggleAsync(player.getUniqueId(), entry.identifier())
                        .thenRun(() -> quests.perkRuntime().refreshAsync(player.getUniqueId()));
            } else if (kind == PerkKind.ACTIVE) {
                quests.perkService().activateAsync(player.getUniqueId(), entry.identifier());
            }
            click.closeForPlayer();
        });
    }

    private static @NotNull Material iconFor(@NotNull PerkKind kind, boolean enabled, long cooldownRemaining) {
        if (kind == PerkKind.ACTIVE && cooldownRemaining > 0L) return Material.RED_CANDLE;
        if (kind == PerkKind.PASSIVE) return Material.YELLOW_CANDLE;
        return enabled ? Material.LIME_CANDLE : Material.GRAY_CANDLE;
    }

    private static @NotNull String stateTag(@NotNull PerkKind kind, boolean enabled, long cooldownRemaining) {
        return switch (kind) {
            case PASSIVE -> "<gradient:#fde047:#f59e0b>always on</gradient>";
            case TOGGLE -> enabled
                    ? "<gradient:#86efac:#16a34a>enabled</gradient>"
                    : "<gradient:#fca5a5:#dc2626>disabled</gradient>";
            case ACTIVE -> cooldownRemaining > 0L
                    ? "<gradient:#fca5a5:#dc2626>cooldown</gradient>"
                    : "<gradient:#86efac:#16a34a>ready</gradient>";
        };
    }
}
