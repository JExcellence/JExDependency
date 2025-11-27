package com.raindropcentral.rdq.database.entity.perk.potion;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.PotionEffectPerk;
import com.raindropcentral.rdq.type.EPerkIdentifier;
import com.raindropcentral.rdq.type.EPerkType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

/** Haste toggleable passive perk. */
@Entity
@DiscriminatorValue("HASTE")
public class Haste extends PotionEffectPerk {

    protected Haste() { super(); }

    public Haste(final @NotNull PerkSection perkSection) {
        super(
                EPerkIdentifier.HASTE.getIdentifier(),
                perkSection,
                EPerkType.TOGGLEABLE_PASSIVE,
                EPerkIdentifier.HASTE.getIdentifier()
        );
    }
}
