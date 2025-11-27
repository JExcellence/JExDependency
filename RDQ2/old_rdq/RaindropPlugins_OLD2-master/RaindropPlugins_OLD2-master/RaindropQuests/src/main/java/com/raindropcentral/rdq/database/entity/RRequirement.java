package com.raindropcentral.rdq.database.entity;

import com.raindropcentral.rdq.config.utility.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rdq.database.converter.RequirementConverter;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.database.converter.BasicMaterialConverter;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Entity representing a requirement in the RaindropQuests system.
 * <p>
 * This entity encapsulates an {@link AbstractRequirement} and its visual icon material,
 * providing convenience methods for requirement evaluation, progress calculation, and resource consumption.
 * It is mapped as a JPA entity for persistence.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@Entity
@Table(name = "r_requirement")
public class RRequirement extends AbstractEntity {

    /**
     * The requirement data, persisted as a JSON string using {@link RequirementConverter}.
     */
    @Column(name = "requirement_data", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = RequirementConverter.class)
    private AbstractRequirement requirement;

    /**
     * The material used to visually represent this requirement in the UI.
     * Persisted using {@link BasicMaterialConverter}.
     */
    @Convert(converter = IconSectionConverter.class)
    @Column(name = "requirement_icon", nullable = false, columnDefinition = "LONGTEXT")
    private IconSection icon;

    /**
     * Protected no-argument constructor for JPA.
     */
    protected RRequirement() {
    }

    /**
     * Constructs a new {@code RRequirement} with the specified requirement and icon material.
     *
     * @param requirement the requirement logic to encapsulate
     * @param icon    the material used for visual representation
     */
    public RRequirement(
            final @NotNull AbstractRequirement requirement,
            final @NotNull IconSection icon
    ) {
        this.requirement = requirement;
        this.icon = icon;
    }

    /**
     * Gets the encapsulated requirement logic.
     *
     * @return the {@link AbstractRequirement} instance
     */
    public AbstractRequirement getRequirement() {
        return this.requirement;
    }

    /**
     * Gets the material used to visually represent this requirement.
     *
     * @return the icon {@link Material}
     */
    public IconSection getShowcase() {
        return this.icon;
    }

    /**
     * Convenience method to check if this requirement is met for a player.
     *
     * @param player The player to check against
     * @return {@code true} if the requirement is met, {@code false} otherwise
     */
    public boolean isMet(@NotNull Player player) {
        return this.requirement.isMet(player);
    }

    /**
     * Convenience method to calculate the progress for this requirement.
     *
     * @param player The player to calculate progress for
     * @return The progress value between 0.0 and 1.0
     */
    public double calculateProgress(@NotNull Player player) {
        return this.requirement.calculateProgress(player);
    }

    /**
     * Convenience method to consume resources for this requirement.
     *
     * @param player The player from whom to consume resources
     */
    public void consume(@NotNull Player player) {
        this.requirement.consume(player);
    }
}
