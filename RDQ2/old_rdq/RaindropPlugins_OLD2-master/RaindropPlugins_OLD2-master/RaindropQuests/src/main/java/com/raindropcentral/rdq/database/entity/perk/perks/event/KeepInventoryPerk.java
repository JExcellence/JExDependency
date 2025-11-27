package com.raindropcentral.rdq.database.entity.perk.perks.event;

import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rdq.config.perks.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.EventTriggeredPerk;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Perk that prevents players from losing their inventory on death.
 * <p>
 * When a player with this perk dies, their inventory items are preserved
 * instead of being dropped at the death location.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@Entity
@DiscriminatorValue("KEEP_INVENTORY")
public class KeepInventoryPerk extends EventTriggeredPerk {
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected KeepInventoryPerk() {
        super();
    }
    
    /**
     * Constructs a new KeepInventoryPerk.
     *
     * @param identifier the unique identifier for this perk
     * @param perkSection the perk configuration section
     */
    public KeepInventoryPerk(
        final @NotNull String identifier,
        final @NotNull PerkSection perkSection,
        final @NotNull RDQImpl rdq
        ) {
        super(identifier, perkSection, rdq);
    }
    
    protected boolean handleDeathEvent(final @NotNull PlayerDeathEvent event) {
        final Player player = event.getEntity();
        
        final ItemStack[] inventoryContents = player.getInventory().getContents().clone();
        final ItemStack[] armorContents = player.getInventory().getArmorContents().clone();
        final ItemStack[] extraContents = player.getInventory().getExtraContents().clone();
        
        event.getDrops().clear();
        
        Bukkit.getScheduler().runTask(this.getRdq().getImpl(), () -> {
            if (player.isOnline()) {
                player.getInventory().setContents(inventoryContents);
                player.getInventory().setArmorContents(armorContents);
                player.getInventory().setExtraContents(extraContents);
                player.updateInventory();
            }
        });
        
        return true;
    }
}