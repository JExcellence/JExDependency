package com.raindropcentral.rdq.database.entity.perk.potion;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.PotionEffectPerk;
import com.raindropcentral.rdq.type.EPerkIdentifier;
import com.raindropcentral.rdq.type.EPerkType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

/** Jump boost toggleable passive perk. */
@Entity
@DiscriminatorValue("JUMP_BOOST")
public class JumpBoost extends PotionEffectPerk {

    protected JumpBoost() { super(); }

    public JumpBoost(final @NotNull PerkSection perkSection) {
        super(
                EPerkIdentifier.JUMP_BOOST.getIdentifier(),
                perkSection,
                EPerkType.TOGGLEABLE_PASSIVE,
                EPerkIdentifier.JUMP_BOOST.getIdentifier()
        );
    }
}
