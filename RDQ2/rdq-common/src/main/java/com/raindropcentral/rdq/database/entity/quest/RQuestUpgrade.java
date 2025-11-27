package com.raindropcentral.rdq.database.entity.quest;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a quest upgrade record containing the requirements needed to advance a quest.
 *
 * <p>The entity primarily acts as the parent for {@link RQuestUpgradeRequirement} instances and
 * ensures the bidirectional relationship with {@link RQuest} remains consistent when
 * requirements are added or removed.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
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

    /**
     * Required by JPA for entity construction through reflection.
     */
    protected RQuestUpgrade() {}

    /**
     * Creates a new quest upgrade for the supplied quest.
     *
     * @param quest the quest that owns this upgrade
     * @throws NullPointerException if the quest is {@code null}
     */
    public RQuestUpgrade(final @NotNull RQuest quest) {
        this.quest = Objects.requireNonNull(quest, "quest cannot be null");
    }

    /**
     * Retrieves the quest associated with this upgrade.
     *
     * @return the non-null quest that owns this upgrade
     */
    public @NotNull RQuest getQuest() {
        return this.quest;
    }

    /**
     * Updates the quest reference for this upgrade.
     *
     * @param quest the new quest that should own this upgrade
     * @throws NullPointerException if the quest is {@code null}
     */
    public void setQuest(final @NotNull RQuest quest) {
        this.quest = Objects.requireNonNull(quest, "quest cannot be null");
    }

    /**
     * Provides an immutable view of all requirements registered for this upgrade.
     *
     * @return an unmodifiable set containing every upgrade requirement
     */
    public @NotNull Set<RQuestUpgradeRequirement> getUpgradeRequirements() {
        return Collections.unmodifiableSet(this.upgradeRequirements);
    }

    /**
     * Retrieves every requirement that applies to the given upgrade level.
     *
     * @param level the level to locate requirements for
     * @return a list of requirements configured for the supplied level
     */
    public @NotNull List<RQuestUpgradeRequirement> getRequirementsForLevel(final int level) {
        return this.upgradeRequirements.stream()
                .filter(requirement -> requirement.getUpgradeLevel() == level)
                .collect(Collectors.toList());
    }

    /**
     * Adds a requirement to this upgrade, ensuring the bidirectional reference is maintained.
     *
     * @param requirement the requirement to register
     * @return {@code true} if the requirement was added, {@code false} otherwise
     * @throws NullPointerException if the requirement is {@code null}
     */
    public boolean addUpgradeRequirement(final @NotNull RQuestUpgradeRequirement requirement) {
        Objects.requireNonNull(requirement, "requirement cannot be null");
        final boolean added = this.upgradeRequirements.add(requirement);
        if (added && requirement.getQuestUpgrade() != this) {
            requirement.setQuestUpgrade(this);
        }
        return added;
    }

    /**
     * Removes a requirement from this upgrade.
     *
     * @param requirement the requirement to remove
     * @return {@code true} if the requirement was present and removed
     * @throws NullPointerException if the requirement is {@code null}
     */
    public boolean removeUpgradeRequirement(final @NotNull RQuestUpgradeRequirement requirement) {
        Objects.requireNonNull(requirement, "requirement cannot be null");
        return this.upgradeRequirements.remove(requirement);
    }

    /**
     * Collects all levels for which requirements have been configured.
     *
     * @return a sorted set containing every upgrade level represented by requirements
     */
    public @NotNull SortedSet<Integer> getAllUpgradeLevels() {
        return this.upgradeRequirements.stream()
                .map(RQuestUpgradeRequirement::getUpgradeLevel)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RQuestUpgrade other)) return false;
        return Objects.equals(getId(), other.getId());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "RQuestUpgrade[id=%d, quest=%s, requirements=%d]"
                .formatted(getId(), quest != null ? quest.getIdentifier() : "null", upgradeRequirements.size());
    }
}