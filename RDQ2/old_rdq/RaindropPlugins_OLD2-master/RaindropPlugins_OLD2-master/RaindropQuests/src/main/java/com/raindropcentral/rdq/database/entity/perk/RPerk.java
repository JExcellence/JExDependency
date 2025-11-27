package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.config.perks.PerkSection;
import com.raindropcentral.rdq.database.converter.PerkSectionConverter;
import com.raindropcentral.rdq.type.EPerkType;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a perk in the RaindropQuests system.
 * <p>
 * This class defines the structure and persistence mapping for perks, including
 * identifier, configuration, type, and database-stored properties. The entity
 * is focused on data persistence and basic queries, with business logic
 * delegated to service classes.
 * </p>
 *
 * @author JExcellence
 * @version 3.0.0
 * @since TBD
 */
@Entity
@Table(name = "r_perk")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "perk_type_discriminator", discriminatorType = DiscriminatorType.STRING)
public abstract class RPerk extends AbstractEntity {
    
    /**
     * Unique string identifier for the perk.
     */
    @Column(name = "identifier", unique = true, nullable = false)
    private String identifier;
    
    /**
     * Display name key for localization.
     */
    @Column(name = "display_name_key", nullable = false)
    private String displayNameKey;
    
    /**
     * Description key for localization.
     */
    @Column(name = "description_key", nullable = false)
    private String descriptionKey;
    
    /**
     * Complete perk configuration section.
     */
    @Column(name = "perk_config", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = PerkSectionConverter.class)
    private PerkSection perkSection;
    
    /**
     * The type of this perk, determining its activation behavior.
     */
    @Column(name = "perk_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EPerkType perkType;
    
    /**
     * Whether this perk is globally enabled and available for use.
     */
    @Column(name = "is_enabled")
    private boolean isEnabled = true;
    
    /**
     * Priority/weight for perk ordering and conflict resolution.
     */
    @Column(name = "priority")
    private int priority = 0;
    
    /**
     * Maximum number of players that can have this perk active simultaneously.
     * Null for unlimited.
     */
    @Column(name = "max_concurrent_users")
    private Integer maxConcurrentUsers;
    
    /**
     * Minimum permission level required to use this perk.
     */
    @Column(name = "required_permission")
    private String requiredPermission;
    
    /**
     * Custom properties for perk-specific configuration stored as JSON.
     */
    @Column(name = "custom_properties", columnDefinition = "LONGTEXT")
    private String customProperties;
    
    /**
     * All player-perk associations for this perk.
     * This represents all players who own this perk.
     */
    @OneToMany(mappedBy = "perk", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<RPlayerPerk> playerPerks = new HashSet<>();
    
    /**
     *
     * Protected no-argument constructor for JPA.
     */
    protected RPerk() {}
    
    /**
     * Legacy constructor for compatibility.
     */
    public RPerk(
        final @NotNull String identifier,
        final @NotNull PerkSection perkSection,
        final @NotNull EPerkType perkType
    ) {
        this.identifier = identifier;
        this.displayNameKey = identifier + ".name";
        this.descriptionKey = identifier + ".description";
        this.perkSection = perkSection;
        this.perkType = perkType;
    }
    
    // Getters and Setters
    public String getIdentifier() {
        return this.identifier;
    }
    
    public String getDisplayNameKey() {
        return this.displayNameKey;
    }
    
    public String getDescriptionKey() {
        return this.descriptionKey;
    }
    
    public PerkSection getPerkSection() {
        return this.perkSection;
    }
    
    public EPerkType getPerkType() {
        return this.perkType;
    }
    
    public boolean isEnabled() {
        return this.isEnabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }
    
    public int getPriority() {
        return this.priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public Integer getMaxConcurrentUsers() {
        return this.maxConcurrentUsers;
    }
    
    public void setMaxConcurrentUsers(Integer maxConcurrentUsers) {
        this.maxConcurrentUsers = maxConcurrentUsers;
    }
    
    public String getRequiredPermission() {
        return this.requiredPermission;
    }
    
    public void setRequiredPermission(String requiredPermission) {
        this.requiredPermission = requiredPermission;
    }
    
    public String getCustomProperties() {
        return this.customProperties;
    }
    
    public void setCustomProperties(String customProperties) {
        this.customProperties = customProperties;
    }
    
    public Set<RPlayerPerk> getPlayerPerks() {
        return this.playerPerks;
    }
    
    /**
     * Performs the perk-specific activation logic.
     * This method should contain the actual effect implementation.
     *
     * @return true if activation was successful, false otherwise
     */
    public abstract boolean performActivation();
    
    /**
     * Performs the perk-specific deactivation logic.
     * This method should contain the cleanup implementation.
     *
     * @return true if deactivation was successful, false otherwise
     */
    public abstract boolean performDeactivation();
    
    /**
     * Checks if the perk can currently be activated.
     * This method should contain perk-specific validation logic.
     *
     * @return true if activation is possible, false otherwise
     */
    public abstract boolean canPerformActivation();
    
    /**
     * Triggers the perk effect for event-based and instant-use perks.
     * This method should contain the immediate effect implementation.
     */
    public abstract void performTrigger();
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof final RPerk rPerk)) return false;
        return identifier.equals(rPerk.identifier);
    }
    
    @Override
    public int hashCode() {
        return identifier.hashCode();
    }
    
    @Override
    public String toString() {
        return "RPerk{" +
               "identifier='" + identifier + '\'' +
               ", perkType=" + perkType +
               ", enabled=" + isEnabled +
               '}';
    }
}