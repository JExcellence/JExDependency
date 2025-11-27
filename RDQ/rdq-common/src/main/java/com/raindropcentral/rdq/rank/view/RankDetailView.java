package com.raindropcentral.rdq.rank.view;

import com.raindropcentral.rdq.api.RankService;
import com.raindropcentral.rdq.rank.Rank;
import com.raindropcentral.rdq.rank.RankRequirement;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RankDetailView extends View {

    private final RankService rankService;
    private final String rankId;
    private State<Optional<Rank>> rankState;

    public RankDetailView(@NotNull RankService rankService, @NotNull String rankId) {
        this.rankService = rankService;
        this.rankId = rankId;
    }

    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.cancelOnClick();
        config.size(4);
        config.title("Rank Details");
        config.layout(
            "XXXXXXXXX",
            "X   R   X",
            "XOOOOOOOX",
            "B   U   X"
        );

        rankState = lazyState(() -> rankService.getRank(rankId).join());
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {
        var rankOpt = rankState.get(render);
        if (rankOpt.isEmpty()) {
            render.closeForPlayer();
            return;
        }

        var rank = rankOpt.get();
        var playerId = render.getPlayer().getUniqueId();

        render.updateTitleForPlayer("<gold>" + rank.displayNameKey());

        render.layoutSlot('R', createRankInfoItem(rank));

        var requirementSlots = new int[]{19, 20, 21, 22, 23, 24, 25};
        var requirements = rank.requirements();
        for (int i = 0; i < Math.min(requirements.size(), requirementSlots.length); i++) {
            var req = requirements.get(i);
            render.slot(requirementSlots[i], createRequirementItem(req));
        }

        render.layoutSlot('B', createBackItem())
            .onClick(ctx -> ctx.closeForPlayer());

        render.layoutSlot('U', createUnlockItem(rank))
            .onClick(ctx -> {
                rankService.unlockRank(playerId, rank.id())
                    .thenAccept(success -> {
                        if (success) {
                            ctx.getPlayer().sendMessage("<green>Rank unlocked!");
                        } else {
                            ctx.getPlayer().sendMessage("<red>Cannot unlock rank. Check requirements.");
                        }
                        ctx.update();
                    });
            });
    }


    private ItemStack createRankInfoItem(@NotNull Rank rank) {
        var material = parseMaterial(rank.iconMaterial());
        var item = new ItemStack(material);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("<gold>" + rank.displayNameKey());
            meta.setLore(List.of(
                "<gray>" + rank.descriptionKey(),
                "",
                "<yellow>Tier: <white>" + rank.tier(),
                "<yellow>Weight: <white>" + rank.weight(),
                rank.hasLuckPermsGroup() ? "<yellow>Group: <white>" + rank.luckPermsGroup() : ""
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createRequirementItem(@NotNull RankRequirement requirement) {
        var item = new ItemStack(Material.PAPER);
        var meta = item.getItemMeta();
        if (meta != null) {
            var lore = new ArrayList<String>();
            var name = switch (requirement) {
                case RankRequirement.StatisticRequirement(var stat, var amount) -> {
                    lore.add("<gray>Statistic: <white>" + stat);
                    lore.add("<gray>Amount: <white>" + amount);
                    yield "<yellow>Statistic Requirement";
                }
                case RankRequirement.PermissionRequirement(var perm) -> {
                    lore.add("<gray>Permission: <white>" + perm);
                    yield "<yellow>Permission Requirement";
                }
                case RankRequirement.PreviousRankRequirement(var rankId) -> {
                    lore.add("<gray>Required Rank: <white>" + rankId);
                    yield "<yellow>Previous Rank Requirement";
                }
                case RankRequirement.CurrencyRequirement(var currency, var amount) -> {
                    lore.add("<gray>Currency: <white>" + currency);
                    lore.add("<gray>Amount: <white>" + amount);
                    yield "<yellow>Currency Requirement";
                }
                case RankRequirement.ItemRequirement(var mat, var amount) -> {
                    lore.add("<gray>Item: <white>" + mat);
                    lore.add("<gray>Amount: <white>" + amount);
                    yield "<yellow>Item Requirement";
                }
                case RankRequirement.LevelRequirement(var level) -> {
                    lore.add("<gray>Level: <white>" + level);
                    yield "<yellow>Level Requirement";
                }
                case RankRequirement.PlaytimeRequirement(var seconds) -> {
                    lore.add("<gray>Playtime: <white>" + formatSeconds(seconds));
                    yield "<yellow>Playtime Requirement";
                }
            };
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBackItem() {
        var item = new ItemStack(Material.ARROW);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("<red>Back");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createUnlockItem(@NotNull Rank rank) {
        var item = new ItemStack(Material.EMERALD);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("<green>Unlock Rank");
            meta.setLore(List.of(
                "<gray>Click to unlock this rank",
                "<gray>if you meet all requirements."
            ));
            item.setItemMeta(meta);
        }
        return item;
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
