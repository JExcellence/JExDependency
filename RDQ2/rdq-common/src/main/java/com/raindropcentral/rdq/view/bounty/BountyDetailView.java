package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.bounty.dto.Bounty;
import com.raindropcentral.rdq.bounty.type.BountyStatus;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.api.Placeholder;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * View for displaying detailed bounty information.
 * Shows complete bounty details including target, commissioner, rewards, and status.
 * 
 * Requirements: 7.1, 15.1, 15.2
 */
public class BountyDetailView extends BaseView {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    // Immutable state
    private final State<RDQ> rdq = initialState("plugin");
    private final State<Optional<Bounty>> bounty = initialState("bounty");

    public BountyDetailView() {
        super(BountyListView.class);
    }

    @Override
    protected String getKey() {
        return "bounty_detail_ui";
    }

    @Override
    protected int getSize() {
        return 6;
    }

    @Override
    protected String[] getLayout() {
        return new String[] {
                "XXXXXXXXX",
                "XtXXXXXcX",
                "XXXXXXXXX",
                "XrrrrrrrX",
                "XrrrrrrrX",
                "XXXXXXXXX"
        };
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        var bountyOpt = this.bounty.get(render);
        
        if (bountyOpt.isEmpty()) {
            // No bounty data provided
            render.layoutSlot('X', UnifiedBuilderFactory
                    .item(Material.BARRIER)
                    .setName(this.i18n("error.name", player).build().component())
                    .setLore(this.i18n("error.lore", player).build().splitLines())
                    .build());
            return;
        }
        
        var bounty = bountyOpt.get();
        
        // Render decorations
        renderDecorations(render);
        
        // Render target information
        renderTargetInfo(render, player, bounty);
        
        // Render commissioner information
        renderCommissionerInfo(render, player, bounty);
        
        // Render rewards
        renderRewards(render, player, bounty);
    }

    /**
     * Renders decorative glass panes.
     */
    private void renderDecorations(@NotNull RenderContext render) {
        render.layoutSlot('X', UnifiedBuilderFactory
                .item(Material.GRAY_STAINED_GLASS_PANE)
                .setName(Component.empty())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
    }

    /**
     * Renders target information.
     * Requirements: 7.1
     */
    private void renderTargetInfo(@NotNull RenderContext render, @NotNull Player player, @NotNull Bounty bounty) {
        var targetPlayer = org.bukkit.Bukkit.getOfflinePlayer(bounty.targetUuid());
        
        // Determine status text
        String statusText;
        Material statusMaterial;
        if (bounty.status() == BountyStatus.ACTIVE) {
            statusText = "Active";
            statusMaterial = Material.LIME_DYE;
        } else if (bounty.status() == BountyStatus.CLAIMED) {
            statusText = "Claimed";
            statusMaterial = Material.RED_DYE;
        } else {
            statusText = "Expired";
            statusMaterial = Material.GRAY_DYE;
        }
        
        render.layoutSlot('t', UnifiedBuilderFactory.unifiedHead(targetPlayer)
                .setDisplayName(this.i18n("target.name", player)
                        .with(Placeholder.of("target_name", bounty.targetName()))
                        .build().component())
                .setLore(this.i18n("target.lore", player)
                        .with(Placeholder.of("status", statusText))
                        .with(Placeholder.of("created_at", bounty.createdAt().format(TIME_FORMATTER)))
                        .with(Placeholder.of("expires_at", bounty.expiresAt() != null ? 
                                bounty.expiresAt().format(TIME_FORMATTER) : "Never"))
                        .build().splitLines())
                .build());
    }

    /**
     * Renders commissioner information.
     * Requirements: 7.1
     */
    private void renderCommissionerInfo(@NotNull RenderContext render, @NotNull Player player, @NotNull Bounty bounty) {
        var commissionerPlayer = org.bukkit.Bukkit.getOfflinePlayer(bounty.commissionerUuid());
        
        render.layoutSlot('c', UnifiedBuilderFactory.unifiedHead(commissionerPlayer)
                .setDisplayName(this.i18n("commissioner.name", player)
                        .with(Placeholder.of("commissioner_name", bounty.commissionerName()))
                        .build().component())
                .setLore(this.i18n("commissioner.lore", player)
                        .with(Placeholder.of("total_value", String.format("%.2f", bounty.totalEstimatedValue())))
                        .build().splitLines())
                .build());
    }

    /**
     * Renders reward items and currencies.
     * Requirements: 7.2, 7.3
     */
    private void renderRewards(@NotNull RenderContext render, @NotNull Player player, @NotNull Bounty bounty) {
        int slotIndex = 0;
        
        // Render reward items
        for (var rewardItem : bounty.rewardItems()) {
            if (slotIndex >= 14) break; // Max 14 reward slots (2 rows of 7)
            
            int row = 3 + (slotIndex / 7);
            int col = 1 + (slotIndex % 7);
            int actualSlot = (row * 9) + col;
            
            var item = rewardItem.item().clone();
            item.setAmount(rewardItem.amount());
            
            render.slot(actualSlot, UnifiedBuilderFactory.item(item)
                    .setLore(this.i18n("reward_item.lore", player)
                            .with(Placeholder.of("amount", rewardItem.amount()))
                            .build().splitLines())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build());
            
            slotIndex++;
        }
        
        // Render reward currencies
        for (var entry : bounty.rewardCurrencies().entrySet()) {
            if (slotIndex >= 14) break;
            
            int row = 3 + (slotIndex / 7);
            int col = 1 + (slotIndex % 7);
            int actualSlot = (row * 9) + col;
            
            render.slot(actualSlot, UnifiedBuilderFactory
                    .item(Material.GOLD_INGOT)
                    .setName(this.i18n("reward_currency.name", player)
                            .with(Placeholder.of("currency_name", entry.getKey()))
                            .build().component())
                    .setLore(this.i18n("reward_currency.lore", player)
                            .with(Placeholder.of("amount", String.format("%.2f", entry.getValue())))
                            .build().splitLines())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build());
            
            slotIndex++;
        }
        
        // If no rewards, show message
        if (slotIndex == 0) {
            render.slot(28, UnifiedBuilderFactory
                    .item(Material.BARRIER)
                    .setName(this.i18n("no_rewards.name", player).build().component())
                    .build());
        }
    }
}
