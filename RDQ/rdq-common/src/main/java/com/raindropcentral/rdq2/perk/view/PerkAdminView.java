/*
package com.raindropcentral.rdq2.perk.view;

import com.raindropcentral.rdq2.RDQ;
import com.raindropcentral.rdq2.api.PerkService;
import com.raindropcentral.rdq2.perk.Perk;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

*/
/**
 * Admin view for managing perks and granting/revoking from players.
 *
 * @author JExcellence
 * @since 1.0.0
 *//*

public final class PerkAdminView extends APaginatedView<Perk> {

    private static final Logger LOGGER = Logger.getLogger(PerkAdminView.class.getName());

    private final State<RDQ> core = initialState("core");
    private final MutableState<UUID> targetPlayer = mutableState(null);

    public PerkAdminView() {
        super(PerkMainView.class);
    }

    @Override
    protected String getKey() {
        return "perk_admin_ui";
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        final UUID target = this.targetPlayer.get(openContext);
        final String targetName = target != null ? getPlayerName(target) : "None";
        return Map.of("target", targetName);
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "T   R    ",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "OOOOOOOOO",
            "         ",
            "b  <p>   "
        };
    }

    @Override
    protected CompletableFuture<List<Perk>> getAsyncPaginationSource(final @NotNull Context context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final RDQ RDQ = this.core.get(context);
                final com.raindropcentral.rdq2.database.repository.RPerkRepository perkRepository = RDQ.getPerkRepository();

                if (perkRepository == null) {
                    return List.of();
                }

                // TODO: Implement findEnabled() method in RPerkRepository
                return List.of();

            } catch (final Exception e) {
                LOGGER.log(Level.SEVERE, "Error loading perks for admin view", e);
                return List.of();
            }
        });
    }

    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull Perk perk
    ) {
        final Player player = context.getPlayer();
        final Material material = parseMaterial(perk.iconMaterial());
        final UUID target = this.targetPlayer.get(context);

        builder
            .withItem(
                UnifiedBuilderFactory
                    .item(material)
                    .setName(this.i18n("perk_entry.name", player)
                        .with("perk_id", perk.id())
                        .build().component())
                    .setLore(this.i18n("perk_entry.lore", player)
                        .withAll(Map.of(
                            "category", perk.category() != null ? perk.category() : "none",
                            "enabled", perk.enabled() ? "Yes" : "No",
                            "has_target", target != null ? "Yes" : "No"
                        ))
                        .build().splitLines())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .setGlowing(perk.enabled())
                    .build()
            )
            .onClick(clickContext -> handlePerkClick(context, player, perk));
    }

    @Override
    protected void onPaginatedRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        renderTargetSelector(render, player);
        renderReloadButton(render, player);
    }

    private void renderTargetSelector(RenderContext render, Player player) {
        final UUID target = this.targetPlayer.get(render);
        final String targetName = target != null ? getPlayerName(target) : "None";

        render.layoutSlot('T', UnifiedBuilderFactory
            .item(Material.PLAYER_HEAD)
            .setName(this.i18n("target.name", player)
                .with("target", targetName)
                .build().component())
            .setLore(this.i18n("target.lore", player).build().splitLines())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            // Open player selector or cycle through online players
            final List<Player> online = List.copyOf(Bukkit.getOnlinePlayers());
            if (online.isEmpty()) return;

            final UUID current = this.targetPlayer.get(render);
            int currentIndex = -1;
            for (int i = 0; i < online.size(); i++) {
                if (online.get(i).getUniqueId().equals(current)) {
                    currentIndex = i;
                    break;
                }
            }
            final int nextIndex = (currentIndex + 1) % online.size();
            this.targetPlayer.set(online.get(nextIndex).getUniqueId(), render);
            render.update();
        });
    }

    private void renderReloadButton(RenderContext render, Player player) {
        final RDQ RDQ = this.core.get(render);

        render.layoutSlot('R', UnifiedBuilderFactory
            .item(Material.COMPARATOR)
            .setName(this.i18n("reload.name", player).build().component())
            .setLore(this.i18n("reload.lore", player).build().splitLines())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        ).onClick(ctx -> {
            RDQ.getPerkService().reload()
                .thenRun(() -> {
                    this.i18n("message.reloaded", player).send();
                    render.update();
                });
        });
    }

    private void handlePerkClick(Context context, Player player, Perk perk) {
        final RDQ RDQ = this.core.get(context);
        final UUID target = this.targetPlayer.get(context);

        if (target == null) {
            this.i18n("message.no_target", player).send();
            return;
        }

        final PerkService perkService = RDQ.getPerkService();
        perkService.unlockPerk(target, perk.id())
            .thenAccept(success -> {
                if (success) {
                    this.i18n("message.granted", player)
                        .with("perk_id", perk.id())
                        .with("target", getPlayerName(target))
                        .send();
                } else {
                    this.i18n("message.grant_failed", player).send();
                }
            });
    }

    private String getPlayerName(UUID uuid) {
        final Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        final OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() != null ? offline.getName() : "Unknown";
    }

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.NETHER_STAR;
        }
    }
}
*/
