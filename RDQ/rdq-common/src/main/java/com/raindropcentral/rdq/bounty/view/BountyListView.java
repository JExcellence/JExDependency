package com.raindropcentral.rdq.bounty.view;

import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.bounty.Bounty;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BountyListView extends BaseView {

    private static final int BOUNTIES_PER_PAGE = 21;

    private final State<RDQCore> rdqCore = initialState("rdqCore");

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
            "XXXXXXXXX",
            "XXXpXnXXX"
        };
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        renderDecorations(render);
        renderLoadingIndicator(render, player);
        loadAndRenderBounties(render, player, 0);
    }

    private void renderDecorations(@NotNull RenderContext render) {
        render.layoutSlot('X', UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(Component.empty())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build());
    }

    private void renderLoadingIndicator(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('b', UnifiedBuilderFactory
            .item(Material.HOPPER)
            .setName(this.i18n("loading.name", player).build().component())
            .setLore(this.i18n("loading.lore", player).build().splitLines())
            .build());
    }

    private void loadAndRenderBounties(@NotNull RenderContext render, @NotNull Player player, int page) {
        var rdq = this.rdqCore.get(render);
        if (rdq == null) {
            renderError(render, player);
            return;
        }

        var bountyService = rdq.getBountyService();
        bountyService.getActiveBounties().thenAccept(bounties -> {
            rdq.getPlatform().getScheduler().runSync(() -> {
                renderBounties(render, player, bounties, page);
                renderPagination(render, player, page, bounties.size());
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

    private void renderBounties(@NotNull RenderContext render, @NotNull Player player, @NotNull List<Bounty> bounties, int page) {
        if (bounties.isEmpty()) {
            render.layoutSlot('b', UnifiedBuilderFactory
                .item(Material.BARRIER)
                .setName(this.i18n("empty.name", player).build().component())
                .setLore(this.i18n("empty.lore", player).build().splitLines())
                .build());
            return;
        }

        int startIndex = page * BOUNTIES_PER_PAGE;
        int endIndex = Math.min(startIndex + BOUNTIES_PER_PAGE, bounties.size());
        var pageBounties = bounties.subList(startIndex, endIndex);

        int slotIndex = 0;
        for (var bounty : pageBounties) {
            if (slotIndex >= 21) break;

            int row = 1 + (slotIndex / 7);
            int col = 1 + (slotIndex % 7);
            int actualSlot = (row * 9) + col;

            render.slot(actualSlot, createBountyItem(bounty, player))
                .onClick(ctx -> ctx.openForPlayer(BountyDetailView.class, Maps.merge(ctx.getInitialData())
                    .with(Map.of("bounty", Optional.of(bounty)))
                    .immutable()));

            slotIndex++;
        }
    }

    private org.bukkit.inventory.ItemStack createBountyItem(@NotNull Bounty bounty, @NotNull Player player) {
        var targetPlayer = Bukkit.getOfflinePlayer(bounty.targetId());
        var timeRemaining = formatTimeRemaining(bounty);

        return UnifiedBuilderFactory.unifiedHead(targetPlayer)
            .setDisplayName(this.i18n("bounty_entry.name", player)
                .with(Placeholder.of("target_name", bounty.target().name()))
                .build().component())
            .setLore(this.i18n("bounty_entry.lore", player)
                .with(Placeholder.of("amount", bounty.amount().toPlainString()))
                .with(Placeholder.of("currency", bounty.currency()))
                .with(Placeholder.of("placer_name", bounty.placer().name()))
                .with(Placeholder.of("time_remaining", timeRemaining))
                .build().splitLines())
            .build();
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

    private void renderPagination(@NotNull RenderContext render, @NotNull Player player, int currentPage, int totalBounties) {
        int totalPages = (int) Math.ceil((double) totalBounties / BOUNTIES_PER_PAGE);

        if (currentPage > 0) {
            render.layoutSlot('p', UnifiedBuilderFactory
                .item(Material.ARROW)
                .setName(this.i18n("previous_page.name", player).build().component())
                .setLore(this.i18n("previous_page.lore", player)
                    .with(Placeholder.of("page", currentPage))
                    .build().splitLines())
                .build())
                .onClick(ctx -> loadAndRenderBounties(render, player, currentPage - 1));
        } else {
            render.layoutSlot('p', UnifiedBuilderFactory
                .item(Material.GRAY_DYE)
                .setName(this.i18n("previous_page_disabled.name", player).build().component())
                .build());
        }

        if (currentPage < totalPages - 1) {
            render.layoutSlot('n', UnifiedBuilderFactory
                .item(Material.ARROW)
                .setName(this.i18n("next_page.name", player).build().component())
                .setLore(this.i18n("next_page.lore", player)
                    .with(Placeholder.of("page", currentPage + 2))
                    .build().splitLines())
                .build())
                .onClick(ctx -> loadAndRenderBounties(render, player, currentPage + 1));
        } else {
            render.layoutSlot('n', UnifiedBuilderFactory
                .item(Material.GRAY_DYE)
                .setName(this.i18n("next_page_disabled.name", player).build().component())
                .build());
        }
    }

    private void renderError(@NotNull RenderContext render, @NotNull Player player) {
        render.layoutSlot('b', UnifiedBuilderFactory
            .item(Material.REDSTONE_BLOCK)
            .setName(this.i18n("error.name", player).build().component())
            .setLore(this.i18n("error.lore", player).build().splitLines())
            .build());
    }
}
