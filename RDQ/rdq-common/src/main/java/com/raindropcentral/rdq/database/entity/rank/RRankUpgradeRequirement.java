package com.raindropcentral.rdq.database.entity.rank;


import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Objects;

@Entity
@Table(name = "r_rank_upgrade_requirement")
public final class RRankUpgradeRequirement extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rank_id", nullable = false)
    private RRank rank;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requirement_id", nullable = false)
    private RRequirement requirement;

    @Column(name = "icon", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = IconSectionConverter.class)
    private IconSection icon;

    @Column(name = "display_order")
    private int displayOrder = 0;

    @Version
    @Column(name = "version")
    private int version;

    protected RRankUpgradeRequirement() {}

    public RRankUpgradeRequirement(final @Nullable RRank rank, final @NotNull RRequirement requirement,
                                   final @NotNull IconSection icon) {
        this.rank = rank;
        this.requirement = Objects.requireNonNull(requirement, "requirement cannot be null");
        this.icon = Objects.requireNonNull(icon, "icon cannot be null");
        if (rank != null) {
            rank.addUpgradeRequirement(this);
        }
    }

    public @NotNull RRank getRank() {
        return this.rank;
    }

    public void setRank(final @Nullable RRank rank) {
        if (this.rank != null && this.rank != rank) {
            this.rank.getUpgradeRequirements();
        }
        this.rank = rank;
        if (rank != null) {
            rank.addUpgradeRequirement(this);
        }
    }

    public @NotNull RRequirement getRequirement() {
        return this.requirement;
    }

    public void setRequirement(final @NotNull RRequirement requirement) {
        this.requirement = Objects.requireNonNull(requirement, "requirement cannot be null");
    }

    public @NotNull IconSection getIcon() {
        return this.icon;
    }

    public void setIcon(final @NotNull IconSection icon) {
        this.icon = Objects.requireNonNull(icon, "icon cannot be null");
    }

    public int getDisplayOrder() {
        return this.displayOrder;
    }

    public void setDisplayOrder(final int displayOrder) {
        this.displayOrder = displayOrder;
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

    public int getVersion() {
        return this.version;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RRankUpgradeRequirement other)) return false;
        if (getId() != null && other.getId() != null) {
            return getId().equals(other.getId());
        }
        return Objects.equals(this.requirement, other.requirement) &&
               Objects.equals(this.rank, other.rank) &&
               this.displayOrder == other.displayOrder;
    }

    @Override
    public int hashCode() {
        if (getId() != null) {
            return getId().hashCode();
        }
        return Objects.hash(this.requirement, this.rank, this.displayOrder);
    }

    @Override
    public String toString() {
        return "RRankUpgradeRequirement[id=%d, rank=%s, displayOrder=%d]"
                .formatted(getId(), rank != null ? rank.getIdentifier() : "null", displayOrder);
    }
}