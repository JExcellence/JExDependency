package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Compact paginated view for displaying all active bounties.
 *
 * @author JExcellence
 * @version 1.1.0
 */
public class BountyOverviewView extends APaginatedView<Bounty> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private final State<RDQ> rdq = initialState("plugin");

    public BountyOverviewView() {
        super(BountyMainView.class);
    }

    @Override
    protected String getKey() {
        return "bounty_overview_ui";
    }

    @Override
    protected @NotNull String[] getLayout() {
        return new String[]{
            " OOOOOOO ",
            " OOOOOOO ",
            "   <p>   "
        };
    }

    @Override
    protected CompletableFuture<List<Bounty>> getAsyncPaginationSource(final @NotNull Context context) {
        return rdq.get(context).getBountyRepository().findAllByAttributesAsync(
            Map.of("active", true)
        );
    }

    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull Bounty bounty
    ) {
        var target = Bukkit.getOfflinePlayer(bounty.getTargetUniqueId());
        var commissioner = Bukkit.getOfflinePlayer(bounty.getCommissionerUniqueId());
        var player = context.getPlayer();

        var targetName = target.getName() != null ? target.getName() : "Unknown";
        var commissionerName = commissioner.getName() != null ? commissioner.getName() : "Unknown";
        var createdAt = bounty.getCreatedAt() != null ? bounty.getCreatedAt().format(DATE_FORMAT) : "Unknown";

        builder.withItem(UnifiedBuilderFactory
            .unifiedHead(target)
            .setDisplayName((net.kyori.adventure.text.Component) this.i18n("bounty.name", player)
                .withPlaceholder("target_name", targetName)
                .build().component())
            .setLore(this.i18n("bounty.lore", player)
                .withPlaceholders(Map.of(
                    "target_name", targetName,
                    "commissioner_name", commissionerName,
                    "created_at", createdAt
                ))
                .build().children())
            .build()
        ).onClick(ctx -> ctx.openForPlayer(
            BountyPlayerInfoView.class,
            Map.of(
                "plugin", this.rdq.get(ctx),
                "bounty", Optional.of(bounty),
                "target", Optional.of(target),
                "rewards", bounty.getRewards(),
                "insertedItems", new HashMap<>()
            )
        ));
    }

    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
        // Pagination handles everything
    }
}
