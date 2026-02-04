package com.raindropcentral.rdq.database.entity.requirement;

import com.raindropcentral.rdq.config.utility.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rdq.database.converter.RequirementConverter;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.Requirement;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Entity representing a requirement in the RaindropQuests system.
 * <p>
 * This entity encapsulates an {@link AbstractRequirement} from RPlatform and its visual icon,
 * providing convenience methods for requirement evaluation, progress calculation, and resource consumption.
 * </p>
 */
@Entity
@Table(name = "r_requirement")
@Getter
@Setter
public class BaseRequirement extends BaseEntity {

    @Column(name = "requirement_data", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = RequirementConverter.class)
    private AbstractRequirement requirement;

    /**
     * Optional description for this requirement.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Convert(converter = IconSectionConverter.class)
    @Column(name = "requirement_icon", nullable = false, columnDefinition = "LONGTEXT")
    private IconSection icon;

    /**
     * Whether this requirement is currently active.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Optional category for grouping requirements.
     */
    @Column(name = "category", length = 100)
    private String category;

    protected BaseRequirement() {
    }

    public BaseRequirement(
            @NotNull AbstractRequirement requirement,
            @NotNull IconSection icon
    ) {
        this.requirement = requirement;
        this.description = requirement.getDescriptionKey();
        this.icon = icon;
    }

    public boolean isMet(@NotNull Player player) {
        return requirement.isMet(player);
    }

    public double calculateProgress(@NotNull Player player) {
        return requirement.calculateProgress(player);
    }

    public void consume(@NotNull Player player) {
        requirement.consume(player);
    }

    public String getTypeId() {
        return requirement.getTypeId();
    }
}
