package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rdq.database.entity.rank.RRequirement;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Objects;

/**
 * Represents an association between a {@link RPerk} and the {@link RRequirement}
 * that must be satisfied before the perk can be unlocked. Each association also
 * carries metadata for how the requirement should be displayed within perk
 * related menus and progress trackers.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@Table(name = "r_perk_unlock_requirement")
public final class RPerkUnlockRequirement extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "perk_id", nullable = false)
    private RPerk perk;

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

    protected RPerkUnlockRequirement() {}

    /**
     * Creates a new unlock requirement link between a perk and a requirement.
     *
     * @param perk         the owning perk, or {@code null} when the relationship will be assigned later
     * @param requirement  the requirement players must fulfill to obtain the perk
     * @param icon         the icon describing the requirement in user interfaces
     * @throws NullPointerException if {@code requirement} or {@code icon} is {@code null}
     */
    public RPerkUnlockRequirement(final @Nullable RPerk perk, final @NotNull RRequirement requirement,
                                   final @NotNull IconSection icon) {
        this.perk = perk;
        this.requirement = Objects.requireNonNull(requirement, "requirement cannot be null");
        this.icon = Objects.requireNonNull(icon, "icon cannot be null");
        if (perk != null) {
            perk.addUnlockRequirement(this);
        }
    }

    /**
     * Retrieves the perk that owns this unlock requirement.
     *
     * @return the owning perk, or {@code null} when not yet attached
     */
    public @Nullable RPerk getPerk() {
        return this.perk;
    }

    /**
     * Updates the owning perk reference while keeping the bidirectional
     * relationship consistent.
     *
     * @param perk the new perk, or {@code null} to detach from the current perk
     */
    public void setPerk(final @Nullable RPerk perk) {
        if (this.perk != null && this.perk != perk) {
            this.perk.getUnlockRequirements();
        }
        this.perk = perk;
        if (perk != null) {
            perk.addUnlockRequirement(this);
        }
    }

    /**
     * Provides access to the requirement that must be satisfied.
     *
     * @return the requirement definition backing this link
     */
    public @NotNull RRequirement getRequirement() {
        return this.requirement;
    }

    /**
     * Replaces the underlying requirement definition.
     *
     * @param requirement the new requirement definition
     * @throws NullPointerException if {@code requirement} is {@code null}
     */
    public void setRequirement(final @NotNull RRequirement requirement) {
        this.requirement = Objects.requireNonNull(requirement, "requirement cannot be null");
    }

    /**
     * Retrieves the icon describing this requirement for display purposes.
     *
     * @return the icon metadata used in menus and progress views
     */
    public @NotNull IconSection getIcon() {
        return this.icon;
    }

    /**
     * Updates the icon metadata used when presenting the requirement.
     *
     * @param icon the new icon metadata to present
     * @throws NullPointerException if {@code icon} is {@code null}
     */
    public void setIcon(final @NotNull IconSection icon) {
        this.icon = Objects.requireNonNull(icon, "icon cannot be null");
    }

    /**
     * Obtains the relative ordering for displaying this requirement.
     *
     * @return the zero-based display order
     */
    public int getDisplayOrder() {
        return this.displayOrder;
    }

    /**
     * Sets the relative ordering for displaying this requirement.
     *
     * @param displayOrder the zero-based display position
     */
    public void setDisplayOrder(final int displayOrder) {
        this.displayOrder = displayOrder;
    }

    /**
     * Determines whether the requirement has been satisfied by the provided player.
     *
     * @param player the player being evaluated
     * @return {@code true} when the player meets the requirement
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public boolean isMet(final @NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        return this.requirement.isMet(player);
    }

    /**
     * Calculates the player's progress toward satisfying the requirement.
     *
     * @param player the player being evaluated
     * @return a value between {@code 0.0} and {@code 1.0} representing progress
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public double calculateProgress(final @NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        return this.requirement.calculateProgress(player);
    }

    /**
     * Consumes any required resources from the player after the requirement is met.
     *
     * @param player the player whose resources should be consumed
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public void consume(final @NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        this.requirement.consume(player);
    }

    /**
     * Exposes the entity version used for optimistic locking.
     *
     * @return the current persistence version
     */
    public int getVersion() {
        return this.version;
    }

    /**
     * Compares this requirement with another object for equality based on the
     * persisted identifier when available, otherwise comparing core fields.
     *
     * @param obj the object to compare with
     * @return {@code true} when the objects represent the same requirement link
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RPerkUnlockRequirement other)) return false;
        if (getId() != null && other.getId() != null) {
            return getId().equals(other.getId());
        }
        return Objects.equals(this.requirement, other.requirement) &&
               Objects.equals(this.perk, other.perk) &&
               this.displayOrder == other.displayOrder;
    }

    /**
     * Computes a hash code consistent with {@link #equals(Object)} using either
     * the persisted identifier or the core relationship fields.
     *
     * @return the computed hash code
     */
    @Override
    public int hashCode() {
        if (getId() != null) {
            return getId().hashCode();
        }
        return Objects.hash(this.requirement, this.perk, this.displayOrder);
    }

    /**
     * Provides a concise textual representation useful for logging and debugging.
     *
     * @return a formatted string summarizing the requirement link
     */
    @Override
    public String toString() {
        return "RPerkUnlockRequirement[id=%d, perk=%s, displayOrder=%d]"
                .formatted(getId(), perk != null ? perk.getIdentifier() : "null", displayOrder);
    }
}