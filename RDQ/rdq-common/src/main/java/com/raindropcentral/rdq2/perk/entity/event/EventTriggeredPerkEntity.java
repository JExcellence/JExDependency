package com.raindropcentral.rdq2.perk.entity.event;

import com.raindropcentral.rdq2.perk.entity.base.BasePerkEntity;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

@Entity
@Table(name = "perk_event_triggered")
@PrimaryKeyJoinColumn(name = "perk_id")
@DiscriminatorValue("EVENT_TRIGGERED")
public class EventTriggeredPerkEntity extends BasePerkEntity {

    @Column(name = "trigger_event", nullable = false)
    private String triggerEvent;

    @Column(name = "cooldown_seconds")
    private Long cooldownSeconds;

    protected EventTriggeredPerkEntity() {
    }

    public EventTriggeredPerkEntity(
        @NotNull String perkId,
        @NotNull String displayName,
        @NotNull String category,
        boolean enabled,
        int priority,
        @NotNull String triggerEvent
    ) {
        super(perkId, displayName, category, enabled, priority);
        this.triggerEvent = triggerEvent;
    }

    public @NotNull String getTriggerEvent() {
        return triggerEvent;
    }

    public void setTriggerEvent(@NotNull String triggerEvent) {
        this.triggerEvent = triggerEvent;
    }

    public Long getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(Long cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }
}
