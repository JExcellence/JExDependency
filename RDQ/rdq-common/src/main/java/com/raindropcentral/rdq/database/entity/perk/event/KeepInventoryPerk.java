package com.raindropcentral.rdq.database.entity.perk.event;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.EventTriggeredPerk;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/** Prevents inventory loss on death for active players. */
@Entity
@DiscriminatorValue("KEEP_INVENTORY")
public class KeepInventoryPerk extends EventTriggeredPerk {

    protected KeepInventoryPerk() { super(); }

    public KeepInventoryPerk(
            final @NotNull String identifier,
            final @NotNull PerkSection perkSection,
            final @NotNull RDQ rdq
    ) {
        super(identifier, perkSection, rdq);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(final @NotNull PlayerDeathEvent event) {
        final Player player = event.getEntity();
        if (!this.shouldProcessEventForPlayer(player.getUniqueId())) return;

        final ItemStack[] contents = player.getInventory().getContents().clone();
        final ItemStack[] armor = player.getInventory().getArmorContents().clone();
        final ItemStack[] extra = player.getInventory().getExtraContents().clone();

        event.getDrops().clear();

        Bukkit.getScheduler().runTask(this.getRdq().getPlugin(), () -> {
            if (player.isOnline()) {
                player.getInventory().setContents(contents);
                player.getInventory().setArmorContents(armor);
                player.getInventory().setExtraContents(extra);
                player.updateInventory();
            }
        });
    }
}
