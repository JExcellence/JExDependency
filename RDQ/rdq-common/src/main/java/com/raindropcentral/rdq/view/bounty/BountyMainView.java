package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.service.bounty.BountyService;
import com.raindropcentral.rdq.service.bounty.BountyServiceProvider;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

/**
 * Main entry point for the Bounty UI in RaindropQuests.
 * <p>
 * This view serves as the primary navigation menu for bounty-related actions,
 * allowing players to view active bounties, create new bounties, and check
 * the bounty hunter leaderboard. Features modern 2025 design patterns with
 * clean messaging using MiniMessage and KyoriAdventure components.
 * </p>
 *
 * @author JExcellence
 * @version 3.0.0
 * @since 2.0.0
 */
public final class BountyMainView extends BaseView {

    private final State<RDQ> rdq = initialState("plugin");

    /**
     * Gets the localization key used to resolve translations for the bounty menu title.
     *
     * @return the static key {@code bounty.main} that represents the bounty overview frame
     */
    @Override
    protected String getKey() {
        return "bounty.main_ui";
    }

    /**
     * Defines the layout pattern for the bounty main menu with decorative borders.
     *
     * @return the layout pattern with glass pane borders and action slots
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
                "GGGGGGGGG",
                "G       G",
                "G v c l G",
                "G       G",
                "GGGGGGGGG"
        };
    }

    /**
     * Renders all primary bounty interactions the first time the view is opened.
     * <p>
     * This modernized implementation provides a clean three-button layout with
     * view bounties, create bounty, and leaderboard access. Asynchronous data
     * loading ensures smooth UX while service calls complete in the background.
     * </p>
     *
     * @param render the inventory framework context used to populate slots
     * @param player the player viewing the bounty menu
     */
    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final BountyService service = BountyServiceProvider.getInstance();

        // Render decorative glass panes
        this.renderDecorations(render, player);

