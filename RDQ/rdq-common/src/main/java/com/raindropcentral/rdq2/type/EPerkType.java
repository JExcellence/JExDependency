package com.raindropcentral.rdq2.type;


public enum EPerkType {

    TOGGLEABLE_PASSIVE(false, true, false),
    EVENT_TRIGGERED(true, false, true),
    INSTANT_USE(true, false, false),
    DURATION_BASED(true, false, true);
    
    private final boolean hasCooldown;
    private final boolean isToggleable;
    private final boolean isEventBased;
    
    EPerkType(boolean hasCooldown, boolean isToggleable, boolean isEventBased) {
        this.hasCooldown = hasCooldown;
        this.isToggleable = isToggleable;
        this.isEventBased = isEventBased;
    }
    
    public boolean hasCooldown() {
        return hasCooldown;
    }

    public boolean isToggleable() {
        return isToggleable;
    }

    public boolean isEventBased() {
        return isEventBased;
    }

    public String getDescription() {
        return switch (this) {
            case TOGGLEABLE_PASSIVE -> "Toggleable passive effect without cooldown";
            case EVENT_TRIGGERED -> "Automatically triggered by events with cooldown";
            case INSTANT_USE -> "Immediate effect with cooldown";
            case DURATION_BASED -> "Temporary effect with duration and cooldown";
        };
    }
}