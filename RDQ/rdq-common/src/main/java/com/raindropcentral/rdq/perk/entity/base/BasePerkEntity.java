package com.raindropcentral.rdq.perk.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "perk")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "perk_type", discriminatorType = DiscriminatorType.STRING)
public abstract class BasePerkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "perk_id", nullable = false, unique = true)
    private String perkId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "description")
    private String description;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected BasePerkEntity() {
    }

    public BasePerkEntity(
        @NotNull String perkId,
        @NotNull String displayName,
        @NotNull String category,
        boolean enabled,
        int priority
    ) {
        this.perkId = perkId;
        this.displayName = displayName;
        this.category = category;
        this.enabled = enabled;
        this.priority = priority;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotNull String getPerkId() {
        return perkId;
    }

    public void setPerkId(@NotNull String perkId) {
        this.perkId = perkId;
    }

    public @NotNull String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(@NotNull String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public @NotNull String getCategory() {
        return category;
    }

    public void setCategory(@NotNull String category) {
        this.category = category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public @NotNull LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NotNull LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public @NotNull LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(@NotNull LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
