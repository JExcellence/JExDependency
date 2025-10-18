package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rdq.database.converter.RequirementConverter;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.database.converter.BasicMaterialConverter;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;

@Entity
@Table(name = "r_quest_upgrade_requirement")
public final class RQuestUpgradeRequirement extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_upgrade_id")
    private RQuestUpgrade questUpgrade;

    @Column(name = "requirement_data", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = RequirementConverter.class)
    private AbstractRequirement requirement;

    @Column(name = "upgrade_level", nullable = false)
    private int upgradeLevel;

    @Column(name = "showcase", nullable = false)
    @Convert(converter = BasicMaterialConverter.class)
    private Material showcase;

    protected RQuestUpgradeRequirement() {}

    public RQuestUpgradeRequirement(final @NotNull RQuestUpgrade questUpgrade,
                                    final @NotNull AbstractRequirement requirement,
                                    final int upgradeLevel) {
        this.questUpgrade = Objects.requireNonNull(questUpgrade, "questUpgrade cannot be null");
        this.requirement = Objects.requireNonNull(requirement, "requirement cannot be null");
        this.upgradeLevel = upgradeLevel;
        this.showcase = Material.PAPER;
    }

    public @NotNull RQuestUpgrade getQuestUpgrade() {
        return this.questUpgrade;
    }

    public void setQuestUpgrade(final @NotNull RQuestUpgrade questUpgrade) {
        this.questUpgrade = Objects.requireNonNull(questUpgrade, "questUpgrade cannot be null");
    }

    public @NotNull AbstractRequirement getRequirement() {
        return this.requirement;
    }

    public void setRequirement(final @NotNull AbstractRequirement requirement) {
        this.requirement = Objects.requireNonNull(requirement, "requirement cannot be null");
    }

    public int getUpgradeLevel() {
        return this.upgradeLevel;
    }

    public void setUpgradeLevel(final int upgradeLevel) {
        this.upgradeLevel = upgradeLevel;
    }

    public @NotNull Material getShowcase() {
        return this.showcase;
    }

    public void setShowcase(final @NotNull Material showcase) {
        this.showcase = Objects.requireNonNull(showcase, "showcase cannot be null");
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RQuestUpgradeRequirement other)) return false;
        return Objects.equals(getId(), other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "RQuestUpgradeRequirement[id=%d, level=%d, requirement=%s]"
                .formatted(getId(), upgradeLevel, requirement.getClass().getSimpleName());
    }
}