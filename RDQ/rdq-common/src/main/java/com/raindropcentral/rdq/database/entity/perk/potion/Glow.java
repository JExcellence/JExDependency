package com.raindropcentral.rdq.database.entity.perk.potion;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.PotionEffectPerk;
import com.raindropcentral.rdq.type.EPerkIdentifier;
import com.raindropcentral.rdq.type.EPerkType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

/** Glowing toggleable passive perk. */
@Entity
@DiscriminatorValue("GLOW")
public class Glow extends PotionEffectPerk {

    protected Glow() { super(); }

    public Glow(final @NotNull PerkSection perkSection) {
        super(
                EPerkIdentifier.GLOW.getIdentifier(),
                perkSection,
                EPerkType.TOGGLEABLE_PASSIVE,
                EPerkIdentifier.GLOW.getIdentifier()
        );
    }
}
