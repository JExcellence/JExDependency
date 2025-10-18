package com.raindropcentral.rdq.database.entity.rank;


import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rdq.database.converter.RequirementConverter;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;

@Entity
@Table(name = "r_requirement")
public final class RRequirement extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "requirement_data", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = RequirementConverter.class)
    private AbstractRequirement requirement;

    @Convert(converter = IconSectionConverter.class)
    @Column(name = "requirement_icon", nullable = false, columnDefinition = "LONGTEXT")
    private IconSection icon;

    protected RRequirement() {}

    public RRequirement(final @NotNull AbstractRequirement requirement, final @NotNull IconSection icon) {
        this.requirement = Objects.requireNonNull(requirement, "requirement cannot be null");
        this.icon = Objects.requireNonNull(icon, "icon cannot be null");
    }

    public @NotNull AbstractRequirement getRequirement() {
        return this.requirement;
    }

    public void setRequirement(final @NotNull AbstractRequirement requirement) {
        this.requirement = Objects.requireNonNull(requirement, "requirement cannot be null");
    }

    public @NotNull IconSection getShowcase() {
        return this.icon;
    }

    public void setShowcase(final @NotNull IconSection icon) {
        this.icon = Objects.requireNonNull(icon, "icon cannot be null");
    }

    public boolean isMet(final @NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        return this.requirement.isMet(player);
    }

    public double calculateProgress(final @NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        return this.requirement.calculateProgress(player);
    }

    public void consume(final @NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        this.requirement.consume(player);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RRequirement other)) return false;
        return Objects.equals(getId(), other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "RRequirement[id=%d, requirement=%s]".formatted(getId(), requirement.getClass().getSimpleName());
    }
}