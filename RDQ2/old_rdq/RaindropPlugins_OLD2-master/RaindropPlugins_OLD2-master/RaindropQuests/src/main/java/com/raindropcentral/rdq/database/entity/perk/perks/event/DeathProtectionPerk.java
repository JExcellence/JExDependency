package com.raindropcentral.rdq.database.entity.perk.perks.event;


import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rdq.config.perks.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.EventTriggeredPerk;
import de.jexcellence.translate.api.I18n;
import de.jexcellence.translate.api.MessageKey;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

/**
 * Perk that prevents death once, similar to a totem of undying.
 * When a player would die, this perk cancels the death and applies
 * healing and beneficial effects instead.
 */
@Entity
@DiscriminatorValue("DEATH_PROTECTION")
public class DeathProtectionPerk extends EventTriggeredPerk {
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected DeathProtectionPerk() {
        super();
    }
    
    /**
     * Constructs a new DeathProtectionPerk.
     *
     * @param identifier the unique identifier for this perk
     * @param perkSection the perk configuration section
     * @param rdq the plugin instance for event registration
     */
    public DeathProtectionPerk(
        final @NotNull String identifier,
        final @NotNull PerkSection perkSection,
        final @NotNull RDQImpl rdq
    ) {
        super(identifier, perkSection, rdq);
    }
    
    /**
     * Handles player death events - prevents death and applies totem-like effects.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(final @NotNull PlayerDeathEvent event) {
        final Player player = event.getEntity();
        
        if (!this.shouldProcessEventFoRDQPlayer(player.getUniqueId())) {
            return;
        }
        
        event.setCancelled(true);
        
        player.setHealth(1);
        player.setFireTicks(0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0));
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);

        I18n.create(MessageKey.of("perk.triggered_perk.death_protection"), player).includePrefix().sendMessage();
        
        this.setCooldownFoRDQPlayer(player);
        this.logPerkActivation(player.getName(), DeathProtectionPerk.class.getName());
    }
}