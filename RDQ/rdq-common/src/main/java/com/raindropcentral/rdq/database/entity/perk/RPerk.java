package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.database.converter.PerkSectionConverter;
import com.raindropcentral.rdq.type.EPerkType;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "r_perk")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "perk_type_discriminator", discriminatorType = DiscriminatorType.STRING)
public abstract class RPerk extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "identifier", unique = true, nullable = false)
    private String identifier;

    @Column(name = "display_name_key", nullable = false)
    private String displayNameKey;

    @Column(name = "description_key", nullable = false)
    private String descriptionKey;

    @Column(name = "perk_config", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = PerkSectionConverter.class)
    private PerkSection perkSection;

    @Column(name = "perk_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EPerkType perkType;

    @Column(name = "is_enabled")
    private boolean isEnabled = true;

    @Column(name = "priority")
    private int priority = 0;

    @Column(name = "max_concurrent_users")
    private Integer maxConcurrentUsers;

    @Column(name = "required_permission")
    private String requiredPermission;

    @Column(name = "custom_properties", columnDefinition = "LONGTEXT")
    private String customProperties;

    @OneToMany(mappedBy = "perk", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<RPlayerPerk> playerPerks = new HashSet<>();

    protected RPerk() {}

    public RPerk(final @NotNull String identifier, final @NotNull PerkSection perkSection, final @NotNull EPerkType perkType) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
        this.displayNameKey = identifier + ".name";
        this.descriptionKey = identifier + ".description";
        this.perkSection = Objects.requireNonNull(perkSection, "perkSection cannot be null");
        this.perkType = Objects.requireNonNull(perkType, "perkType cannot be null");
    }

    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    public @NotNull String getDisplayNameKey() {
        return this.displayNameKey;
    }

    public @NotNull String getDescriptionKey() {
        return this.descriptionKey;
    }

    public @NotNull PerkSection getPerkSection() {
        return this.perkSection;
    }

    public @NotNull EPerkType getPerkType() {
        return this.perkType;
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public void setEnabled(final boolean enabled) {
        this.isEnabled = enabled;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(final int priority) {
        this.priority = priority;
    }

    public @Nullable Integer getMaxConcurrentUsers() {
        return this.maxConcurrentUsers;
    }

    public void setMaxConcurrentUsers(final @Nullable Integer maxConcurrentUsers) {
        this.maxConcurrentUsers = maxConcurrentUsers;
    }

    public @Nullable String getRequiredPermission() {
        return this.requiredPermission;
    }

    public void setRequiredPermission(final @Nullable String requiredPermission) {
        this.requiredPermission = requiredPermission;
    }

    public @Nullable String getCustomProperties() {
        return this.customProperties;
    }

    public void setCustomProperties(final @Nullable String customProperties) {
        this.customProperties = customProperties;
    }

    public @NotNull Set<RPlayerPerk> getPlayerPerks() {
        return Collections.unmodifiableSet(this.playerPerks);
    }

    public abstract boolean performActivation();

    public abstract boolean performDeactivation();

    public abstract boolean canPerformActivation();

    public abstract void performTrigger();

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof final RPerk other)) return false;
        return this.identifier.equals(other.identifier);
    }

    @Override
    public int hashCode() {
        return this.identifier.hashCode();
    }

    @Override
    public String toString() {
        return "RPerk[identifier=%s, type=%s, enabled=%b]".formatted(identifier, perkType, isEnabled);
    }
}