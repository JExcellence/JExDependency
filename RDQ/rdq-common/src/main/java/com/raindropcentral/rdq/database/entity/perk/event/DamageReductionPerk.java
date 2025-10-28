package com.raindropcentral.rdq.database.entity.perk.event;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.EventTriggeredPerk;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

/** Example perk that reduces incoming damage for active players. */
@Entity
@DiscriminatorValue("DAMAGE_REDUCTION")
public class DamageReductionPerk extends EventTriggeredPerk {

    protected DamageReductionPerk() { super(); }

    public DamageReductionPerk(
            final @NotNull String identifier,
            final @NotNull PerkSection perkSection,
            final @NotNull RDQ rdq
    ) {
        super(identifier, perkSection, rdq);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(final @NotNull EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!this.shouldProcessEventForPlayer(player.getUniqueId())) return;

        final double original = event.getDamage();
        final double reduced = original * 0.5D;
        event.setDamage(reduced);

        this.setCooldownForPlayer(player);
        this.logPerkActivation(player.getName(), DamageReductionPerk.class.getName());
    }
}
