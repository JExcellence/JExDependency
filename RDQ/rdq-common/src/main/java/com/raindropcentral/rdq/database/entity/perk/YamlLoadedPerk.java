package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.type.EPerkType;
import org.jetbrains.annotations.NotNull;

/**
 * Concrete implementation of RPerk for perks loaded from YAML configuration files.
 * This class provides default implementations for abstract methods since YAML-loaded perks
 * are processed through the PerkRegistry system.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class YamlLoadedPerk extends RPerk {

    /**
     * Creates a new YAML-loaded perk.
     *
     * @param identifier the unique identifier
     * @param perkSection the configuration section
     * @param perkType the perk type
     */
    public YamlLoadedPerk(@NotNull String identifier, @NotNull PerkSection perkSection, @NotNull EPerkType perkType) {
        super(identifier, perkSection, perkType);
    }

    @Override
    public boolean performActivation() {
        // YAML-loaded perks use the registry system
        return false;
    }

    @Override
    public boolean performDeactivation() {
        // YAML-loaded perks use the registry system
        return false;
    }

    @Override
    public boolean canPerformActivation() {
        // YAML-loaded perks use the registry system
        return false;
    }

    @Override
    public void performTrigger() {
        // YAML-loaded perks use the registry system
    }
}