/*
package com.raindropcentral.rdq2.database.entity.rank;


import com.raindropcentral.rdq2.config.item.IconSection;
import com.raindropcentral.rdq2.database.converter.IconSectionConverter;
import com.raindropcentral.rdq2.database.converter.RequirementConverter;
import com.raindropcentral.rdq2.requirement.AbstractRequirement;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Objects;

*/
/**
 * Database wrapper that connects a persisted requirement definition with the icon shown in the
 * ranking UI.
 *
 * <p>The entity delegates all evaluation logic to the underlying {@link AbstractRequirement} while
 * exposing Bukkit facing helper methods for the surrounding services.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 *//*

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

    */
/**
     * Creates a requirement instance for JPA. This constructor is intentionally empty so the
     * persistence framework can hydrate all properties through reflection.
     *//*

    protected RRequirement() {}

    */
/**
     * Creates a new requirement entity using the supplied evaluation logic and icon metadata.
     *
     * @param requirement the delegated requirement implementation
     * @param icon the visual configuration presented to the player
     *//*

    public RRequirement(final @NotNull AbstractRequirement requirement, final @NotNull IconSection icon) {
        this.requirement = Objects.requireNonNull(requirement, "requirement cannot be null");
        this.icon = Objects.requireNonNull(icon, "icon cannot be null");
    }

    */
/**
     * Retrieves the delegated requirement implementation.
     *
     * @return the stored requirement
     *//*

    public @NotNull AbstractRequirement getRequirement() {
        return this.requirement;
    }

    */
/**
     * Updates the delegated requirement implementation.
     *
     * @param requirement the new requirement to persist
     *//*

    public void setRequirement(final @NotNull AbstractRequirement requirement) {
        this.requirement = Objects.requireNonNull(requirement, "requirement cannot be null");
    }

    */
/**
     * Provides the icon metadata used to showcase the requirement inside rank menus.
     *
     * @return the icon definition
     *//*

    public @NotNull IconSection getShowcase() {
        return this.icon;
    }

    */
/**
     * Stores the icon metadata that should represent the requirement.
     *
     * @param icon the icon configuration to set
     *//*

    public void setShowcase(final @NotNull IconSection icon) {
        this.icon = Objects.requireNonNull(icon, "icon cannot be null");
    }

    */
/**
     * Evaluates whether the supplied player satisfies the stored requirement.
     *
     * @param player the player to inspect
     * @return {@code true} when the requirement criteria are met
     *//*

    public boolean isMet(final @NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        return this.requirement.isMet(player);
    }

    */
/**
     * Calculates the player's completion progress for the stored requirement.
     *
     * @param player the player to inspect
     * @return the progress as a value between {@code 0.0} and {@code 1.0}
     *//*

    public double calculateProgress(final @NotNull Player player) {
        Objects.requireNonNull(player, "player cannot be null");
        return this.requirement.calculateProgress(player);
    }

    */
/**
     * Consumes any resources or state required for the requirement to be fulfilled.
     *
     * @param player the player whose state should be consumed
     *//*

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
}*/
