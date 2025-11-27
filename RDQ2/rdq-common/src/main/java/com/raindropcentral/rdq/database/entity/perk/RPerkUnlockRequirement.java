package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.database.entity.rank.RRequirement;
import com.raindropcentral.rdq.database.entity.requirement.RequirementAssociation;
import jakarta.persistence.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Objects;

@Entity @Table(name = "r_perk_unlock_requirement")
public final class RPerkUnlockRequirement extends RequirementAssociation {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "perk_id", nullable = false)
    private RPerk perk;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requirement_id", nullable = false)
    private RRequirement requirement;

    protected RPerkUnlockRequirement() {
    }

    public RPerkUnlockRequirement(
            final @NotNull RPerk perk,
            final @NotNull RRequirement requirement,
            final @NotNull IconSection icon
    ) {
        this.perk = perk;
        this.requirement = requirement;
        this.setIcon(icon);

        this.perk.addUnlockRequirement(this);
    }

    public @Nullable RPerk getPerk() {
        return this.perk;
    }

    public void setPerk(final @Nullable RPerk perk) {
        if (this.perk != null && this.perk != perk) {
            this.perk.getUnlockRequirements();
        }
        this.perk = perk;
        if (perk != null) {
            perk.addUnlockRequirement(this);
        }
    }

    @Override
    public @NotNull RRequirement getRequirement() {
        return this.requirement;
    }

    @Override
    public void setRequirement(final @NotNull RRequirement requirement) {
        this.requirement = requirement;
    }

    public boolean isMet(final @NotNull Player player) {
        return this.requirement.isMet(player);
    }

    public double calculateProgress(final @NotNull Player player) {
        return this.requirement.calculateProgress(player);
    }

    public void consume(final @NotNull Player player) {
        this.requirement.consume(player);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RPerkUnlockRequirement other)) return false;
        if (getId() != null && other.getId() != null) {
            return getId().equals(other.getId());
        }
        return Objects.equals(this.requirement, other.requirement) &&
                Objects.equals(this.perk, other.perk) &&
                this.getDisplayOrder() == other.getDisplayOrder();
    }

    @Override
    public int hashCode() {
        if (getId() != null) {
            return getId().hashCode();
        }
        return Objects.hash(this.requirement, this.perk, this.getDisplayOrder());
    }

    @Override
    public String toString() {
        return "RPerkUnlockRequirement[id=%d, perk=%s, displayOrder=%d]"
                .formatted(getId(), perk != null ? perk.getIdentifier() : "null", getDisplayOrder());
    }
}