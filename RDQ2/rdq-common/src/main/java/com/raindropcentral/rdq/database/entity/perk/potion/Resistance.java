package com.raindropcentral.rdq.database.entity.perk.potion;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.PotionEffectPerk;
import com.raindropcentral.rdq.type.EPerkIdentifier;
import com.raindropcentral.rdq.type.EPerkType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

/** Resistance toggleable passive perk. */
@Entity
@DiscriminatorValue("RESISTANCE")
public class Resistance extends PotionEffectPerk {

    protected Resistance() { super(); }

    public Resistance(final @NotNull PerkSection perkSection) {
        super(
                EPerkIdentifier.RESISTANCE.getIdentifier(),
                perkSection,
                EPerkType.TOGGLEABLE_PASSIVE,
                EPerkIdentifier.RESISTANCE.getIdentifier()
        );
    }
}
