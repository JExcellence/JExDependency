package de.jexcellence.quests.view;

import de.jexcellence.jexplatform.view.PaginatedView;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.database.entity.Bounty;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated list of active bounties. Click opens the target's profile
 * (out of scope here — placeholder no-op until a player profile view
 * lands).
 */
public class BountyOverviewView extends PaginatedView<Bounty> {

    private final State<JExQuests> plugin = initialState("plugin");

    public BountyOverviewView() {
        super();
    }

    @Override
    protected String translationKey() {
        return "bounty_overview_ui";
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
    protected @NotNull CompletableFuture<List<Bounty>> loadData(@NotNull Context ctx) {
        return this.plugin.get(ctx).bountyService().activeAsync();
    }

    @Override
    protected void renderItem(
            @NotNull Context ctx,
            @NotNull BukkitItemComponentBuilder builder,
            int index,
            @NotNull Bounty entry
    ) {
        final var player = ctx.getPlayer();
        final String targetName = nameOf(entry.getTargetUuid().toString());
        final String issuerName = nameOf(entry.getIssuerUuid().toString());

        builder.withItem(createItem(
                Material.SKELETON_SKULL,
                i18n("entry.name", player)
                        .withPlaceholder("target", targetName)
                        .build().component(),
                i18n("entry.lore", player)
                        .withPlaceholders(Map.of(
                                "target", targetName,
                                "issuer", issuerName,
                                "amount", String.valueOf(entry.getAmount()),
                                "currency", entry.getCurrency(),
                                "index", index + 1
                        ))
                        .build().children()
        ));
    }

    private static @NotNull String nameOf(@NotNull String uuid) {
        try {
            final var offline = Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid));
            return offline.getName() != null ? offline.getName() : uuid;
        } catch (final IllegalArgumentException ex) {
            return uuid;
        }
    }
}
