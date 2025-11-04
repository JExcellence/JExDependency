package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.service.bounty.BountyService;
import com.raindropcentral.rdq.service.bounty.BountyServiceProvider;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
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
 * allowing players to view all active bounties and create new bounties (if premium).
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public final class BountyMainView extends BaseView {

    private static final int SLOT_VIEW_BOUNTIES = 11;
    private static final int SLOT_CREATE_BOUNTY = 13;
    private static final int SLOT_UPGRADE_PREMIUM = 15;

    /**
     * Gets the localization key used to resolve translations for the bounty menu title.
     *
     * @return the static key {@code bounty.main} that represents the bounty overview frame
     */
    @Override
    protected String getKey() {
        return "bounty.main";
    }

    /**
     * Defines the number of rows displayed by the inventory-based bounty menu.
     *
     * @return the fixed size of {@code 3} rows for the view
     */
    @Override
    protected int getSize() {
        return 3;
    }

    /**
     * Renders all primary bounty interactions the first time the view is opened.
     * <p>
     * The render pipeline immediately paints static content and schedules any
     * asynchronous service lookups (such as the bounty count) before wiring click
     * handlers for the session. Premium-only actions are hidden behind their
     * upgrade prompt when the active {@link BountyService} instance indicates the
     * player lacks access.
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

        this.renderViewBountiesButton(render, player, service);
        this.renderCreateBountyButton(render, player, service);

        if (!service.isPremium()) {
            this.renderUpgradePremiumButton(render, player);
        }
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
        service.getTotalBountyCount().thenAccept(count -> {
            render.slot(
                    SLOT_VIEW_BOUNTIES,
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

        render.slot(
                SLOT_CREATE_BOUNTY,
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
     * Adds the premium upsell control used by free-edition players to learn how to
     * unlock bounty creation.
     *
     * @param render the inventory context used to register the slot
     * @param player the player opening the bounty menu
     */
    private void renderUpgradePremiumButton(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        render.slot(
                SLOT_UPGRADE_PREMIUM,
                UnifiedBuilderFactory
                        .item(Material.NETHER_STAR)
                        .setName(
                                this.i18n("upgrade_premium.name", player)
                                        .build()
                                        .component()
                        )
                        .setLore(
                                this.i18n("upgrade_premium.lore", player)
                                        .build()
                                        .splitLines()
                        )
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build()
        ).onClick(context -> {
            this.i18n("upgrade_premium.message", player)
                    .withPrefix()
                    .send();
            context.closeForPlayer();
        });
    }
}