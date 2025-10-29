package com.raindropcentral.rdq.perk.runtime;

import com.raindropcentral.rdq.manager.perk.PerkManager;
import com.raindropcentral.rdq.perk.event.PerkEventBus;
import com.raindropcentral.rdq.perk.event.PerkEventListener;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Random;

/**
 * Perk service that grants bonus loot when breaking blocks.
 *
 * <p>When activated, players have a chance to receive bonus items when breaking
 * ore blocks. The chance and bonus amount scale with permission levels.
 *
 * @author JExcellence
 * @version 1.0.2
 * @since 3.2.0
 */
public class TreasureHunterPerkService implements PerkEventListener, Listener {

    private final PerkManager perkManager;
    private final Random random = new Random();
    private static final String PERK_ID = "treasure_hunter";

    public TreasureHunterPerkService(@NotNull PerkManager perkManager, @NotNull PerkEventBus perkEventBus) {
        this.perkManager = perkManager;
        perkEventBus.register(this);
    }

    @EventHandler
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        Player player = event.getPlayer();
        final var runtime = perkManager.findRuntime(PERK_ID);
        if (runtime.isEmpty() || !runtime.get().isActive(player)) {
            return;
        }

        if (!isOreBlock(event.getBlock().getType())) {
            return;
        }

        LoadedPerk perk = perkManager.getPerkRegistry().get(PERK_ID);
        if (perk == null) {
            return;
        }

        int triggerChance = getTriggerChance(player, perk);
        if (random.nextInt(100) >= triggerChance) {
            return;
        }

        ItemStack bonus = event.getBlock().getDrops().stream()
                .findFirst()
                .orElse(null);

        if (bonus != null) {
            bonus.setAmount(bonus.getAmount() + getBonusAmount(player, perk));
            player.getWorld().dropItemNaturally(event.getBlock().getLocation(), bonus);
        }
    }

    @Override
    public void onPerkActivated(@NotNull Player player, @NotNull String perkId) {
    }

    @Override
    public void onPerkDeactivated(@NotNull Player player, @NotNull String perkId) {
    }

    @Override
    public void onPerkTriggered(@NotNull Player player, @NotNull String perkId) {
    }

    @Override
    public void onPerkCooldownStart(@NotNull Player player, @NotNull String perkId, long durationSeconds) {
    }

    @Override
    public void onPerkCooldownEnd(@NotNull Player player, @NotNull String perkId) {
    }

    private int getTriggerChance(@NotNull Player player, @NotNull LoadedPerk perk) {
        int baseChance = 15;
        int amplifier = getPermissionAmplifier(player, perk);
        return Math.min(baseChance + (amplifier * 5), 50);
    }

    private int getBonusAmount(@NotNull Player player, @NotNull LoadedPerk perk) {
        int baseBonus = 1;
        int amplifier = getPermissionAmplifier(player, perk);
        return baseBonus + amplifier;
    }

    private int getPermissionAmplifier(@NotNull Player player, @NotNull LoadedPerk perk) {
        for (Map.Entry<String, Integer> entry : perk.config().permissionAmplifiers().entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                return entry.getValue();
            }
        }
        return 0;
    }

    private boolean isOreBlock(@NotNull Material material) {
        return material.name().contains("ORE") || material.name().contains("DEEPSLATE");
    }
}
