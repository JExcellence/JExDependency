package com.raindropcentral.rdq.utility.perk;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.config.perk.PerkSystemSection;
import com.raindropcentral.rdq.database.entity.perk.RPerk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class PerkSystemState {

    private final @NotNull Map<String, PerkSection> perkSections;
    private final @NotNull Map<String, RPerk> perks;
    private final @Nullable PerkSystemSection perkSystemSection;

    private PerkSystemState(@Nullable PerkSystemSection perkSystemSection,
                            @NotNull Map<String, PerkSection> perkSections,
                            @NotNull Map<String, RPerk> perks) {
        this.perkSystemSection = perkSystemSection;
        this.perkSections = perkSections;
        this.perks = perks;
    }

    static @NotNull PerkSystemState empty() {
        return new PerkSystemState(null, new HashMap<>(), new HashMap<>());
    }

    static @NotNull Builder builder() {
        return new Builder();
    }

    @Nullable PerkSystemSection perkSystemSection() {
        return perkSystemSection;
    }

    @NotNull Map<String, PerkSection> perkSections() {
        return perkSections;
    }

    @NotNull Map<String, RPerk> perks() {
        return perks;
    }

    static final class Builder {
        private PerkSystemSection perkSystemSection;
        private Map<String, PerkSection> perkSections = new HashMap<>();
        private Map<String, RPerk> perks = new HashMap<>();

        Builder perkSystemSection(PerkSystemSection section) {
            this.perkSystemSection = section;
            return this;
        }

        Builder perkSections(Map<String, PerkSection> sections) {
            this.perkSections = sections != null ? sections : new HashMap<>();
            return this;
        }

        Builder perks(Map<String, RPerk> map) {
            this.perks = map != null ? map : new HashMap<>();
            return this;
        }

        PerkSystemState build() {
            return new PerkSystemState(perkSystemSection, perkSections, perks);
        }
    }
}
