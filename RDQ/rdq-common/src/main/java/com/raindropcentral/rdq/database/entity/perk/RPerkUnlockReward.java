package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rdq.database.converter.RewardConverter;
import com.raindropcentral.rdq.reward.AbstractReward;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Objects;

/**
 * Represents an association between a {@link RPerk} and the {@link AbstractReward}
 * that is granted when the perk is unlocked. Each association also
 * carries metadata for how the reward should be displayed within perk
 * related menus and progress trackers.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Entity
@Table(name = "r_perk_unlock_reward")
public final class RPerkUnlockReward extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "perk_id", nullable = false)
    private RPerk perk;

    @Column(name = "reward", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = RewardConverter.class)
    private AbstractReward reward;

    @Column(name = "icon", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = IconSectionConverter.class)
    private IconSection icon;

    @Column(name = "display_order")
    private int displayOrder = 0;

    @Version
    @Column(name = "version")
    private int version;

    protected RPerkUnlockReward() {}

    /**
     * Creates a new unlock reward link between a perk and a reward.
     *
     * @param perk         the owning perk, or {@code null} when the relationship will be assigned later
     * @param reward       the reward granted when the perk is unlocked
     * @param icon         the icon describing the reward in user interfaces
     * @throws NullPointerException if {@code reward} or {@code icon} is {@code null}
     */
    public RPerkUnlockReward(final @Nullable RPerk perk, final @NotNull AbstractReward reward,
                             final @NotNull IconSection icon) {
        this.perk = perk;
        this.reward = Objects.requireNonNull(reward, "reward cannot be null");
        this.icon = Objects.requireNonNull(icon, "icon cannot be null");
        if (perk != null) {
            perk.addUnlockReward(this);
        }
    }

    /**
     * Retrieves the perk that owns this unlock reward.
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
            this.perk.getUnlockRewards();
        }
        this.perk = perk;
        if (perk != null) {
            perk.addUnlockReward(this);
        }
    }

    /**
     * Provides access to the reward that is granted.
     *
     * @return the reward definition backing this link
     */
    public @NotNull AbstractReward getReward() {
        return this.reward;
    }

    /**
     * Replaces the underlying reward definition.
     *
     * @param reward the new reward definition
     * @throws NullPointerException if {@code reward} is {@code null}
     */
    public void setReward(final @NotNull AbstractReward reward) {
        this.reward = Objects.requireNonNull(reward, "reward cannot be null");
    }

    /**
     * Retrieves the icon describing this reward for display purposes.
     *
     * @return the icon metadata used in menus and progress views
     */
    public @NotNull IconSection getIcon() {
        return this.icon;
    }

    /**
     * Updates the icon metadata used when presenting the reward.
     *
     * @param icon the new icon metadata to present
     * @throws NullPointerException if {@code icon} is {@code null}
     */
    public void setIcon(final @NotNull IconSection icon) {
        this.icon = Objects.requireNonNull(icon, "icon cannot be null");
    }

    /**
     * Obtains the relative ordering for displaying this reward.
     *
     * @return the zero-based display order
     */
    public int getDisplayOrder() {
        return this.displayOrder;
    }

    /**
     * Sets the relative ordering for displaying this reward.
     *
     * @param displayOrder the zero-based display position
     */
    public void setDisplayOrder(final int displayOrder) {
        this.displayOrder = displayOrder;
    }

    /**
     * Applies the reward to the provided player.
     *
     * @param player the player to receive the reward
     * @throws NullPointerException if {@code player} is {@code null}
     */
    public void apply(final @NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        this.reward.apply(player);
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
     * Compares this reward with another object for equality based on the
     * persisted identifier when available, otherwise comparing core fields.
     *
     * @param obj the object to compare with
     * @return {@code true} when the objects represent the same reward link
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RPerkUnlockReward other)) return false;
        if (getId() != null && other.getId() != null) {
            return getId().equals(other.getId());
        }
        return Objects.equals(this.reward, other.reward) &&
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
        return Objects.hash(this.reward, this.perk, this.displayOrder);
    }

    /**
     * Provides a concise textual representation useful for logging and debugging.
     *
     * @return a formatted string summarizing the reward link
     */
    @Override
    public String toString() {
        return "RPerkUnlockReward[id=%d, perk=%s, reward=%s, displayOrder=%d]"
                .formatted(getId(), perk != null ? perk.getIdentifier() : "null",
                          reward != null ? reward.getType().name() : "null", displayOrder);
    }
}