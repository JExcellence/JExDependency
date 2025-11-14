package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.service.bounty.BountyService;
import com.raindropcentral.rdq.service.bounty.BountyServiceProvider;
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

    /**
     * Creates a new instance that links back to the {@link BountyMainView} when navigating away.
     */
    public BountyPlayerInfoView() {
        super(BountyMainView.class);
    }

    /**
     * Builds the static layout used by the inventory framework to position components in the view.
     *
     * @return the array representing the rows of the inventory layout
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
                "GGGGGGGGG",
                "G   p   G",
                "G i   r G",
                "bGGGGGGGd"
        };
    }

    /**
     * Defines the amount of rows used for the inventory interface.
     *
     * @return the total number of rows rendered for the view
     */
    @Override
    protected int getSize() {
        return 4;
    }

    /**
     * Provides placeholder values that will be interpolated into the localized title string when
     * the view opens.
     *
     * @param open the context supplied by the inventory framework during the open lifecycle phase
     * @return a map containing placeholder keys paired with their resolved values
     */
    @Override
    protected Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext open) {
        return Map.of(
                "target_name", this.bounty.get(open).getPlayer().getPlayerName()
        );
    }

    /**
     * Supplies the translation key used for routing to the proper title and lore resources.
     *
     * @return the namespaced key identifying this view
     */
    @Override
    protected String getKey() {
        return "bounty.player_info";
    }

    /**
     * Renders the primary components when the inventory is first created.
     *
     * @param render the render context representing the current inventory frame
     * @param player the player viewing the bounty information
     */
    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        this.renderDecorations(render, player);
        this.renderTargetHead(render, player);
        this.renderInfoButton(render, player);
        this.renderRewardsButton(render, player);
        this.renderDeleteButton(render, player);
    }

    /**
     * Places the target player's head into the layout and names it according to localization rules.
     *
     * @param render the render context supplying state access
     * @param player the viewer so locale-aware translations can be resolved
     */
    private void renderTargetHead(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        render.layoutSlot(
                'p',
                UnifiedBuilderFactory
                        .head()
                        .
                        setPlayerHead(this.target.get(render))
                        .setName(
                                this.i18n("target.name", player)
                                        .with("player_name", this.target.get(render).getName())
                                        .build()
                                        .component()
                        )
                        .build()
        );
    }

    /**
     * Adds a button that summarizes the bounty rewards and links to the detailed reward view.
     *
     * @param render the render context supplying the current bounty state
     * @param player the player opening the interface for locale-sensitive messaging
     */
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

    /**
     * Renders an operator-only delete button that routes to a confirmation dialog before removal.
     *
     * @param render the render context rendering the delete action
     * @param player the player using the interface to determine locale and permissions
     */
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

    /**
     * Handles the resume lifecycle after returning from the confirmation view, deleting the bounty
     * when the action is approved and notifying the player of the outcome.
     *
     * @param origin the context that triggered the resume, providing player access
     * @param target the context containing the confirmation result data
     */
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
                        .item(Material.ORANGE_STAINED_GLASS_PANE)
                        .setName(this.i18n("decoration.name", player).build().component())
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build()
        );
    }

    /**
     * Renders an info button showing bounty details.
     *
     * @param render the render context used to populate slots
     * @param player the player viewing the menu
     */
    private void renderInfoButton(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final RBounty bounty = this.bounty.get(render);
        render.layoutSlot(
                'i',
                UnifiedBuilderFactory
                        .item(Material.BOOK)
                        .setName(this.i18n("info.name", player).build().component())
                        .setLore(
                                this.i18n("info.lore", player)
                                        .withAll(
                                                Map.of(
                                                        "bounty_id", bounty.getId(),
                                                        "target_name", bounty.getPlayer().getPlayerName(),
                                                        "creator_name", bounty.getCommissioner()
                                                )
                                        )
                                        .build()
                                        .splitLines()
                        )
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build()
        );
    }



}