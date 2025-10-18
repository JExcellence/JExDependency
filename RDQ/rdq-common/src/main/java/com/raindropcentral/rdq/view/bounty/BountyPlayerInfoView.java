package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.service.BountyService;
import com.raindropcentral.rdq.service.BountyServiceProvider;
import com.raindropcentral.rplatform.utility.heads.view.Cancel;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import com.raindropcentral.rplatform.view.ConfirmationView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * View for displaying detailed information about a specific bounty and its target player.
 * <p>
 * This view allows users to:
 * <ul>
 * <li>See the target player's head and information</li>
 * <li>View the bounty's rewards</li>
 * <li>Navigate back to the main bounty menu</li>
 * <li>Delete the bounty (if the user is an operator), with confirmation</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public final class BountyPlayerInfoView extends BaseView {

    private final State<RBounty> bounty = initialState("bounty");
    private final State<OfflinePlayer> target = initialState("target");

    public BountyPlayerInfoView() {
        super(BountyMainView.class);
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
                "         ",
                "    p    ",
                "  i   r  ",
                "b       d"
        };
    }

    @Override
    protected int getSize() {
        return 4;
    }

    @Override
    protected Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext open) {
        return Map.of(
                "target_name", this.bounty.get(open).getPlayer().getPlayerName()
        );
    }

    @Override
    protected String getKey() {
        return "bounty.player_info";
    }

    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        this.renderTargetHead(render, player);
        this.renderRewardsButton(render, player);
        this.renderDeleteButton(render, player);
    }

    private void renderTargetHead(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        render.layoutSlot(
                'p',
                UnifiedBuilderFactory
                        .head()
                        .setPlayerHead(this.target.get(render))
                        .setName(
                                this.i18n("target.name", player)
                                        .with("player_name", this.target.get(render).getName())
                                        .build()
                                        .component()
                        )
                        .build()
        );
    }

    private void renderRewardsButton(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final int itemAmount = this.bounty.get(render)
                .getRewardItems()
                .stream()
                .mapToInt(RewardItem::getAmount)
                .sum();

        final String itemList = String.join(
                ", ",
                this.bounty.get(render)
                        .getRewardItems()
                        .stream()
                        .map(RewardItem::getItem)
                        .map(item -> "<lang:" + item.translationKey() + ">")
                        .toList()
        );

        render
                .layoutSlot(
                        'r',
                        UnifiedBuilderFactory
                                .item(Material.CHEST)
                                .setName(
                                        this.i18n("rewards.name", player)
                                                .build()
                                                .component()
                                )
                                .setLore(
                                        this.i18n("rewards.lore", player)
                                                .withAll(
                                                        Map.of(
                                                                "item_amount", itemAmount,
                                                                "item_list", itemList
                                                        )
                                                )
                                                .build()
                                                .splitLines()
                                )
                                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                                .build()
                )
                .onClick(clickContext ->
                        clickContext.openForPlayer(
                                BountyRewardView.class,
                                clickContext.getInitialData()
                        )
                );
    }

    private void renderDeleteButton(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        render
                .layoutSlot(
                        'd',
                        UnifiedBuilderFactory
                                .item(new Cancel().getHead(player))
                                .setName(
                                        this.i18n("delete.name", player)
                                                .build()
                                                .component()
                                )
                                .setLore(
                                        this.i18n("delete.lore", player)
                                                .build()
                                                .splitLines()
                                )
                                .build()
                )
                .displayIf(context -> context.getPlayer().isOp())
                .onClick(clickContext ->
                        clickContext.openForPlayer(
                                ConfirmationView.class,
                                Map.of(
                                        "titleKey", "bounty.confirm.delete.title",
                                        "messageKey", "bounty.confirm.delete.message",
                                        "initialData", clickContext.getInitialData()
                                )
                        )
                );
    }

    @Override
    public void onResume(
            final @NotNull Context origin,
            final @NotNull Context target
    ) {
        final Map<String, Object> initialData = (Map<String, Object>) target.getInitialData();

        if (initialData == null || initialData.get("confirmed") == null || !initialData.get("confirmed").equals(true)) {
            return;
        }

        final RBounty bounty = (RBounty) initialData.get("bounty");
        if (bounty == null) {
            return;
        }

        final BountyService service = BountyServiceProvider.getInstance();
        final Player player = origin.getPlayer();

        service.deleteBounty(bounty.getId()).thenAccept(success -> {
            if (success) {
                this.i18n("deleted_successfully", player)
                        .withPrefix()
                        .withAll(
                                Map.of(
                                        "bounty_id", bounty.getId(),
                                        "target_name", bounty.getPlayer().getPlayerName()
                                )
                        )
                        .send();

                target.openForPlayer(BountyMainView.class);
            } else {
                this.i18n("delete_failed", player).withPrefix().send();
            }
        });
    }
}