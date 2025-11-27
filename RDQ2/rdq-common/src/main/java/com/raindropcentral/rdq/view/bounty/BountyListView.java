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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * View for displaying all active bounties with pagination support.
 * Allows players to browse bounties and view detailed information.
 * 
 * Requirements: 6.1, 15.1, 15.2
 */
public class BountyListView extends BaseView {

    private static final int BOUNTIES_PER_PAGE = 28; // 4 rows of 7 bounties
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, HH:mm");

    // Immutable state
    private final State<RDQ> rdq = initialState("plugin");

    public BountyListView() {
        super(BountyMainView.class);
    }

    @Override
    protected String getKey() {
        return "bounty_list_ui";
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
                "XXXpXnXXX"
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
        
        // Load bounties asynchronously
        var rdq = this.rdq.get(render);
        if (rdq == null) {
            renderError(render, player, "RDQ instance not found");
            return;
        }
        
        loadAndRenderBounties(render, player, 0);
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
     * Loads bounties asynchronously and renders them.
     * Requirements: 6.1, 6.2
     */
    private void loadAndRenderBounties(@NotNull RenderContext render, @NotNull Player player, int page) {
        var rdq = this.rdq.get(render);
        var bountyRepository = rdq.getBountyRepository();
        
        // Fetch bounties asynchronously
        bountyRepository.findAllActiveAsync(page, BOUNTIES_PER_PAGE).thenAccept(loadedBounties -> {
            // Get total count for pagination
            bountyRepository.countActiveAsync().thenAccept(totalCount -> {
                // Convert entities to DTOs
                var bountyDTOs = new ArrayList<Bounty>();
                for (var entity : loadedBounties) {
                    bountyDTOs.add(convertToDTO(entity));
                }
                
                int totalPages = (int) Math.ceil((double) totalCount / BOUNTIES_PER_PAGE);
                
                // Render on main thread
                rdq.getPlatform().getScheduler().runSync(() -> {
                    renderDecorations(render);
                    renderBounties(render, player, bountyDTOs);
                    renderPagination(render, player, page, totalPages);
                    render.update();
                });
            }).exceptionally(countError -> {
                rdq.getPlatform().getScheduler().runSync(() -> {
                    renderError(render, player, "Failed to count bounties: " + countError.getMessage());
                });
                return null;
            });
        }).exceptionally(throwable -> {
            rdq.getPlatform().getScheduler().runSync(() -> {
                renderError(render, player, "Failed to load bounties: " + throwable.getMessage());
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
     * Renders bounty entries as player heads.
     * Requirements: 6.2, 6.3
     */
    private void renderBounties(@NotNull RenderContext render, @NotNull Player player, @NotNull List<Bounty> bountyList) {
        if (bountyList.isEmpty()) {
            // Show empty message
            render.layoutSlot('b', UnifiedBuilderFactory
                    .item(Material.BARRIER)
                    .setName(this.i18n("empty.name", player).build().component())
                    .setLore(this.i18n("empty.lore", player).build().splitLines())
                    .build());
            return;
        }
        
        // Render each bounty in the 'b' slots (4 rows x 7 columns = 28 slots)
        int slotIndex = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                if (slotIndex < bountyList.size()) {
                    var bounty = bountyList.get(slotIndex);
                    int actualSlot = (row * 9) + col;
                    
                    render.slot(actualSlot, createBountyItem(bounty, player))
                            .onClick(ctx -> handleBountyClick(ctx, bounty));
                    
                    slotIndex++;
                }
            }
        }
    }

    /**
     * Creates an ItemStack representing a bounty entry.
     * Requirements: 6.3
     */
    private org.bukkit.inventory.ItemStack createBountyItem(@NotNull Bounty bounty, @NotNull Player player) {
        var targetPlayer = org.bukkit.Bukkit.getOfflinePlayer(bounty.targetUuid());
        
        // Calculate reward summary
        var itemCount = bounty.rewardItems().size();
        var currencyCount = bounty.rewardCurrencies().size();
        var totalValue = bounty.totalEstimatedValue();
        
        // Calculate time remaining
        String timeRemaining = "Never";
        if (bounty.expiresAt() != null) {
            var duration = Duration.between(LocalDateTime.now(), bounty.expiresAt());
            if (duration.isNegative()) {
                timeRemaining = "Expired";
            } else {
                long hours = duration.toHours();
                long minutes = duration.toMinutes() % 60;
                timeRemaining = String.format("%dh %dm", hours, minutes);
            }
        }
        
        return UnifiedBuilderFactory.unifiedHead(targetPlayer)
                .setDisplayName(this.i18n("bounty_entry.name", player)
                        .with(Placeholder.of("target_name", bounty.targetName()))
                        .build().component())
                .setLore(this.i18n("bounty_entry.lore", player)
                        .with(Placeholder.of("commissioner_name", bounty.commissionerName()))
                        .with(Placeholder.of("item_count", itemCount))
                        .with(Placeholder.of("currency_count", currencyCount))
                        .with(Placeholder.of("total_value", String.format("%.2f", totalValue)))
                        .with(Placeholder.of("time_remaining", timeRemaining))
                        .with(Placeholder.of("expires_at", bounty.expiresAt() != null ? 
                                bounty.expiresAt().format(TIME_FORMATTER) : "Never"))
                        .build().splitLines())
                .build();
    }

    /**
     * Renders pagination controls.
     * Requirements: 6.5, 6.6
     */
    private void renderPagination(@NotNull RenderContext render, @NotNull Player player, int currentPage, int totalPages) {
        // Previous page button
        if (currentPage > 0) {
            render.layoutSlot('p', UnifiedBuilderFactory
                    .item(Material.ARROW)
                    .setName(this.i18n("previous_page.name", player).build().component())
                    .setLore(this.i18n("previous_page.lore", player)
                            .with(Placeholder.of("page", currentPage))
                            .build().splitLines())
                    .build())
                    .onClick(ctx -> {
                        loadAndRenderBounties(render, player, currentPage - 1);
                    });
        } else {
            render.layoutSlot('p', UnifiedBuilderFactory
                    .item(Material.GRAY_DYE)
                    .setName(this.i18n("previous_page_disabled.name", player).build().component())
                    .build());
        }
        
        // Next page button
        if (currentPage < totalPages - 1) {
            render.layoutSlot('n', UnifiedBuilderFactory
                    .item(Material.ARROW)
                    .setName(this.i18n("next_page.name", player).build().component())
                    .setLore(this.i18n("next_page.lore", player)
                            .with(Placeholder.of("page", currentPage + 2))
                            .build().splitLines())
                    .build())
                    .onClick(ctx -> {
                        loadAndRenderBounties(render, player, currentPage + 1);
                    });
        } else {
            render.layoutSlot('n', UnifiedBuilderFactory
                    .item(Material.GRAY_DYE)
                    .setName(this.i18n("next_page_disabled.name", player).build().component())
                    .build());
        }
    }

    /**
     * Handles bounty entry click to open detail view.
     * Requirements: 6.4
     */
    private void handleBountyClick(
            @NotNull me.devnatan.inventoryframework.context.SlotClickContext ctx,
            @NotNull Bounty bounty
    ) {
        ctx.openForPlayer(BountyDetailView.class, Maps.merge(ctx.getInitialData())
                .with(Map.of("bounty", Optional.of(bounty)))
                .immutable());
    }
    
    /**
     * Renders an error state.
     */
    private void renderError(@NotNull RenderContext render, @NotNull Player player, @NotNull String errorMessage) {
        renderDecorations(render);
        
        render.layoutSlot('b', UnifiedBuilderFactory
                .item(Material.REDSTONE_BLOCK)
                .setName(this.i18n("error.name", player).build().component())
                .setLore(this.i18n("error.lore", player).build().splitLines())
                .build());
        
        render.update();
        
        // Log error for debugging
        java.util.logging.Logger.getLogger(getClass().getName())
                .warning("BountyListView error: " + errorMessage);
    }
}
