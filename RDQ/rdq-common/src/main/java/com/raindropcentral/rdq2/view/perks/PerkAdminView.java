/*
package com.raindropcentral.rdq2.view.perks;

import com.raindropcentral.rdq2.RDQ;
import com.raindropcentral.rdq2.database.entity.perk.RPerk;
import com.raindropcentral.rdq2.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq2.manager.perk.PerkManager;
import com.raindropcentral.rdq2.perk.runtime.LoadedPerk;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

*/
/**
 * Admin view for managing perks (grant, revoke, enable, disable).
 * Only accessible to players with admin permissions.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 *//*

public class PerkAdminView extends BaseView {

    private static final Logger LOGGER = CentralLogger.getLogger(PerkAdminView.class.getName());

    // Slot positions
    private static final int PERK_INFO_SLOT = 13;
    private static final int GRANT_SLOT = 19;
    private static final int REVOKE_SLOT = 20;
    private static final int ENABLE_SLOT = 24;
    private static final int DISABLE_SLOT = 25;
    private static final int BACK_SLOT = 49;

    // States
    private final State<RDQ> rdq = initialState("plugin");
    private final State<String> perkId = initialState("perkId");
    private final State<String> targetPlayerName = initialState("targetPlayer");

    @Override
    protected String getKey() {
        return "perk_admin_ui";
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(final @NotNull OpenContext openContext) {
        final String id = this.perkId.get(openContext);
        final RDQ plugin = this.rdq.get(openContext);
        final LoadedPerk perk = plugin.getPerkRegistry().get(id);
        final String target = this.targetPlayerName.get(openContext);

        final String perkName = perk != null ?
                this.i18n(perk.config().displayName(), openContext.getPlayer()).build().toString() :
                id;

        return Map.of(
                "perk_name", perkName,
                "player_name", target != null ? target : "Unknown"
        );
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render, @NotNull Player player) {
        try {
            // Check admin permission
            if (!player.hasPermission("rdq.admin.perks")) {
                player.sendMessage(this.i18n("perk.error.no_permission", player).build().toString());
                player.closeInventory();
                return;
            }

            final RDQ plugin = this.rdq.get(render);
            final String id = this.perkId.get(render);
            final String targetName = this.targetPlayerName.get(render);
            final LoadedPerk perk = plugin.getPerkRegistry().get(id);

            if (perk == null) {
                player.sendMessage(this.i18n("perk.error.not_found", player)
                        .with("perk", id)
                        .build().toString());
                player.closeInventory();
                return;
            }

            final Player targetPlayer = Bukkit.getPlayer(targetName);
            final RDQPlayer rdqPlayer = targetPlayer != null ?
                    plugin.getPlayerRepository().findByAttributes(Map.of("uniqueId", targetPlayer.getUniqueId())) :
                    null;

            if (rdqPlayer == null) {
                player.sendMessage(this.i18n("perk.error.player_not_found", player)
                        .with("player", targetName)
                        .build().toString());
                player.closeInventory();
                return;
            }

            final PerkManager perkManager = plugin.getPerkInitializationManager().getPerkManager();
            final RPerk rPerk = plugin.getPerkRepository().findByAttributes(Map.of("identifier", id));
            final boolean isOwned = rPerk != null && perkManager.getPerkStateService().playerOwnsPerk(rdqPlayer, rPerk);
            final boolean isEnabled = rPerk != null && perkManager.getPerkStateService().isPerkEnabled(rdqPlayer, rPerk);

            // Perk info
            render.slot(PERK_INFO_SLOT)
                    .withItem(this.createPerkInfoItem(player, perk, targetName, isOwned, isEnabled));

            // Grant button
            render.slot(GRANT_SLOT)
                    .withItem(this.createGrantButton(player, isOwned))
                    .onClick(click -> {
                        if (!isOwned && rPerk != null) {
                            perkManager.getPerkStateService().grantPerk(rdqPlayer, rPerk, false);
                            player.sendMessage(this.i18n("perk.admin.granted", player)
                                    .with("perk_name", perk.config().displayName())
                                    .with("player", targetName)
                                    .build().toString());
                            render.update();
                        }
                    });

            // Revoke button
            render.slot(REVOKE_SLOT)
                    .withItem(this.createRevokeButton(player, isOwned))
                    .onClick(click -> {
                        if (isOwned && rPerk != null) {
                            perkManager.getPerkStateService().revokePerk(rdqPlayer, rPerk);
                            player.sendMessage(this.i18n("perk.admin.revoked", player)
                                    .with("perk_name", perk.config().displayName())
                                    .with("player", targetName)
                                    .build().toString());
                            render.update();
                        }
                    });

            // Enable button
            render.slot(ENABLE_SLOT)
                    .withItem(this.createEnableButton(player, isOwned, isEnabled))
                    .onClick(click -> {
                        if (isOwned && !isEnabled && rPerk != null) {
                            perkManager.getPerkStateService().enablePerk(rdqPlayer, rPerk, 10); // Max 10 enabled
                            player.sendMessage(this.i18n("perk.admin.enabled", player)
                                    .with("perk_name", perk.config().displayName())
                                    .with("player", targetName)
                                    .build().toString());
                            render.update();
                        }
                    });

            // Disable button
            render.slot(DISABLE_SLOT)
                    .withItem(this.createDisableButton(player, isEnabled))
                    .onClick(click -> {
                        if (isEnabled && rPerk != null) {
                            perkManager.getPerkStateService().disablePerk(rdqPlayer, rPerk);
                            player.sendMessage(this.i18n("perk.admin.disabled", player)
                                    .with("perk_name", perk.config().displayName())
                                    .with("player", targetName)
                                    .build().toString());
                            render.update();
                        }
                    });

            // Back button
            render.slot(BACK_SLOT)
                    .withItem(this.createBackButton(player))
                    .onClick(click -> click.closeForPlayer());

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Error rendering perk admin view", e);
        }
    }

    private ItemStack createPerkInfoItem(@NotNull Player player, @NotNull LoadedPerk perk, 
                                         @NotNull String targetName, boolean isOwned, boolean isEnabled) {
        final List<Component> lore = new ArrayList<>();

        lore.add(Component.text("§7Target: §f" + targetName));
        lore.add(Component.empty());

        final String desc = perk.config().description();
        if (desc != null && !desc.isEmpty()) {
            lore.add(this.i18n(desc, player).build().component());
            lore.add(Component.empty());
        }

        lore.add(Component.text("§7Category: §f" + perk.config().category().getDisplayName()));
        lore.add(Component.text("§7Priority: §f" + perk.config().priority()));
        lore.add(Component.empty());
        lore.add(Component.text("§7Owned: " + (isOwned ? "§a✓" : "§c✗")));
        lore.add(Component.text("§7Enabled: " + (isEnabled ? "§a✓" : "§c✗")));

        return UnifiedBuilderFactory.item(Material.valueOf(perk.config().iconMaterial()))
                .setName(this.i18n(perk.config().displayName(), player).build().component())
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS)
                .build();
    }

    private ItemStack createGrantButton(@NotNull Player player, boolean isOwned) {
        final List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Grant this perk to the player"));
        lore.add(Component.empty());
        if (isOwned) {
            lore.add(Component.text("§c✗ Already owned"));
        } else {
            lore.add(Component.text("§a▶ Click to grant"));
        }

        return UnifiedBuilderFactory.item(isOwned ? Material.GRAY_DYE : Material.LIME_DYE)
                .setName(Component.text("§a§lGrant Perk"))
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private ItemStack createRevokeButton(@NotNull Player player, boolean isOwned) {
        final List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Revoke this perk from the player"));
        lore.add(Component.empty());
        if (!isOwned) {
            lore.add(Component.text("§c✗ Not owned"));
        } else {
            lore.add(Component.text("§c▶ Click to revoke"));
        }

        return UnifiedBuilderFactory.item(isOwned ? Material.RED_DYE : Material.GRAY_DYE)
                .setName(Component.text("§c§lRevoke Perk"))
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private ItemStack createEnableButton(@NotNull Player player, boolean isOwned, boolean isEnabled) {
        final List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Enable this perk for the player"));
        lore.add(Component.empty());
        if (!isOwned) {
            lore.add(Component.text("§c✗ Not owned"));
        } else if (isEnabled) {
            lore.add(Component.text("§c✗ Already enabled"));
        } else {
            lore.add(Component.text("§a▶ Click to enable"));
        }

        return UnifiedBuilderFactory.item((isOwned && !isEnabled) ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(Component.text("§a§lEnable Perk"))
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private ItemStack createDisableButton(@NotNull Player player, boolean isEnabled) {
        final List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7Disable this perk for the player"));
        lore.add(Component.empty());
        if (!isEnabled) {
            lore.add(Component.text("§c✗ Not enabled"));
        } else {
            lore.add(Component.text("§c▶ Click to disable"));
        }

        return UnifiedBuilderFactory.item(isEnabled ? Material.RED_DYE : Material.GRAY_DYE)
                .setName(Component.text("§c§lDisable Perk"))
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private ItemStack createBackButton(@NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.ARROW)
                .setName(Component.text("§f← Back"))
                .setLore(List.of(Component.text("§7Return to previous menu")))
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }
}
*/
