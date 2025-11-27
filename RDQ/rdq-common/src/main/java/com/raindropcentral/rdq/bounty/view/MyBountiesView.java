package com.raindropcentral.rdq.bounty.view;

import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.bounty.Bounty;
import com.raindropcentral.rdq.bounty.BountyStatus;
import com.raindropcentral.rplatform.utility.map.Maps;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.api.Placeholder;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MyBountiesView extends BaseView {

    private final State<RDQCore> rdqCore = initialState("rdqCore");

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
            "XpppppppX",
            "XpppppppX",
            "XtttttttX",
            "XtttttttX",
            "XXXXXXXXX"
        };
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        renderDecorations(render);
        renderLoadingIndicator(render, player);
        loadAndRenderMyBounties(render, player);
    }

    private void renderDecorations(@NotNull RenderContext render) {
        render.layoutSlot('X', UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(Component.empty())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build());
    }

    private void renderLoadingIndicator(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('p', UnifiedBuilderFactory
            .item(Material.HOPPER)
            .setName(this.i18n("loading.name", player).build().component())
            .setLore(this.i18n("loading.lore", player).build().splitLines())
            .build());

        render.layoutSlot('t', UnifiedBuilderFactory
            .item(Material.HOPPER)
            .setName(this.i18n("loading.name", player).build().component())
            .setLore(this.i18n("loading.lore", player).build().splitLines())
            .build());
    }

    private void loadAndRenderMyBounties(@NotNull RenderContext render, @NotNull Player player) {
        var rdq = this.rdqCore.get(render);
        if (rdq == null) {
            renderError(render, player);
            return;
        }

        var bountyService = rdq.getBountyService();
        var playerId = player.getUniqueId();

        bountyService.getBountiesPlacedBy(playerId).thenAccept(placedBounties -> {
            bountyService.getBountiesOnPlayer(playerId).thenAccept(targetedBounties -> {
                rdq.getPlatform().getScheduler().runSync(() -> {
                    renderPlacedBounties(render, player, placedBounties);
                    renderTargetedBounties(render, player, targetedBounties);
                });
            });
        }).exceptionally(throwable -> {
            rdq.getPlatform().getScheduler().runSync(() -> {
                this.i18n("load_error", player)
                    .withPrefix()
                    .with(Placeholder.of("error", throwable.getMessage()))
                    .send();
                renderError(render, player);
            });
            return null;
        });
    }

    private void renderPlacedBounties(@NotNull RenderContext render, @NotNull Player player, @NotNull List<Bounty> bounties) {
        render.slot(4, UnifiedBuilderFactory
            .item(Material.EMERALD)
            .setName(this.i18n("placed_header.name", player).build().component())
            .setLore(this.i18n("placed_header.lore", player)
                .with(Placeholder.of("count", bounties.size()))
                .build().splitLines())
            .build());

        if (bounties.isEmpty()) {
            render.slot(13, UnifiedBuilderFactory
                .item(Material.GRAY_STAINED_GLASS_PANE)
                .setName(this.i18n("no_placed.name", player).build().component())
                .setLore(this.i18n("no_placed.lore", player).build().splitLines())
                .build());
            return;
        }

        int slotIndex = 0;
        for (var bounty : bounties) {
            if (slotIndex >= 14) break;

            int row = 1 + (slotIndex / 7);
            int col = 1 + (slotIndex % 7);
            int actualSlot = (row * 9) + col;

            render.slot(actualSlot, createPlacedBountyItem(bounty, player))
                .onClick(ctx -> ctx.openForPlayer(BountyDetailView.class, Maps.merge(ctx.getInitialData())
                    .with(Map.of("bounty", Optional.of(bounty)))
                    .immutable()));

            slotIndex++;
        }
    }

    private void renderTargetedBounties(@NotNull RenderContext render, @NotNull Player player, @NotNull List<Bounty> bounties) {
        var totalBounty = bounties.stream()
            .filter(Bounty::isActive)
            .map(Bounty::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        render.slot(31, UnifiedBuilderFactory
            .item(Material.REDSTONE)
            .setName(this.i18n("targeted_header.name", player).build().component())
            .setLore(this.i18n("targeted_header.lore", player)
                .with(Placeholder.of("count", bounties.size()))
                .with(Placeholder.of("total", totalBounty.toPlainString()))
                .build().splitLines())
            .build());

        if (bounties.isEmpty()) {
            render.slot(40, UnifiedBuilderFactory
                .item(Material.GRAY_STAINED_GLASS_PANE)
                .setName(this.i18n("no_targeted.name", player).build().component())
                .setLore(this.i18n("no_targeted.lore", player).build().splitLines())
                .build());
            return;
        }

        int slotIndex = 0;
        for (var bounty : bounties) {
            if (slotIndex >= 14) break;

            int row = 3 + (slotIndex / 7);
            int col = 1 + (slotIndex % 7);
            int actualSlot = (row * 9) + col;

            render.slot(actualSlot, createTargetedBountyItem(bounty, player));
            slotIndex++;
        }
    }

    private org.bukkit.inventory.ItemStack createPlacedBountyItem(@NotNull Bounty bounty, @NotNull Player player) {
        var targetPlayer = Bukkit.getOfflinePlayer(bounty.targetId());
        var statusKey = getStatusKey(bounty);

        return UnifiedBuilderFactory.unifiedHead(targetPlayer)
            .setDisplayName(this.i18n("placed_entry.name", player)
                .with(Placeholder.of("target_name", bounty.target().name()))
                .build().component())
            .setLore(this.i18n("placed_entry.lore", player)
                .with(Placeholder.of("amount", bounty.amount().toPlainString()))
                .with(Placeholder.of("currency", bounty.currency()))
                .with(Placeholder.of("status", this.i18n(statusKey, player).build().asLegacyText()))
                .with(Placeholder.of("time_remaining", formatTimeRemaining(bounty)))
                .build().splitLines())
            .build();
    }

    private org.bukkit.inventory.ItemStack createTargetedBountyItem(@NotNull Bounty bounty, @NotNull Player player) {
        var placerPlayer = Bukkit.getOfflinePlayer(bounty.placerId());

        return UnifiedBuilderFactory.unifiedHead(placerPlayer)
            .setDisplayName(this.i18n("targeted_entry.name", player)
                .with(Placeholder.of("placer_name", bounty.placer().name()))
                .build().component())
            .setLore(this.i18n("targeted_entry.lore", player)
                .with(Placeholder.of("amount", bounty.amount().toPlainString()))
                .with(Placeholder.of("currency", bounty.currency()))
                .with(Placeholder.of("time_remaining", formatTimeRemaining(bounty)))
                .build().splitLines())
            .build();
    }

    private String getStatusKey(@NotNull Bounty bounty) {
        return switch (bounty.status()) {
            case ACTIVE -> bounty.isExpired() ? "status_expired" : "status_active";
            case CLAIMED -> "status_claimed";
            case EXPIRED -> "status_expired";
            case CANCELLED -> "status_cancelled";
        };
    }

    private String formatTimeRemaining(@NotNull Bounty bounty) {
        if (bounty.expiresAt() == null) {
            return "Never";
        }

        var duration = Duration.between(Instant.now(), bounty.expiresAt());
        if (duration.isNegative()) {
            return "Expired";
        }

        long hours = duration.toHours();
        if (hours > 24) {
            return (hours / 24) + "d " + (hours % 24) + "h";
        }
        return hours + "h " + (duration.toMinutes() % 60) + "m";
    }

    private void renderError(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('p', UnifiedBuilderFactory
            .item(Material.REDSTONE_BLOCK)
            .setName(this.i18n("error.name", player).build().component())
            .setLore(this.i18n("error.lore", player).build().splitLines())
            .build());

        render.layoutSlot('t', UnifiedBuilderFactory
            .item(Material.REDSTONE_BLOCK)
            .setName(this.i18n("error.name", player).build().component())
            .setLore(this.i18n("error.lore", player).build().splitLines())
            .build());
    }
}
