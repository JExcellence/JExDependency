/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rdq.config.utility.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entity representing a quest category.
 *
 * <p>Quest categories group related quests together for organization and navigation.
 * Each category has a unique identifier, display information, and contains multiple quests.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
@Getter
@Setter
@Entity
@Table(
        name = "rdq_quest_category",
        uniqueConstraints = @UniqueConstraint(columnNames = "identifier"),
        indexes = {
                @Index(name = "idx_quest_category_identifier", columnList = "identifier"),
                @Index(name = "idx_quest_category_enabled", columnList = "enabled"),
                @Index(name = "idx_quest_category_display_order", columnList = "display_order")
        }
)
/**
 * Represents the QuestCategory API type.
 */
public class QuestCategory extends BaseEntity {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * Unique identifier for this category (e.g., "combat", "mining").
     */
    @Column(name = "identifier", nullable = false, unique = true, length = 64)
    private String identifier;
    
    /**
     * The icon representing this category in the UI.
     * Contains material, display name key, description key, and visual properties.
     */
    @Convert(converter = IconSectionConverter.class)
    @Column(name = "icon", nullable = false, columnDefinition = "LONGTEXT")
    private IconSection icon;
    
    /**
     * Display order for sorting categories in GUIs.
     * Lower values appear first.
     */
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;
    
    /**
     * Whether this category is enabled and visible to players.
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
    
    /**
     * Quests belonging to this category.
     */
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Quest> quests = new ArrayList<>();
    
    /**
     * Protected no-argument constructor for JPA.
     */
    protected QuestCategory() {
    }
    
    /**
     * Constructs a new quest category.
     *
     * @param identifier the unique identifier
     * @param icon       the icon section with display information
     */
    public QuestCategory(
            @NotNull final String identifier,
            @NotNull final IconSection icon
    ) {
        this.identifier = identifier;
        this.icon = icon;
    }
    
    /**
     * Factory method to create a new quest category.
     *
     * @param identifier the unique identifier
     * @param icon       the icon section with display information
     * @return a new quest category instance
     */
    public static QuestCategory create(
            @NotNull final String identifier,
            @NotNull final IconSection icon
    ) {
        return new QuestCategory(identifier, icon);
    }
    
    /**
     * Adds a quest to this category.
     *
     * @param quest the quest to add
     */
    public void addQuest(@NotNull final Quest quest) {
        quests.add(quest);
        quest.setCategory(this);
    }
    
    /**
     * Removes a quest from this category.
     *
     * @param quest the quest to remove
     */
    public void removeQuest(@NotNull final Quest quest) {
        quests.remove(quest);
        quest.setCategory(null);
    }
    
    /**
     * Executes equals.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuestCategory that)) return false;
        
        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }
        
        return identifier != null && identifier.equals(that.identifier);
    }
    
    /**
     * Returns whether hCode.
     */
    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }
        
        return Objects.hash(identifier);
    }
    
    /**
     * Executes toString.
     */
    @Override
    public String toString() {
        return "QuestCategory{" +
                "id=" + getId() +
                ", identifier='" + identifier + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
