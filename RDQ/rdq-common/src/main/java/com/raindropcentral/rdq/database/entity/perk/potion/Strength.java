package com.raindropcentral.rdq.database.entity.perk.potion;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.PotionEffectPerk;
import com.raindropcentral.rdq.type.EPerkIdentifier;
import com.raindropcentral.rdq.type.EPerkType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

/** Strength toggleable passive perk. */
@Entity
@DiscriminatorValue("STRENGTH")
public class Strength extends PotionEffectPerk {

    protected Strength() { super(); }

    public Strength(final @NotNull PerkSection perkSection) {
        super(
                EPerkIdentifier.STRENGTH.getIdentifier(),
                perkSection,
                EPerkType.TOGGLEABLE_PASSIVE,
                EPerkIdentifier.STRENGTH.getIdentifier()
        );
    }
}
