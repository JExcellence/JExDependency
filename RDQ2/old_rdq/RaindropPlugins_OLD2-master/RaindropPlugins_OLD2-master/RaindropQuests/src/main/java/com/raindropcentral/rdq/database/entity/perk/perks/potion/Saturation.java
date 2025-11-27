package com.raindropcentral.rdq.database.entity.perk.perks.potion;

import com.raindropcentral.rdq.config.perks.PerkSection;
import com.raindropcentral.rdq.database.entity.perk.PotionEffectPerk;
import com.raindropcentral.rdq.type.EPerkIdentifier;
import com.raindropcentral.rdq.type.EPerkType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Speed perk that grants players increased movement speed.
 * <p>
 * This toggleable passive perk applies the Speed potion effect to players
 * when activated. The amplifier level can be configured through the database
 * fields and permission-based amplifier system, with sensible defaults provided.
 * </p>
 * <p>
 * The perk integrates with the permission-based amplifier system, allowing
 * different speed levels based on player permissions.
 * </p>
 *
 * @author JExcellence
 * @version 3.0.0
 * @since TBD
 */
@Entity
@DiscriminatorValue("SATURATION")
public class Saturation extends PotionEffectPerk {
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected Saturation() {
        super();
    }
    
    /**
     * Constructs a new Haste perk.
     *
     * @param perkSection the perk configuration section
     */
    public Saturation(
        final @NotNull PerkSection perkSection
    ) {
        super(
            EPerkIdentifier.SATURATION.getIdentifier(),
            perkSection,
            EPerkType.TOGGLEABLE_PASSIVE,
            EPerkIdentifier.SATURATION.getIdentifier()
        );
    }
}