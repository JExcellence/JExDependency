package com.raindropcentral.rdq.database.entity.perk;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;

/**
 * Represents a requirement that must be met before a player can unlock or receive a perk.
 * Requirements can include level thresholds, quest completions, currency costs, or custom conditions.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(name = "r_perk_requirement")
public final class RPerkRequirement extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perk_id", nullable = false)
    private RPerk perk;

    @Column(name = "requirement_type", nullable = false, length = 64)
    private String requirementType;

    @Column(name = "requirement_value", nullable = false)
    private String requirementValue;

    @Column(name = "description_key", nullable = false)
    private String descriptionKey;

    @Column(name = "is_optional", nullable = false)
    private boolean optional = false;

    @Column(name = "priority", nullable = false)
    private int priority = 0;

    /**
     * Framework-required constructor for JPA.
     */
    protected RPerkRequirement() {}

    /**
     * Creates a new perk requirement.
     *
     * @param perk the perk this requirement applies to
     * @param requirementType the type of requirement (e.g., "LEVEL", "CURRENCY", "QUEST")
     * @param requirementValue the value/condition for the requirement
     * @param descriptionKey the localization key for the requirement description
     */
    public RPerkRequirement(
        final @NotNull RPerk perk,
        final @NotNull String requirementType,
        final @NotNull String requirementValue,
        final @NotNull String descriptionKey
    ) {
        this.perk = Objects.requireNonNull(perk, "perk cannot be null");
        this.requirementType = Objects.requireNonNull(requirementType, "requirementType cannot be null");
        this.requirementValue = Objects.requireNonNull(requirementValue, "requirementValue cannot be null");
        this.descriptionKey = Objects.requireNonNull(descriptionKey, "descriptionKey cannot be null");
    }

    public @NotNull RPerk getPerk() {
        return this.perk;
    }

    public void setPerk(final @NotNull RPerk perk) {
        this.perk = Objects.requireNonNull(perk, "perk cannot be null");
    }

    public @NotNull String getRequirementType() {
        return this.requirementType;
    }

    public void setRequirementType(final @NotNull String requirementType) {
        this.requirementType = Objects.requireNonNull(requirementType, "requirementType cannot be null");
    }

    public @NotNull String getRequirementValue() {
        return this.requirementValue;
    }

    public void setRequirementValue(final @NotNull String requirementValue) {
        this.requirementValue = Objects.requireNonNull(requirementValue, "requirementValue cannot be null");
    }

    public @NotNull String getDescriptionKey() {
        return this.descriptionKey;
    }

    public void setDescriptionKey(final @NotNull String descriptionKey) {
        this.descriptionKey = Objects.requireNonNull(descriptionKey, "descriptionKey cannot be null");
    }

    public boolean isOptional() {
        return this.optional;
    }

    public void setOptional(final boolean optional) {
        this.optional = optional;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(final int priority) {
        this.priority = priority;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RPerkRequirement other)) return false;
        return Objects.equals(this.perk, other.perk) && 
               Objects.equals(this.requirementType, other.requirementType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.perk, this.requirementType);
    }

    @Override
    public String toString() {
        return "RPerkRequirement[perk=%s, type=%s, optional=%b]"
            .formatted(perk != null ? perk.getIdentifier() : "null", requirementType, optional);
    }
}