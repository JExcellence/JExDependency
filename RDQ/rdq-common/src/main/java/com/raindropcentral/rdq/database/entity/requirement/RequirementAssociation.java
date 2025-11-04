package com.raindropcentral.rdq.database.entity.requirement;

import com.raindropcentral.rdq.config.item.IconSection; import com.raindropcentral.rdq.database.converter.IconSectionConverter; import com.raindropcentral.rdq.database.entity.rank.RRequirement; import de.jexcellence.hibernate.entity.AbstractEntity; import jakarta.persistence.Column; import jakarta.persistence.Convert; import jakarta.persistence.MappedSuperclass; import jakarta.persistence.Version; import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@MappedSuperclass
public abstract class RequirementAssociation extends AbstractEntity {

    @Column(name = "display_order")
    private int displayOrder = 0;

    @Version
    @Column(name = "version")
    private int version;

    @Column(name = "icon", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = IconSectionConverter.class)
    private IconSection icon;

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public int getVersion() {
        return version;
    }

    public @NotNull IconSection getIcon() {
        return icon;
    }

    public void setIcon(@NotNull IconSection icon) {
        this.icon = Objects.requireNonNull(icon, "icon");
    }

    public abstract @NotNull RRequirement getRequirement();

    public abstract void setRequirement(@NotNull RRequirement requirement);
}