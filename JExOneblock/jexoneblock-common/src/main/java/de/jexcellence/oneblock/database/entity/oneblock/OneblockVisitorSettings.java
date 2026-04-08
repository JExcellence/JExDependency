package de.jexcellence.oneblock.database.entity.oneblock;

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Entity
@Table(name = "oneblock_visitor_settings")
@Getter
@Setter
public class OneblockVisitorSettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "island_id", referencedColumnName = "id", nullable = false)
    private OneblockIsland island;

    @Column(name = "can_visit", nullable = false)
    private boolean canVisit;

    @Column(name = "can_interact_with_blocks", nullable = false)
    private boolean canInteractWithBlocks;

    @Column(name = "can_interact_with_entities", nullable = false)
    private boolean canInteractWithEntities;

    @Column(name = "can_use_items", nullable = false)
    private boolean canUseItems;

    @Column(name = "can_place_blocks", nullable = false)
    private boolean canPlaceBlocks;

    @Column(name = "can_break_blocks", nullable = false)
    private boolean canBreakBlocks;

    @Column(name = "can_open_chests", nullable = false)
    private boolean canOpenChests;

    @Column(name = "can_use_furnaces", nullable = false)
    private boolean canUseFurnaces;

    @Column(name = "can_use_crafting_tables", nullable = false)
    private boolean canUseCraftingTables;

    @Column(name = "can_use_redstone", nullable = false)
    private boolean canUseRedstone;

    @Column(name = "can_use_buttons_and_levers", nullable = false)
    private boolean canUseButtonsAndLevers;

    @Column(name = "can_hurt_animals", nullable = false)
    private boolean canHurtAnimals;

    @Column(name = "can_breed_animals", nullable = false)
    private boolean canBreedAnimals;

    @Column(name = "can_tame_animals", nullable = false)
    private boolean canTameAnimals;

    @Column(name = "can_harvest_crops", nullable = false)
    private boolean canHarvestCrops;

    @Column(name = "can_plant_crops", nullable = false)
    private boolean canPlantCrops;

    @Column(name = "can_use_bone_meal", nullable = false)
    private boolean canUseBoneMeal;

    @Column(name = "can_use_anvils", nullable = false)
    private boolean canUseAnvils;

    @Column(name = "can_use_enchanting_tables", nullable = false)
    private boolean canUseEnchantingTables;

    @Column(name = "can_use_brewing_stands", nullable = false)
    private boolean canUseBrewingStands;

    @Column(name = "can_pickup_items", nullable = false)
    private boolean canPickupItems;

    @Column(name = "can_drop_items", nullable = false)
    private boolean canDropItems;

    protected OneblockVisitorSettings() {}

    public OneblockVisitorSettings(@NotNull OneblockIsland island) {
        this.island = island;
        this.canVisit = true;
        // Set all other permissions to false by default
        this.canInteractWithBlocks = false;
        this.canInteractWithEntities = false;
        this.canUseItems = false;
        this.canPlaceBlocks = false;
        this.canBreakBlocks = false;
        this.canOpenChests = false;
        this.canUseFurnaces = false;
        this.canUseCraftingTables = false;
        this.canUseRedstone = false;
        this.canUseButtonsAndLevers = false;
        this.canHurtAnimals = false;
        this.canBreedAnimals = false;
        this.canTameAnimals = false;
        this.canHarvestCrops = false;
        this.canPlantCrops = false;
        this.canUseBoneMeal = false;
        this.canUseAnvils = false;
        this.canUseEnchantingTables = false;
        this.canUseBrewingStands = false;
        this.canPickupItems = false;
        this.canDropItems = false;
    }

    public void setAllPermissions(boolean allowed) {
        this.canInteractWithBlocks = allowed;
        this.canInteractWithEntities = allowed;
        this.canUseItems = allowed;
        this.canPlaceBlocks = allowed;
        this.canBreakBlocks = allowed;
        this.canOpenChests = allowed;
        this.canUseFurnaces = allowed;
        this.canUseCraftingTables = allowed;
        this.canUseRedstone = allowed;
        this.canUseButtonsAndLevers = allowed;
        this.canHurtAnimals = allowed;
        this.canBreedAnimals = allowed;
        this.canTameAnimals = allowed;
        this.canHarvestCrops = allowed;
        this.canPlantCrops = allowed;
        this.canUseBoneMeal = allowed;
        this.canUseAnvils = allowed;
        this.canUseEnchantingTables = allowed;
        this.canUseBrewingStands = allowed;
        this.canPickupItems = allowed;
        this.canDropItems = allowed;
    }

    public void setBasicPermissions() {
        this.canInteractWithBlocks = true;
        this.canInteractWithEntities = true;
        this.canUseItems = true;
        this.canUseCraftingTables = true;
        this.canUseButtonsAndLevers = true;
        this.canPickupItems = true;
        this.canDropItems = true;
    }

    public void setTrustedPermissions() {
        setBasicPermissions();
        this.canOpenChests = true;
        this.canUseFurnaces = true;
        this.canUseRedstone = true;
        this.canHarvestCrops = true;
        this.canPlantCrops = true;
        this.canUseBoneMeal = true;
        this.canBreedAnimals = true;
    }
}