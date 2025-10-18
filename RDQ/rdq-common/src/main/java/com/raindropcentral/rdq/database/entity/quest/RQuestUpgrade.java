package com.raindropcentral.rdq.database.entity.quest;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "r_quest_upgrade")
public final class RQuestUpgrade extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private RQuest quest;

    @OneToMany(mappedBy = "questUpgrade", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<RQuestUpgradeRequirement> upgradeRequirements = new HashSet<>();

    protected RQuestUpgrade() {}

    public RQuestUpgrade(final @NotNull RQuest quest) {
        this.quest = Objects.requireNonNull(quest, "quest cannot be null");
    }

    public @NotNull RQuest getQuest() {
        return this.quest;
    }

    public void setQuest(final @NotNull RQuest quest) {
        this.quest = Objects.requireNonNull(quest, "quest cannot be null");
    }

    public @NotNull Set<RQuestUpgradeRequirement> getUpgradeRequirements() {
        return Collections.unmodifiableSet(this.upgradeRequirements);
    }

    public @NotNull List<RQuestUpgradeRequirement> getRequirementsForLevel(final int level) {
        return this.upgradeRequirements.stream()
                .filter(requirement -> requirement.getUpgradeLevel() == level)
                .collect(Collectors.toList());
    }

    public boolean addUpgradeRequirement(final @NotNull RQuestUpgradeRequirement requirement) {
        Objects.requireNonNull(requirement, "requirement cannot be null");
        final boolean added = this.upgradeRequirements.add(requirement);
        if (added && requirement.getQuestUpgrade() != this) {
            requirement.setQuestUpgrade(this);
        }
        return added;
    }

    public boolean removeUpgradeRequirement(final @NotNull RQuestUpgradeRequirement requirement) {
        Objects.requireNonNull(requirement, "requirement cannot be null");
        return this.upgradeRequirements.remove(requirement);
    }

    public @NotNull SortedSet<Integer> getAllUpgradeLevels() {
        return this.upgradeRequirements.stream()
                .map(RQuestUpgradeRequirement::getUpgradeLevel)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RQuestUpgrade other)) return false;
        return Objects.equals(getId(), other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "RQuestUpgrade[id=%d, quest=%s, requirements=%d]"
                .formatted(getId(), quest != null ? quest.getIdentifier() : "null", upgradeRequirements.size());
    }
}