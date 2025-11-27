package com.raindropcentral.rdq.database.entity.perk.potion;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.PotionEffectPerk;
import com.raindropcentral.rdq.type.EPerkIdentifier;
import com.raindropcentral.rdq.type.EPerkType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

/** Fire resistance toggleable passive perk. */
@Entity
@DiscriminatorValue("FIRE_RESISTANCE")
public class FireResistance extends PotionEffectPerk {

    protected FireResistance() { super(); }

    public FireResistance(final @NotNull PerkSection perkSection) {
        super(
                EPerkIdentifier.FIRE_RESISTANCE.getIdentifier(),
                perkSection,
                EPerkType.TOGGLEABLE_PASSIVE,
                EPerkIdentifier.FIRE_RESISTANCE.getIdentifier()
        );
    }
}
