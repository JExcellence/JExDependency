package com.raindropcentral.rdq.database.entity.quest;

import com.raindropcentral.rplatform.database.converter.BasicMaterialConverter;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
public abstract class RQuest extends AbstractEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "identifier", unique = true, nullable = false)
    private String identifier;

    @Column(name = "initial_upgrade_level", nullable = false)
    private int initialUpgradeLevel;

    @Column(name = "maximum_upgrade_level", nullable = false)
    private int maximumUpgradeLevel;

    @Column(name = "showcase_item", nullable = false, columnDefinition = "LONGTEXT")
    @Convert(converter = BasicMaterialConverter.class)
    private Material showcase;

    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RQuestUpgrade> upgrades = new ArrayList<>();

    protected RQuest() {}

    public RQuest(@NotNull String identifier, int initialUpgradeLevel, int maximumUpgradeLevel) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
        this.initialUpgradeLevel = initialUpgradeLevel;
        this.maximumUpgradeLevel = maximumUpgradeLevel;
        this.showcase = this.initializeShowcase();
    }

    public @NotNull String getIdentifier() {
        return this.identifier;
    }

    public int getInitialUpgradeLevel() {
        return this.initialUpgradeLevel;
    }

    public int getMaximumUpgradeLevel() {
        return this.maximumUpgradeLevel;
    }

    public @NotNull Material getShowcase() {
        return this.showcase;
    }

    public @NotNull List<RQuestUpgrade> getUpgrades() {
        return Collections.unmodifiableList(this.upgrades);
    }

    public void addUpgrade(@NotNull RQuestUpgrade upgrade) {
        Objects.requireNonNull(upgrade, "upgrade cannot be null");
        if (!this.upgrades.contains(upgrade)) {
            this.upgrades.add(upgrade);
        }
    }

    public boolean removeUpgrade(@NotNull RQuestUpgrade upgrade) {
        Objects.requireNonNull(upgrade, "upgrade cannot be null");
        return this.upgrades.remove(upgrade);
    }

    @NotNull
    protected abstract Material initializeShowcase();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RQuest other)) return false;
        return this.identifier.equals(other.identifier);
    }

    @Override
    public int hashCode() {
        return this.identifier.hashCode();
    }

    @Override
    public String toString() {
        return "RQuest[id=%d, identifier=%s, levels=%d-%d]"
                .formatted(getId(), identifier, initialUpgradeLevel, maximumUpgradeLevel);
    }
}