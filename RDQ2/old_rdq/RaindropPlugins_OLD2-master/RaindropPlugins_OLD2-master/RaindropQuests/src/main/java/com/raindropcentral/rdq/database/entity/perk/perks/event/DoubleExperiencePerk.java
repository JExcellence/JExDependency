package com.raindropcentral.rdq.database.entity.perk.perks.event;


import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rdq.config.perks.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.EventTriggeredPerk;
import de.jexcellence.translate.api.I18n;
import de.jexcellence.translate.api.MessageKey;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Perk that doubles experience gained from killing entities.
 * <p>
 * When a player with this perk kills an entity (mob or player), they receive
 * double the normal experience points. This perk has a cooldown to prevent abuse.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@Entity
@DiscriminatorValue("DOUBLE_EXPERIENCE")
public class DoubleExperiencePerk extends EventTriggeredPerk {
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected DoubleExperiencePerk() {
        super();
    }
    
    /**
     * Constructs a new DoubleExperiencePerk.
     *
     * @param identifier the unique identifier for this perk
     * @param perkSection the perk configuration section
     * @param rdq the plugin instance for event registration
     */
    public DoubleExperiencePerk(
        final @NotNull String identifier,
        final @NotNull PerkSection perkSection,
        final @NotNull RDQImpl rdq
    ) {
        super(identifier, perkSection, rdq);
    }
    
    /**
     * Handles entity death events to double experience for the killer.
     * This includes all entities: mobs, animals, and players.
     *
     * @param event the EntityDeathEvent
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(final @NotNull EntityDeathEvent event) {
        final Player killer = event.getEntity().getKiller();
        
        if (killer == null) {
            return;
        }
        
        if (!this.shouldProcessEventFoRDQPlayer(killer.getUniqueId())) {
            return;
        }
        
        final int originalExp = event.getDroppedExp();
        
        if (originalExp <= 0) {
            return;
        }
        
        final int doubledExp = originalExp * 2;
        event.setDroppedExp(doubledExp);
        
        this.setCooldownFoRDQPlayer(killer);
        
        this.logPerkActivation(killer.getName(), DoubleExperiencePerk.class.getName());

        I18n.create(
                MessageKey.of("perk.triggered_perk.doubled_experience"),
                killer
        ).includePrefix().withPlaceholder("experience", doubledExp).sendMessage();
    }
    
}