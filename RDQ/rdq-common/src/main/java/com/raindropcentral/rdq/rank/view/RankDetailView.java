package com.raindropcentral.rdq.rank.view;

import com.raindropcentral.rdq.api.RankService;
import com.raindropcentral.rdq.rank.Rank;
import com.raindropcentral.rdq.rank.RankRequirement;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.api.Placeholder;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public final class RankDetailView extends BaseView {

    private final RankService rankService;
    private final String rankId;
    private State<Optional<Rank>> rankState;

    public RankDetailView(@NotNull RankService rankService, @NotNull String rankId) {
        super(RankTreeView.class);
        this.rankService = rankService;
        this.rankId = rankId;
        this.rankState = lazyState(() -> rankService.getRank(rankId).join());
    }

    @Override
    protected String getKey() {
        return "rank_detail_ui";
    }

    @Override
    protected int getSize() {
        return 4;
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
                "XXXXXXXXX",
                "X   R   X",
                "XOOOOOOOX",
                "    U   X"
        };
    }

    @Override
    protected Map<String, Object> getTitlePlaceholders(@NotNull OpenContext open) {
        var rank = rankState.get(open);
        return Map.of(
                "rank_name", rank.map(Rank::displayNameKey).orElse("Unknown")
        );
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        var rankOpt = rankState.get(render);
        if (rankOpt.isEmpty()) {
            render.closeForPlayer();
            return;
        }

        var rank = rankOpt.get();
        renderDecorations(render);
        renderRankInfo(render, player, rank);
        renderRequirements(render, player, rank);
        renderUnlockButton(render, player, rank);
    }

    /**
     * Renders decorative glass panes.
     */
    private void renderDecorations(@NotNull RenderContext render) {
        render.layoutSlot('X', UnifiedBuilderFactory
                .item(Material.GRAY_STAINED_GLASS_PANE)
                .setName(net.kyori.adventure.text.Component.empty())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build());
    }

    /**
     * Renders rank information display.
     */
    private void renderRankInfo(@NotNull RenderContext render, @NotNull Player player, @NotNull Rank rank) {
        render.layoutSlot('R', createRankInfoItem(player, rank));
    }

    /**
     * Renders requirement items.
     */
    private void renderRequirements(@NotNull RenderContext render, @NotNull Player player, @NotNull Rank rank) {
        var requirementSlots = new int[]{19, 20, 21, 22, 23, 24, 25};
        var requirements = rank.requirements();
        for (int i = 0; i < Math.min(requirements.size(), requirementSlots.length); i++) {
            var req = requirements.get(i);
            render.slot(requirementSlots[i], createRequirementItem(player, req));
        }
    }

    /**
     * Renders unlock button with async unlock handler.
     */
    private void renderUnlockButton(@NotNull RenderContext render, @NotNull Player player, @NotNull Rank rank) {
        var playerId = player.getUniqueId();
        render.layoutSlot('U', createUnlockItem(player, rank))
                .onClick(ctx -> {
                    rankService.unlockRank(playerId, rank.id())
                            .thenAccept(success -> {
                                if (success) {
                                    this.i18n("unlock_success", player).withPrefix().send();
                                } else {
                                    this.i18n("unlock_failed", player).withPrefix().send();
                                }
                                ctx.update();
                            })
                            .exceptionally(throwable -> {
                                this.i18n("unlock_error", player)
                                        .withPrefix()
                                        .with(Placeholder.of("error", throwable.getMessage()))
                                        .send();
                                return null;
                            });
                });
    }

    private ItemStack createRankInfoItem(@NotNull Player player, @NotNull Rank rank) {
        var material = parseMaterial(rank.iconMaterial());
        var group = rank.hasLuckPermsGroup() ? rank.luckPermsGroup() : "";

        return UnifiedBuilderFactory
                .item(material)
                .setName(this.i18n("rank_info.name", player)
                        .with(Placeholder.of("rank_name", rank.displayNameKey()))
                        .build().component())
                .setLore(this.i18n("rank_info.lore", player)
                        .with(Placeholder.of("description", rank.descriptionKey()))
                        .with(Placeholder.of("tier", rank.tier()))
                        .with(Placeholder.of("weight", rank.weight()))
                        .with(Placeholder.of("group", group))
                        .build().splitLines())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private ItemStack createRequirementItem(@NotNull Player player, @NotNull RankRequirement requirement) {
        return switch (requirement) {
            case RankRequirement.StatisticRequirement(var stat, var amount) ->
                    UnifiedBuilderFactory
                            .item(Material.PAPER)
                            .setName(this.i18n("requirement.statistic.name", player).build().component())
                            .setLore(this.i18n("requirement.statistic.lore", player)
                                    .with(Placeholder.of("stat", stat))
                                    .with(Placeholder.of("amount", amount))
                                    .build().splitLines())
                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                            .build();
            case RankRequirement.PermissionRequirement(var perm) ->
                    UnifiedBuilderFactory
                            .item(Material.PAPER)
                            .setName(this.i18n("requirement.permission.name", player).build().component())
                            .setLore(this.i18n("requirement.permission.lore", player)
                                    .with(Placeholder.of("permission", perm))
                                    .build().splitLines())
                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                            .build();
            case RankRequirement.PreviousRankRequirement(var rankId) ->
                    UnifiedBuilderFactory
                            .item(Material.PAPER)
                            .setName(this.i18n("requirement.previous_rank.name", player).build().component())
                            .setLore(this.i18n("requirement.previous_rank.lore", player)
                                    .with(Placeholder.of("rank_id", rankId))
                                    .build().splitLines())
                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                            .build();
            case RankRequirement.CurrencyRequirement(var currency, var amount) ->
                    UnifiedBuilderFactory
                            .item(Material.PAPER)
                            .setName(this.i18n("requirement.currency.name", player).build().component())
                            .setLore(this.i18n("requirement.currency.lore", player)
                                    .with(Placeholder.of("currency", currency))
                                    .with(Placeholder.of("amount", amount))
                                    .build().splitLines())
                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                            .build();
            case RankRequirement.ItemRequirement(var mat, var amount) ->
                    UnifiedBuilderFactory
                            .item(Material.PAPER)
                            .setName(this.i18n("requirement.item.name", player).build().component())
                            .setLore(this.i18n("requirement.item.lore", player)
                                    .with(Placeholder.of("material", mat))
                                    .with(Placeholder.of("amount", amount))
                                    .build().splitLines())
                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                            .build();
            case RankRequirement.LevelRequirement(var level) ->
                    UnifiedBuilderFactory
                            .item(Material.PAPER)
                            .setName(this.i18n("requirement.level.name", player).build().component())
                            .setLore(this.i18n("requirement.level.lore", player)
                                    .with(Placeholder.of("level", level))
                                    .build().splitLines())
                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                            .build();
            case RankRequirement.PlaytimeRequirement(var seconds) ->
                    UnifiedBuilderFactory
                            .item(Material.PAPER)
                            .setName(this.i18n("requirement.playtime.name", player).build().component())
                            .setLore(this.i18n("requirement.playtime.lore", player)
                                    .with(Placeholder.of("playtime", formatSeconds(seconds)))
                                    .build().splitLines())
                            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                            .build();
        };
    }

    private ItemStack createUnlockItem(@NotNull Player player, @NotNull Rank rank) {
        return UnifiedBuilderFactory
                .item(Material.EMERALD)
                .setName(this.i18n("unlock_button.name", player).build().component())
                .setLore(this.i18n("unlock_button.lore", player).build().splitLines())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private String formatSeconds(long seconds) {
        var hours = seconds / 3600;
        var minutes = (seconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }

    private Material parseMaterial(@NotNull String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }
}
