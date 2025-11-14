package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.reward.AbstractReward;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Modern view for selecting and configuring rewards for bounty creation.
 * <p>
 * This view provides an intuitive interface for players to add multiple reward
 * types including items, currency, and experience to their bounty. Features
 * clean design patterns with reactive state management and validation.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 2.0.0
 */
public final class BountyRewardSelectionView extends BaseView {

    private static final int SLOT_ITEM_REWARDS = 11;
    private static final int SLOT_CURRENCY_REWARDS = 13;
    private static final int SLOT_EXPERIENCE_REWARDS = 15;
    private static final int SLOT_CONFIRM = 31;

    private final MutableState<List<AbstractReward>> rewards = initialState("rewards");

    /**
     * Creates a new reward selection view bound to the creation view as parent.
     */
    public BountyRewardSelectionView() {
        super(BountyCreationView.class);
    }

    @Override
    protected String getKey() {
        return "bounty.reward_selection";
    }

    @Override
    protected int getSize() {
        return 5;
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
                "GGGGGGGGG",
                "G i c e G",
                "G       G",
                "G   C   G",
                "GGGGGGGGG"
        };
    }

    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        this.renderDecorations(render, player);
        this.renderItemRewardButton(render, player);
        this.renderCurrencyRewardButton(render, player);
        this.renderExperienceRewardButton(render, player);
        this.renderConfirmButton(render, player);
    }

    /**
     * Renders decorative glass panes for visual enhancement.
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
     * Renders the button for adding item rewards.
     */
    private void renderItemRewardButton(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        render.layoutSlot(
                'i',
                UnifiedBuilderFactory
                        .item(Material.CHEST)
                        .setName(
                                this.i18n("add_item_reward.name", player)
                                        .build()
                                        .component()
                        )
                        .setLore(
                                this.i18n("add_item_reward.lore", player)
                                        .with("count", this.countRewardsByType(render, AbstractReward.Type.ITEM))
                                        .build()
                                        .splitLines()
                        )
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build()
        ).onClick(context -> {
            this.i18n("add_item_reward.instruction", player)
                    .withPrefix()
                    .send();
            // Implementation would open item selection or allow dropping items
        });
    }

    /**
     * Renders the button for adding currency rewards.
     */
    private void renderCurrencyRewardButton(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        render.layoutSlot(
                'c',
                UnifiedBuilderFactory
                        .item(Material.GOLD_INGOT)
                        .setName(
                                this.i18n("add_currency_reward.name", player)
                                        .build()
                                        .component()
                        )
                        .setLore(
                                this.i18n("add_currency_reward.lore", player)
                                        .with("count", this.countRewardsByType(render, AbstractReward.Type.CURRENCY))
                                        .build()
                                        .splitLines()
                        )
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .setGlowing(true)
                        .build()
        ).onClick(context -> {
            this.i18n("add_currency_reward.instruction", player)
                    .withPrefix()
                    .send();
            // Implementation would open amount input chat interface
        });
    }

    /**
     * Renders the button for adding experience rewards.
     */
    private void renderExperienceRewardButton(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        render.layoutSlot(
                'e',
                UnifiedBuilderFactory
                        .item(Material.EXPERIENCE_BOTTLE)
                        .setName(
                                this.i18n("add_experience_reward.name", player)
                                        .build()
                                        .component()
                        )
                        .setLore(
                                this.i18n("add_experience_reward.lore", player)
                                        .with("count", this.countRewardsByType(render, AbstractReward.Type.EXPERIENCE))
                                        .build()
                                        .splitLines()
                        )
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .build()
        ).onClick(context -> {
            this.i18n("add_experience_reward.instruction", player)
                    .withPrefix()
                    .send();
            // Implementation would open amount input chat interface
        });
    }

    /**
     * Renders the confirm button to finalize reward selection.
     */
    private void renderConfirmButton(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final boolean hasRewards = !this.rewards.get(render).isEmpty();

        render.layoutSlot(
                'C',
                UnifiedBuilderFactory
                        .item(hasRewards ? Material.EMERALD_BLOCK : Material.BARRIER)
                        .setName(
                                this.i18n("confirm.name", player)
                                        .build()
                                        .component()
                        )
                        .setLore(
                                this.i18n("confirm.lore", player)
                                        .with("reward_count", this.rewards.get(render).size())
                                        .build()
                                        .splitLines()
                        )
                        .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                        .setGlowing(hasRewards)
                        .build()
        ).onClick(context -> {
            if (this.rewards.get(context).isEmpty()) {
                this.i18n("confirm.no_rewards", player)
                        .withPrefix()
                        .send();
                return;
            }

            // Pass rewards back to creation view
            final Map<String, Object> data = (Map<String, Object>) context.getInitialData();
            data.put("selectedRewards", new ArrayList<>(this.rewards.get(context)));
            
            context.back(data);
        });
    }

    /**
     * Counts rewards of a specific type in the current selection.
     */
    private int countRewardsByType(
            final @NotNull RenderContext render,
            final @NotNull AbstractReward.Type type
    ) {
        return (int) this.rewards.get(render).stream()
                .filter(reward -> reward.getType() == type)
                .count();
    }
}
