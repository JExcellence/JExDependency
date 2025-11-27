package com.raindropcentral.rdq.view.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.bounty.dto.Bounty;
import com.raindropcentral.rdq.bounty.dto.ClaimInfo;
import com.raindropcentral.rdq.bounty.type.BountyStatus;
import com.raindropcentral.rdq.bounty.type.ClaimMode;
import com.raindropcentral.rplatform.utility.map.Maps;
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

import java.util.*;

/**
 * View for displaying bounties created by the player.
 * Shows active, claimed, and expired bounties commissioned by the viewer.
 *
 * Requirements: 9.1, 15.1, 15.2
 * 
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class MyBountiesView extends BaseView {

    private final State<RDQ> rdq = initialState("plugin");

    public MyBountiesView() {
        super(BountyMainView.class);
    }

    @Override
    protected String getKey() {
        return "bounty_my_bounties_ui";
    }

    @Override
    protected int getSize() {
        return 6;
    }

    @Override
    protected String[] getLayout() {
        return new String[] {
                "XXXXXXXXX",
                "XbbbbbbbX",
                "XbbbbbbbX",
                "XbbbbbbbX",
                "XbbbbbbbX",
                "XXXXXXXXX"
        };
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        // Render decorations
        renderDecorations(render);
        
        // Show loading indicator
        render.layoutSlot('b', UnifiedBuilderFactory
                .item(Material.HOPPER)
                .setName(this.i18n("loading.name", player).build().component())
                .setLore(this.i18n("loading.lore", player).build().splitLines())
                .build());
        
        // Load and render player's bounties
        loadAndRenderMyBounties(render, player);
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
     * Loads and renders the player's bounties.
     * Requirements: 9.1
     */
    private void loadAndRenderMyBounties(@NotNull RenderContext render, @NotNull Player player) {
        var rdq = this.rdq.get(render);
        var bountyRepository = rdq.getBountyRepository();
        
        // Fetch bounties by commissioner
        bountyRepository.findByCommissionerAsync(player.getUniqueId()).thenAccept(commissionedBounties -> {
            // Convert to DTOs
            var bountyDTOs = new ArrayList<Bounty>();
            for (var entity : commissionedBounties) {
                bountyDTOs.add(convertToDTO(entity));
            }
            
            // Render on main thread
            rdq.getPlatform().getScheduler().runSync(() -> {
                renderMyBounties(render, player, bountyDTOs);
            });
        }).exceptionally(throwable -> {
            rdq.getPlatform().getScheduler().runSync(() -> {
                this.i18n("load_error", player)
                        .withPrefix()
                        .with(Placeholder.of("error", throwable.getMessage()))
                        .send();
            });
            return null;
        });
    }

    /**
     * Converts a bounty entity to a DTO.
     */
    private Bounty convertToDTO(@NotNull com.raindropcentral.rdq.database.entity.bounty.RBounty entity) {
        var targetPlayer = org.bukkit.Bukkit.getOfflinePlayer(entity.getTargetUniqueId());
        var commissionerPlayer = org.bukkit.Bukkit.getOfflinePlayer(entity.getCommissionerUniqueId());
        
        // Convert reward items
        var rewardItems = new HashSet<com.raindropcentral.rdq.bounty.dto.RewardItem>();
        for (var r : entity.getRewardItems()) {
            rewardItems.add(new com.raindropcentral.rdq.bounty.dto.RewardItem(
                    r.getItem(),
                    r.getAmount(),
                    0.0
            ));
        }
        
        // Get reward currencies
        var rewardCurrencies = new HashMap<>(entity.getRewardCurrencies());
        
        // Create claim info if bounty is claimed
        Optional<ClaimInfo> claimInfo = Optional.empty();
        if (entity.getClaimedBy().isPresent() && entity.getClaimedAt().isPresent()) {
            var claimerUuid = entity.getClaimedBy().get();
            var claimedAt = entity.getClaimedAt().get();
            var claimerPlayer = org.bukkit.Bukkit.getOfflinePlayer(claimerUuid);
            claimInfo = Optional.of(new ClaimInfo(
                    claimerUuid,
                    claimerPlayer.getName() != null ? claimerPlayer.getName() : "Unknown",
                    claimedAt,
                    ClaimMode.LAST_HIT
            ));
        }
        
        // Determine status
        BountyStatus status;
        if (entity.isActive()) {
            status = BountyStatus.ACTIVE;
        } else if (entity.getClaimedBy().isPresent()) {
            status = BountyStatus.CLAIMED;
        } else {
            status = BountyStatus.EXPIRED;
        }
        
        return new Bounty(
                entity.getId(),
                entity.getTargetUniqueId(),
                targetPlayer.getName() != null ? targetPlayer.getName() : "Unknown",
                entity.getCommissionerUniqueId(),
                commissionerPlayer.getName() != null ? commissionerPlayer.getName() : "Unknown",
                rewardItems,
                rewardCurrencies,
                entity.getTotalEstimatedValue(),
                entity.getCreatedAt(),
                entity.getExpiresAt().orElse(null),
                status,
                claimInfo
        );
    }

    /**
     * Renders the player's bounties.
     * Requirements: 9.2, 9.3, 9.4, 9.5
     */
    private void renderMyBounties(@NotNull RenderContext render, @NotNull Player player, @NotNull List<Bounty> bounties) {
        if (bounties.isEmpty()) {
            // Show empty message
            render.layoutSlot('b', UnifiedBuilderFactory
                    .item(Material.BARRIER)
                    .setName(this.i18n("empty.name", player).build().component())
                    .setLore(this.i18n("empty.lore", player).build().splitLines())
                    .build());
            return;
        }
        
        // Render each bounty
        int slotIndex = 0;
        for (var bounty : bounties) {
            if (slotIndex >= 28) break; // Max 28 slots (4 rows of 7)
            
            int row = 1 + (slotIndex / 7);
            int col = 1 + (slotIndex % 7);
            int actualSlot = (row * 9) + col;
            
            render.slot(actualSlot, createBountyItem(bounty, player))
                    .onClick(ctx -> handleBountyClick(ctx, bounty));
            
            slotIndex++;
        }
    }

    /**
     * Creates an ItemStack representing a bounty entry with status indicators.
     * Requirements: 9.2, 9.3, 9.4
     */
    private org.bukkit.inventory.ItemStack createBountyItem(@NotNull Bounty bounty, @NotNull Player player) {
        var targetPlayer = org.bukkit.Bukkit.getOfflinePlayer(bounty.targetUuid());
        
        // Determine status material and text
        Material statusMaterial;
        String statusKey;
        if (bounty.status() == BountyStatus.ACTIVE) {
            statusMaterial = Material.LIME_DYE;
            statusKey = "status_active";
        } else if (bounty.status() == BountyStatus.CLAIMED) {
            statusMaterial = Material.RED_DYE;
            statusKey = "status_claimed";
        } else {
            statusMaterial = Material.GRAY_DYE;
            statusKey = "status_expired";
        }
        
        // Build lore with status and claimer info
        var loreBuilder = this.i18n("my_bounty_entry.lore", player)
                .with(Placeholder.of("target_name", bounty.targetName()))
                .with(Placeholder.of("status", this.i18n(statusKey, player).build().asLegacyText()))
                .with(Placeholder.of("total_value", String.format("%.2f", bounty.totalEstimatedValue())));
        
        // Add claimer info if claimed
        if (bounty.claimInfo().isPresent()) {
            var claimInfo = bounty.claimInfo().get();
            loreBuilder.with(Placeholder.of("claimer_name", claimInfo.hunterName()));
        }
        
        return UnifiedBuilderFactory.unifiedHead(targetPlayer)
                .setDisplayName(this.i18n("my_bounty_entry.name", player)
                        .with(Placeholder.of("target_name", bounty.targetName()))
                        .build().component())
                .setLore(loreBuilder.build().splitLines())
                .build();
    }

    /**
     * Handles bounty entry click to open detail view.
     * Requirements: 9.6
     */
    private void handleBountyClick(
            @NotNull me.devnatan.inventoryframework.context.SlotClickContext ctx,
            @NotNull Bounty bounty
    ) {
        ctx.openForPlayer(BountyDetailView.class, Maps.merge(ctx.getInitialData())
                .with(Map.of("bounty", Optional.of(bounty)))
                .immutable());
    }
}