        // Render primary action buttons
        this.renderViewBountiesButton(render, player, service);
        this.renderCreateBountyButton(render, player, service);
        this.renderLeaderboardButton(render, player, service);
    }

    /**
     * Renders decorative glass pane borders for visual enhancement.
     *
     * @param render the render context used to populate slots
     * @param player the player viewing the menu
     */
    private void renderDecorations(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        render.layoutSlot(
                'G',
                UnifiedBuilderFactory
                        .item(Material.GRAY_STAINED_GLASS_PANE)
                        .setName(this.i18n("decoration.name", player).build().component())
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build()
        );
    }

    /**
     * Populates the entry point for browsing active bounties and navigates to the
     * overview screen once selected.
     *
     * @param render  the inventory context used to register the slot
     * @param player  the player opening the bounty menu
     * @param service the bounty service supplying asynchronous statistics
     */
    private void renderViewBountiesButton(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @NotNull BountyService service
    ) {
        service.getTotalBountyCount()
                .thenAccept(count -> {
                    render.layoutSlot(
                            'v',
                            UnifiedBuilderFactory
                                    .item(Material.DIAMOND_SWORD)
                                    .setName(
                                            this.i18n("view_bounties.name", player)
                                                    .build()
                                                    .component()
                                    )
                                    .setLore(
                                            this.i18n("view_bounties.lore", player)
                                                    .with("count", count)
                                                    .build()
                                                    .splitLines()
                                    )
                                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                                    .build()
                    ).onClick(context ->
                            context.openForPlayer(BountyOverviewView.class)
                    );
                })
                .exceptionally(ex -> {
                    // Render button even if count fails
                    render.layoutSlot(
                            'v',
                            UnifiedBuilderFactory
                                    .item(Material.DIAMOND_SWORD)
                                    .setName(
                                            this.i18n("view_bounties.name", player)
                                                    .build()
                                                    .component()
                                    )
                                    .setLore(
                                            this.i18n("view_bounties.lore", player)
                                                    .with("count", 0)
                                                    .build()
                                                    .splitLines()
                                    )
                                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                                    .build()
                    ).onClick(context ->
                            context.openForPlayer(BountyOverviewView.class)
                    );
                    return null;
                });
    }

    /**
     * Renders the bounty creation control and transitions to the creation view if
     * the player is eligible to create new bounties.
     * <p>
     * When the service indicates the player cannot create bounties, the slot is
     * rendered in a disabled state and communicates the denial on click instead of
     * opening the next screen.
     * </p>
     *
     * @param render  the inventory context used to register the slot
     * @param player  the player opening the bounty menu
     * @param service the bounty service used to verify creation eligibility
     */
    private void renderCreateBountyButton(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @NotNull BountyService service
    ) {
        final boolean canCreate = service.canCreateBounty(player);
        final Material material = canCreate ? Material.EMERALD : Material.BARRIER;
        final String keyPrefix = canCreate ? "create_bounty" : "create_bounty_locked";

        render.layoutSlot(
                'c',
                UnifiedBuilderFactory
                        .item(material)
                        .setName(
                                this.i18n(keyPrefix + ".name", player)
                                        .build()
                                        .component()
                        )
                        .setLore(
                                this.i18n(keyPrefix + ".lore", player)
                                        .build()
                                        .splitLines()
                        )
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build()
        ).onClick(context -> {
            if (!canCreate) {
                this.i18n("create_bounty_locked.message", player)
                        .withPrefix()
                        .send();
                return;
            }

            context.openForPlayer(
                    BountyCreationView.class,
                    Map.of(
                            "plugin", rdq.get(context),
                            "target", Optional.empty(),
                            "rewardItems", new HashSet<>(),
                            "rewardCurrencies", new HashMap<>(),
                            "bounty", Optional.empty(),
                            "insertedItems", new HashMap<>()
                    )
            );
        });
    }

    /**
     * Renders the leaderboard access button for viewing top bounty hunters.
     *
     * @param render the inventory context used to register the slot
     * @param player the player opening the bounty menu
     * @param service the bounty service for fetching leaderboard data
     */
    private void renderLeaderboardButton(
            final @NotNull RenderContext render,
            final @NotNull Player player,
            final @NotNull BountyService service
    ) {
        service.getTopHunters(3, "bounties_claimed")
                .thenAccept(topHunters -> {
                    final String topHunterName = !topHunters.isEmpty()
                            ? topHunters.get(0).getPlayer().getPlayerName()
                            : this.i18n("leaderboard.none", player).build().asLegacyText();

                    render.layoutSlot(
                            'l',
                            UnifiedBuilderFactory
                                    .item(Material.GOLDEN_HELMET)
                                    .setName(
                                            this.i18n("leaderboard.name", player)
                                                    .build()
                                                    .component()
                                    )
                                    .setLore(
                                            this.i18n("leaderboard.lore", player)
                                                    .with("top_hunter", topHunterName)
                                                    .build()
                                                    .splitLines()
                                    )
                                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                                    .setGlowing(true)
                                    .build()
                    ).onClick(context ->
                            context.openForPlayer(BountyLeaderboardView.class)
                    );
                })
                .exceptionally(ex -> {
                    // Render button even if top hunters query fails
                    render.layoutSlot(
                            'l',
                            UnifiedBuilderFactory
                                    .item(Material.GOLDEN_HELMET)
                                    .setName(
                                            this.i18n("leaderboard.name", player)
                                                    .build()
                                                    .component()
                                    )
                                    .setLore(
                                            this.i18n("leaderboard.lore", player)
                                                    .with("top_hunter", this.i18n("leaderboard.none", player).build().asLegacyText())
                                                    .build()
                                                    .splitLines()
                                    )
                                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                                    .build()
                    ).onClick(context ->
                            context.openForPlayer(BountyLeaderboardView.class)
                    );
                    return null;
                });
    }
}