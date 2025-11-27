package com.raindropcentral.rdq.database.entity.perk.event;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.EventTriggeredPerk;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;

/** Doubles experience gained from kills for active players with cooldown. */
@Entity
@DiscriminatorValue("DOUBLE_EXPERIENCE")
public class DoubleExperiencePerk extends EventTriggeredPerk {

    protected DoubleExperiencePerk() { super(); }

    public DoubleExperiencePerk(
            final @NotNull String identifier,
            final @NotNull PerkSection perkSection,
            final @NotNull RDQ rdq
    ) {
        super(identifier, perkSection, rdq);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(final @NotNull EntityDeathEvent event) {
        final Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!this.shouldProcessEventForPlayer(killer.getUniqueId())) return;

        final int originalExp = event.getDroppedExp();
        if (originalExp <= 0) return;

        final int doubledExp = originalExp * 2;
        event.setDroppedExp(doubledExp);

        this.setCooldownForPlayer(killer);
        this.logPerkActivation(killer.getName(), DoubleExperiencePerk.class.getName());

        TranslationService.create(TranslationKey.of("perk.triggered_perk.doubled_experience"), killer).withPrefix().with("experience", doubledExp).send();
    }
}
