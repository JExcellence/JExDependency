package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.service.bounty.BountyService;
import com.raindropcentral.rdq.service.bounty.BountyServiceProvider;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * View for displaying a paginated overview of all bounties in the system.
 * <p>
 * This view shows a list of all active bounties with player heads and basic information.
 * Players can click on individual bounties to view detailed information.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public final class BountyOverviewView extends APaginatedView<RBounty> {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Creates a new bounty overview view that navigates back to the main bounty screen.
     */
    public BountyOverviewView() {
        super(BountyMainView.class);
    }

    /**
     * Gets the translation key used to resolve localized resources for this view.
     *
     * @return the translation key representing this view
     */
    @Override
    protected String getKey() {
        return "bounty.overview";
    }

    /**
     * Loads a paginated collection of bounties to display within this view.
     *
     * @param context the rendering context containing the viewing player
     * @return a future providing the list of bounties to render
     */
    @Override
    protected CompletableFuture<List<RBounty>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final BountyService service = BountyServiceProvider.getInstance();
        return service.getAllBounties(1, 128);
    }

    /**
     * Populates the item representing a single bounty entry within the overview list.
     *
     * @param context the render context for the current frame
     * @param builder the builder used to configure the item component
     * @param index   the zero-based index of the bounty in the current page
     * @param bounty  the bounty being rendered
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull RBounty bounty
    ) {
        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(bounty.getPlayer().getUniqueId());
        final Player player = context.getPlayer();
        final String commissionerName = Bukkit.getOfflinePlayer(bounty.getCommissioner()).getName();
        final String createdTime = bounty.getCreatedAt().toLocalTime().format(TIME_FORMATTER);

        builder
                .withItem(
                        UnifiedBuilderFactory
                                .head()
                                .setPlayerHead(offlinePlayer)
                                .setName(
                                        this.i18n("entry.name", player)
                                                .with("target_name", bounty.getPlayer().getPlayerName())
                                                .build()
                                                .component()
                                )
                                .setLore(
                                        this.i18n("entry.lore", player)
                                                .withAll(
                                                        Map.of(
                                                                "target_name", bounty.getPlayer().getPlayerName(),
                                                                "commissioner_name", commissionerName != null ? commissionerName : "Unknown",
                                                                "created_at", createdTime,
                                                                "index", index + 1,
                                                                "reward_count", bounty.getRewardItems().size()
                                                        )
                                                )
                                                .build()
                                                .splitLines()
                                )
                                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                                .build()
                )
                .onClick(clickContext -> {
                    clickContext.openForPlayer(
                            BountyPlayerInfoView.class,
                            Map.of(
                                    "bounty", bounty,
                                    "target", offlinePlayer
                            )
                    );
                });
    }

    /**
     * Handles any additional rendering concerns once the paginated layout has been drawn.
     *
     * @param render the paginated render context
     * @param player the player viewing the bounty overview
     */
    @Override
    protected void onPaginatedRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
    }
}