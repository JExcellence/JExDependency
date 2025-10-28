package com.raindropcentral.rdq.database.entity.perk.event;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.EventTriggeredPerk;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
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

/** Prevents death once and applies totem-like effects. */
@Entity
@DiscriminatorValue("DEATH_PROTECTION")
public class DeathProtectionPerk extends EventTriggeredPerk {

    protected DeathProtectionPerk() { super(); }

    public DeathProtectionPerk(
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

        event.setCancelled(true);
        player.setHealth(1);
        player.setFireTicks(0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0));
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);

        TranslationService.create(TranslationKey.of("perk.triggered_perk.death_protection"), player).withPrefix().send();

        this.setCooldownForPlayer(player);
        this.logPerkActivation(player.getName(), DeathProtectionPerk.class.getName());
    }
}
