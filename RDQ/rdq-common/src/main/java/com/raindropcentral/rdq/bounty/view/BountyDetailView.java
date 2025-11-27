package com.raindropcentral.rdq.bounty.view;

import com.raindropcentral.rdq.RDQCore;
import com.raindropcentral.rdq.bounty.Bounty;
import com.raindropcentral.rdq.bounty.BountyStatus;
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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class BountyDetailView extends BaseView {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private final State<RDQCore> rdqCore = initialState("rdqCore");
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
        return 4;
    }

    @Override
    protected String[] getLayout() {
        return new String[] {
            "XXXXXXXXX",
            "XtXXiXXcX",
            "XXXXaXXXX",
            "XXXXXXXXX"
        };
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        var bountyOpt = this.bounty.get(render);

        if (bountyOpt == null || bountyOpt.isEmpty()) {
            renderError(render, player);
            return;
        }

        var bountyData = bountyOpt.get();
        renderDecorations(render);
        renderTargetInfo(render, player, bountyData);
        renderBountyInfo(render, player, bountyData);
        renderCommissionerInfo(render, player, bountyData);
        renderActionButton(render, player, bountyData);
    }

    private void renderDecorations(@NotNull RenderContext render) {
        render.layoutSlot('X', UnifiedBuilderFactory
            .item(Material.GRAY_STAINED_GLASS_PANE)
            .setName(Component.empty())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build());
    }

    private void renderTargetInfo(@NotNull RenderContext render, @NotNull Player player, @NotNull Bounty bounty) {
        var targetPlayer = Bukkit.getOfflinePlayer(bounty.targetId());

        render.layoutSlot('t', UnifiedBuilderFactory.unifiedHead(targetPlayer)
            .setDisplayName(this.i18n("target.name", player)
                .with(Placeholder.of("target_name", bounty.target().name()))
                .build().component())
            .setLore(this.i18n("target.lore", player)
                .with(Placeholder.of("status", getStatusText(bounty)))
                .build().splitLines())
            .build());
    }

    private void renderBountyInfo(@NotNull RenderContext render, @NotNull Player player, @NotNull Bounty bounty) {
        var createdAt = TIME_FORMATTER.format(bounty.createdAt().atZone(ZoneId.systemDefault()));
        var expiresAt = bounty.expiresAt() != null 
            ? TIME_FORMATTER.format(bounty.expiresAt().atZone(ZoneId.systemDefault()))
            : "Never";

        render.layoutSlot('i', UnifiedBuilderFactory
            .item(Material.PAPER)
            .setName(this.i18n("info.name", player).build().component())
            .setLore(this.i18n("info.lore", player)
                .with(Placeholder.of("amount", bounty.amount().toPlainString()))
                .with(Placeholder.of("currency", bounty.currency()))
                .with(Placeholder.of("created_at", createdAt))
                .with(Placeholder.of("expires_at", expiresAt))
                .build().splitLines())
            .build());
    }

    private void renderCommissionerInfo(@NotNull RenderContext render, @NotNull Player player, @NotNull Bounty bounty) {
        var commissionerPlayer = Bukkit.getOfflinePlayer(bounty.placerId());

        render.layoutSlot('c', UnifiedBuilderFactory.unifiedHead(commissionerPlayer)
            .setDisplayName(this.i18n("commissioner.name", player)
                .with(Placeholder.of("commissioner_name", bounty.placer().name()))
                .build().component())
            .setLore(this.i18n("commissioner.lore", player).build().splitLines())
            .build());
    }

    private void renderActionButton(@NotNull RenderContext render, @NotNull Player player, @NotNull Bounty bounty) {
        if (!bounty.isActive()) {
            render.layoutSlot('a', UnifiedBuilderFactory
                .item(Material.GRAY_DYE)
                .setName(this.i18n("action_inactive.name", player).build().component())
                .setLore(this.i18n("action_inactive.lore", player)
                    .with(Placeholder.of("status", getStatusText(bounty)))
                    .build().splitLines())
                .build());
            return;
        }

        if (bounty.placerId().equals(player.getUniqueId())) {
            render.layoutSlot('a', UnifiedBuilderFactory
                .item(Material.RED_WOOL)
                .setName(this.i18n("cancel_button.name", player).build().component())
                .setLore(this.i18n("cancel_button.lore", player).build().splitLines())
                .build())
                .onClick(ctx -> handleCancelBounty(ctx.getPlayer(), bounty, render));
        } else {
            render.layoutSlot('a', UnifiedBuilderFactory
                .item(Material.LIME_DYE)
                .setName(this.i18n("active_indicator.name", player).build().component())
                .setLore(this.i18n("active_indicator.lore", player).build().splitLines())
                .build());
        }
    }

    private void handleCancelBounty(@NotNull Player player, @NotNull Bounty bounty, @NotNull RenderContext render) {
        var rdq = this.rdqCore.get(render);
        if (rdq == null) return;

        rdq.getBountyService().cancelBounty(player.getUniqueId(), bounty.id())
            .thenAccept(success -> {
                rdq.getPlatform().getScheduler().runSync(() -> {
                    if (success) {
                        this.i18n("cancel_success", player).withPrefix().send();
                    } else {
                        this.i18n("cancel_failed", player).withPrefix().send();
                    }
                    player.closeInventory();
                });
            });
    }

    private String getStatusText(@NotNull Bounty bounty) {
        return switch (bounty.status()) {
            case ACTIVE -> bounty.isExpired() ? "Expired" : "Active";
            case CLAIMED -> "Claimed";
            case EXPIRED -> "Expired";
            case CANCELLED -> "Cancelled";
        };
    }

    private void renderError(@NotNull RenderContext render, @NotNull Player player) {
        renderDecorations(render);
        render.slot(13, UnifiedBuilderFactory
            .item(Material.BARRIER)
            .setName(this.i18n("error.name", player).build().component())
            .setLore(this.i18n("error.lore", player).build().splitLines())
            .build());
    }
}
