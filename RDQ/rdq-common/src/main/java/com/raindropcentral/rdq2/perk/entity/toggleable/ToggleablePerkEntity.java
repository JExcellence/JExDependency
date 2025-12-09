package com.raindropcentral.rdq2.perk.entity.toggleable;

import com.raindropcentral.rdq2.perk.entity.base.BasePerkEntity;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

@Entity
@Table(name = "perk_toggleable")
@PrimaryKeyJoinColumn(name = "perk_id")
@DiscriminatorValue("TOGGLEABLE")
public class ToggleablePerkEntity extends BasePerkEntity {

    @Column(name = "cooldown_seconds")
    private Long cooldownSeconds;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    protected ToggleablePerkEntity() {
    }

    public ToggleablePerkEntity(
        @NotNull String perkId,
        @NotNull String displayName,
        @NotNull String category,
        boolean enabled,
        int priority
    ) {
        super(perkId, displayName, category, enabled, priority);
    }

    public Long getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(Long cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
