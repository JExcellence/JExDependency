package de.jexcellence.quests.database.entity;

import de.jexcellence.jehibernate.entity.base.LongIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Per-player row for JExQuests. Stores JExQuests-specific toggles and
 * persistence; the canonical player identity record lives in JExCore as
 * {@code CorePlayer} — this row references it by UUID rather than
 * duplicating name / first-seen / last-seen.
 */
@Entity
@Table(name = "jexquests_player")
public class QuestsPlayer extends LongIdEntity {

    @Column(name = "unique_id", unique = true, nullable = false)
    private UUID uniqueId;

    @Column(name = "quest_sidebar_enabled", nullable = false)
    private boolean questSidebarEnabled = true;

    @Column(name = "perk_sidebar_enabled", nullable = false)
    private boolean perkSidebarEnabled;

    @Column(name = "tracked_quest_identifier", length = 64)
    private String trackedQuestIdentifier;

    @Column(name = "active_rank_tree", length = 64)
    private String activeRankTree;

    protected QuestsPlayer() {
    }

    public QuestsPlayer(@NotNull UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public @NotNull UUID getUniqueId() { return this.uniqueId; }
    public boolean isQuestSidebarEnabled() { return this.questSidebarEnabled; }
    public void setQuestSidebarEnabled(boolean questSidebarEnabled) { this.questSidebarEnabled = questSidebarEnabled; }
    public boolean isPerkSidebarEnabled() { return this.perkSidebarEnabled; }
    public void setPerkSidebarEnabled(boolean perkSidebarEnabled) { this.perkSidebarEnabled = perkSidebarEnabled; }
    public String getTrackedQuestIdentifier() { return this.trackedQuestIdentifier; }
    public void setTrackedQuestIdentifier(String trackedQuestIdentifier) { this.trackedQuestIdentifier = trackedQuestIdentifier; }
    public String getActiveRankTree() { return this.activeRankTree; }
    public void setActiveRankTree(String activeRankTree) { this.activeRankTree = activeRankTree; }

    @Override
    public String toString() {
        return "QuestsPlayer[" + this.uniqueId + "]";
    }
}
