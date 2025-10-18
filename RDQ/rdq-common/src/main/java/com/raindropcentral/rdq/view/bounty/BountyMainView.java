package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.service.BountyService;
import com.raindropcentral.rdq.service.BountyServiceProvider;
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

    @Override
    protected String getKey() {
        return "bounty.main";
    }

    @Override
    protected int getSize() {
        return 3;
    }

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