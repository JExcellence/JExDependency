package com.raindropcentral.rdq.database.entity.perk.potion;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.PotionEffectPerk;
import com.raindropcentral.rdq.type.EPerkIdentifier;
import com.raindropcentral.rdq.type.EPerkType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

/** Saturation toggleable passive perk. */
@Entity
@DiscriminatorValue("SATURATION")
public class Saturation extends PotionEffectPerk {

    protected Saturation() { super(); }

    public Saturation(final @NotNull PerkSection perkSection) {
        super(
                EPerkIdentifier.SATURATION.getIdentifier(),
                perkSection,
                EPerkType.TOGGLEABLE_PASSIVE,
                EPerkIdentifier.SATURATION.getIdentifier()
        );
    }
}
