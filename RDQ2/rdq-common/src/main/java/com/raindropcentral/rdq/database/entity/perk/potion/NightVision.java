package com.raindropcentral.rdq.database.entity.perk.potion;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.PotionEffectPerk;
import com.raindropcentral.rdq.type.EPerkIdentifier;
import com.raindropcentral.rdq.type.EPerkType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

/** Night vision toggleable passive perk. */
@Entity
@DiscriminatorValue("NIGHT_VISION")
public class NightVision extends PotionEffectPerk {

    protected NightVision() { super(); }

    public NightVision(final @NotNull PerkSection perkSection) {
        super(
                EPerkIdentifier.NIGHT_VISION.getIdentifier(),
                perkSection,
                EPerkType.TOGGLEABLE_PASSIVE,
                EPerkIdentifier.NIGHT_VISION.getIdentifier()
        );
    }
}
