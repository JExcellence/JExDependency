package com.raindropcentral.rdq.database.entity.perk.perks.event;

import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rdq.config.perks.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.EventTriggeredPerk;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Perk that prevents players from losing experience on death.
 * <p>
 * When a player with this perk dies, their experience points and levels
 * are preserved instead of being dropped or lost.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@Entity
@DiscriminatorValue("KEEP_EXPERIENCE")
public class KeepExperiencePerk extends EventTriggeredPerk {
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected KeepExperiencePerk() {
        super();
    }
    
    /**
     * Constructs a new KeepExperiencePerk.
     *
     * @param identifier the unique identifier for this perk
     * @param perkSection the perk configuration section
     */
    public KeepExperiencePerk(
        final @NotNull String identifier,
        final @NotNull PerkSection perkSection,
        final @NotNull RDQImpl rdq
    ) {
        super(identifier, perkSection, rdq);
    }
    
    
    protected boolean handleDeathEvent(final @NotNull PlayerDeathEvent event) {
        final Player player = event.getEntity();
        
        
        final int totalExp = player.getTotalExperience();
        final int level = player.getLevel();
        final float exp = player.getExp();
        
        
        event.setDroppedExp(0);
        
        
        Bukkit.getScheduler().runTask(this.getRdq().getImpl(), () -> {
            if (player.isOnline()) {
                player.setTotalExperience(totalExp);
                player.setLevel(level);
                player.setExp(exp);
            }
        });
        
        return true;
    }
}