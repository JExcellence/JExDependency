package com.raindropcentral.rdq.database.entity.perk.perks.event;

import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rdq.config.perks.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.EventTriggeredPerk;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Example perk that reduces damage taken by players.
 */
@Entity
@DiscriminatorValue("DAMAGE_REDUCTION")
public class DamageReductionPerk extends EventTriggeredPerk {
    
    protected DamageReductionPerk() {
        super();
    }
    
    public DamageReductionPerk(
        final @NotNull String identifier,
        final @NotNull PerkSection perkSection,
        final @NotNull RDQImpl rdq
        ) {
        super(identifier, perkSection, rdq);
    }
    
    /**
     * Handles entity damage events - reduces damage for players with this perk.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(final @NotNull EntityDamageEvent event) {
        if (
            ! (event.getEntity() instanceof Player player)
        ) {
            return;
        }
        
        if (
            ! this.shouldProcessEventFoRDQPlayer(player.getUniqueId())
        ) {
            return;
        }
        
        final double originalDamage = event.getDamage();
        final double reducedDamage = originalDamage * 0.5;
        event.setDamage(reducedDamage);
        
        this.setCooldownFoRDQPlayer(player);
        this.logPerkActivation(player.getName(), DamageReductionPerk.class.getName());
    }
}