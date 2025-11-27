package com.raindropcentral.rdq.manager;

import com.raindropcentral.rdq.manager.perk.DefaultPerkManager;
import com.raindropcentral.rdq.manager.quest.QuestManager;
import org.jetbrains.annotations.NotNull;

public abstract class RDQManager {

    private final String edition;

    protected RDQManager(@NotNull String edition) {
        this.edition = edition;
    }

    public abstract @NotNull QuestManager getQuestManager();
    public abstract @NotNull DefaultPerkManager getPerkManager();
    
    public @NotNull String getEdition() {
        return edition;
    }

    public abstract boolean isPremium();
    public abstract void initialize();
    public abstract void shutdown();
}