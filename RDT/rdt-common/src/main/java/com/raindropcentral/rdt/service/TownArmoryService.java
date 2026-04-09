/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdt.service;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.configs.ArmoryConfigSection;
import com.raindropcentral.rdt.database.entity.RDTPlayer;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import com.raindropcentral.rdt.items.RepairBlock;
import com.raindropcentral.rdt.items.SalvageBlock;
import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownProtections;
import org.bukkit.Material;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Maintains Armory chunk perk behavior such as free repair, salvaging, repair blocks, and
 * double-smelt fuel handling.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownArmoryService {

    private static final Map<Material, SalvageRecipe> SALVAGE_RECIPES = createSalvageRecipes();
    private static final Map<Material, RepairFamily> REPAIR_FAMILIES = createRepairFamilies();

    private final RDT plugin;

    /**
     * Creates the Armory chunk runtime service.
     *
     * @param plugin active plugin runtime
     */
    public TownArmoryService(final @NotNull RDT plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Returns whether free repair is unlocked for one Armory chunk.
     *
     * @param townChunk chunk to inspect
     * @return {@code true} when free repair is unlocked and enabled
     */
    public boolean isFreeRepairUnlocked(final @Nullable RTownChunk townChunk) {
        final var settings = this.plugin.getArmoryConfig().getFreeRepair();
        return this.isArmoryChunk(townChunk) && settings.enabled() && settings.isUnlocked(townChunk.getChunkLevel());
    }

    /**
     * Returns whether the salvage block is unlocked for one Armory chunk.
     *
     * @param townChunk chunk to inspect
     * @return {@code true} when the salvage block is unlocked and enabled
     */
    public boolean isSalvageBlockUnlocked(final @Nullable RTownChunk townChunk) {
        final var settings = this.plugin.getArmoryConfig().getSalvageBlock();
        return this.isArmoryChunk(townChunk) && settings.enabled() && settings.isUnlocked(townChunk.getChunkLevel());
    }

    /**
     * Returns whether the repair block is unlocked for one Armory chunk.
     *
     * @param townChunk chunk to inspect
     * @return {@code true} when the repair block is unlocked and enabled
     */
    public boolean isRepairBlockUnlocked(final @Nullable RTownChunk townChunk) {
        final var settings = this.plugin.getArmoryConfig().getRepairBlock();
        return this.isArmoryChunk(townChunk) && settings.enabled() && settings.isUnlocked(townChunk.getChunkLevel());
    }

    /**
     * Returns whether double smelting is unlocked for one Armory chunk.
     *
     * @param townChunk chunk to inspect
     * @return {@code true} when double smelting is unlocked and enabled
     */
    public boolean isDoubleSmeltUnlocked(final @Nullable RTownChunk townChunk) {
        final var settings = this.plugin.getArmoryConfig().getDoubleSmelt();
        return this.isArmoryChunk(townChunk) && settings.enabled() && settings.isUnlocked(townChunk.getChunkLevel());
    }

    /**
     * Returns whether double smelting is currently active for one Armory chunk.
     *
     * @param townChunk chunk to inspect
     * @return {@code true} when double smelting is unlocked and enabled for the chunk
     */
    public boolean isDoubleSmeltActive(final @Nullable RTownChunk townChunk) {
        if (!this.isDoubleSmeltUnlocked(townChunk)) {
            return false;
        }
        final boolean fallback = this.plugin.getArmoryConfig().getDoubleSmelt().enabledByDefault();
        return Objects.requireNonNull(townChunk, "townChunk").isArmoryDoubleSmeltEnabled(fallback);
    }

    /**
     * Returns the remaining free-repair cooldown for one player in one Armory chunk.
     *
     * @param player player to inspect
     * @param townChunk Armory chunk to inspect
     * @return remaining cooldown in milliseconds, or {@code 0} when ready
     */
    public long getFreeRepairCooldownRemainingMillis(
        final @NotNull Player player,
        final @NotNull RTownChunk townChunk
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(townChunk, "townChunk");
        if (!this.isFreeRepairUnlocked(townChunk)) {
            return 0L;
        }

        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        final RDTPlayer playerData = runtimeService == null ? null : runtimeService.getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return 0L;
        }

        final long cooldownMillis = this.plugin.getArmoryConfig().getFreeRepair().cooldownSeconds() * 1000L;
        if (cooldownMillis <= 0L) {
            return 0L;
        }

        final long lastUsedAt = playerData.getArmoryFreeRepairUsedAt(townChunk.getIdentifier());
        if (lastUsedAt <= 0L) {
            return 0L;
        }
        final long elapsedMillis = Math.max(0L, System.currentTimeMillis() - lastUsedAt);
        return Math.max(0L, cooldownMillis - elapsedMillis);
    }

    /**
     * Repairs every equipped damageable item for one player when the Armory chunk allows it.
     *
     * @param player player using free repair
     * @param townChunk Armory chunk being used
     * @return structured free-repair result
     */
    public @NotNull FreeRepairResult useFreeRepair(final @NotNull Player player, final @NotNull RTownChunk townChunk) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(townChunk, "townChunk");

        if (!this.isArmoryChunk(townChunk)) {
            return FreeRepairResult.invalidChunk();
        }
        if (!this.isFreeRepairUnlocked(townChunk)) {
            return FreeRepairResult.locked();
        }

        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        if (runtimeService == null) {
            return FreeRepairResult.failed();
        }
        if (!runtimeService.isPlayerAllowed(player, townChunk, TownProtections.ARMORY_USE)) {
            return FreeRepairResult.noPermission();
        }

        final long cooldownRemainingMillis = this.getFreeRepairCooldownRemainingMillis(player, townChunk);
        if (cooldownRemainingMillis > 0L) {
            return FreeRepairResult.coolingDown(cooldownRemainingMillis);
        }

        final PlayerInventory inventory = player.getInventory();
        int repairedItems = 0;
        repairedItems += this.repairInventorySlot(inventory.getHelmet(), inventory::setHelmet);
        repairedItems += this.repairInventorySlot(inventory.getChestplate(), inventory::setChestplate);
        repairedItems += this.repairInventorySlot(inventory.getLeggings(), inventory::setLeggings);
        repairedItems += this.repairInventorySlot(inventory.getBoots(), inventory::setBoots);
        repairedItems += this.repairInventorySlot(inventory.getItemInMainHand(), inventory::setItemInMainHand);
        repairedItems += this.repairInventorySlot(inventory.getItemInOffHand(), inventory::setItemInOffHand);
        if (repairedItems <= 0) {
            return FreeRepairResult.nothingToRepair();
        }

        final RDTPlayer playerData = runtimeService.getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return FreeRepairResult.failed();
        }
        playerData.setArmoryFreeRepairUsedAt(townChunk.getIdentifier(), System.currentTimeMillis());
        if (this.plugin.getPlayerRepository() != null) {
            this.plugin.getPlayerRepository().update(playerData);
        }
        return FreeRepairResult.success(
            repairedItems,
            this.plugin.getArmoryConfig().getFreeRepair().cooldownSeconds() * 1000L
        );
    }

    /**
     * Salvages the player's held supported item into scaled base materials.
     *
     * @param player player using the salvage block
     * @param townChunk Armory chunk being used
     * @return structured salvage result
     */
    public @NotNull SalvageResult salvageHeldItem(final @NotNull Player player, final @NotNull RTownChunk townChunk) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(townChunk, "townChunk");

        if (!this.isArmoryChunk(townChunk)) {
            return SalvageResult.invalidChunk();
        }
        if (!this.isSalvageBlockUnlocked(townChunk)) {
            return SalvageResult.locked();
        }

        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        if (runtimeService == null) {
            return SalvageResult.failed();
        }
        if (!runtimeService.isPlayerAllowed(player, townChunk, TownProtections.ARMORY_USE)) {
            return SalvageResult.noPermission();
        }
        if (!townChunk.hasSalvageBlock()) {
            return SalvageResult.noBlock();
        }

        final ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.isEmpty()) {
            return SalvageResult.unsupportedItem();
        }

        final SalvageRecipe recipe = SALVAGE_RECIPES.get(heldItem.getType());
        if (recipe == null) {
            return SalvageResult.unsupportedItem();
        }

        final double durabilityFactor = this.resolveDurabilityFactor(heldItem);
        final List<ItemStack> recoveredItems = new ArrayList<>();
        int recoveredMaterialCount = 0;
        for (final MaterialAmount materialAmount : recipe.materials()) {
            final int recoveredAmount = (int) Math.floor(materialAmount.amount() * durabilityFactor);
            if (recoveredAmount <= 0) {
                continue;
            }
            recoveredItems.add(new ItemStack(materialAmount.material(), recoveredAmount));
            recoveredMaterialCount += recoveredAmount;
        }

        this.consumeMainHandItem(player);
        this.giveItems(player, recoveredItems);
        return recoveredMaterialCount > 0
            ? SalvageResult.success(recoveredMaterialCount)
            : SalvageResult.successEmpty();
    }

    /**
     * Repairs the player's held supported item using configured family-specific repair materials.
     *
     * @param player player using the repair block
     * @param townChunk Armory chunk being used
     * @return structured repair result
     */
    public @NotNull RepairResult repairHeldItem(final @NotNull Player player, final @NotNull RTownChunk townChunk) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(townChunk, "townChunk");

        if (!this.isArmoryChunk(townChunk)) {
            return RepairResult.invalidChunk();
        }
        if (!this.isRepairBlockUnlocked(townChunk)) {
            return RepairResult.locked();
        }

        final TownRuntimeService runtimeService = this.plugin.getTownRuntimeService();
        if (runtimeService == null) {
            return RepairResult.failed();
        }
        if (!runtimeService.isPlayerAllowed(player, townChunk, TownProtections.ARMORY_USE)) {
            return RepairResult.noPermission();
        }
        if (!townChunk.hasRepairBlock()) {
            return RepairResult.noBlock();
        }

        final PlayerInventory inventory = player.getInventory();
        final ItemStack heldItem = inventory.getItemInMainHand();
        if (heldItem == null || heldItem.isEmpty()) {
            return RepairResult.unsupportedItem();
        }

        final RepairFamily repairFamily = REPAIR_FAMILIES.get(heldItem.getType());
        if (repairFamily == null) {
            return RepairResult.unsupportedItem();
        }

        final ItemMeta itemMeta = heldItem.getItemMeta();
        if (!(itemMeta instanceof Damageable damageable) || !damageable.hasDamage() || damageable.getDamage() <= 0) {
            return RepairResult.fullyRepaired();
        }

        final ArmoryConfigSection.RepairFamilySettings familySettings = this.resolveRepairSettings(repairFamily);
        if (familySettings.materialCost() > 0
            && !this.consumeInventoryMaterial(player, repairFamily.repairMaterial(), familySettings.materialCost())) {
            return RepairResult.notEnoughMaterials(repairFamily.repairMaterial(), familySettings.materialCost());
        }

        final ItemStack repairedItem = heldItem.clone();
        final ItemMeta repairedMeta = repairedItem.getItemMeta();
        if (!(repairedMeta instanceof Damageable repairedDamageable)) {
            return RepairResult.failed();
        }

        final int restoreAmount = this.resolveRestoreAmount(heldItem.getType(), familySettings.repairPercent());
        repairedDamageable.setDamage(Math.max(0, damageable.getDamage() - restoreAmount));
        repairedItem.setItemMeta(repairedMeta);
        inventory.setItemInMainHand(repairedItem);
        return RepairResult.success(repairFamily.repairMaterial(), familySettings.materialCost(), familySettings.repairPercent());
    }

    /**
     * Returns the fuel burn time that should be applied while Armory double smelting is active.
     *
     * @param townChunk chunk owning the furnace
     * @param originalBurnTime original furnace burn time
     * @return adjusted burn time, or the original value when no adjustment applies
     */
    public int resolveAdjustedBurnTime(final @Nullable RTownChunk townChunk, final int originalBurnTime) {
        if (!this.isDoubleSmeltActive(townChunk) || originalBurnTime <= 0) {
            return originalBurnTime;
        }

        final double burnFasterMultiplier = this.plugin.getArmoryConfig().getDoubleSmelt().burnFasterMultiplier();
        if (burnFasterMultiplier <= 1.0D) {
            return originalBurnTime;
        }
        return Math.max(1, (int) Math.ceil(originalBurnTime / burnFasterMultiplier));
    }

    /**
     * Applies the Armory double-smelt bonus to one furnace output when enough fuel remains.
     *
     * @param townChunk chunk owning the furnace
     * @param furnace furnace being updated
     * @param result base smelt result
     * @return structured double-smelt outcome
     */
    public @NotNull DoubleSmeltResult applyDoubleSmelt(
        final @Nullable RTownChunk townChunk,
        final @Nullable Furnace furnace,
        final @Nullable ItemStack result
    ) {
        if (result == null || result.isEmpty()) {
            return DoubleSmeltResult.noBonus(null);
        }
        if (!this.isDoubleSmeltActive(townChunk) || furnace == null) {
            return DoubleSmeltResult.noBonus(result.clone());
        }

        final int extraFuelPerSmeltUnits = this.plugin.getArmoryConfig().getDoubleSmelt().extraFuelPerSmeltUnits();
        if (extraFuelPerSmeltUnits > 0 && furnace.getBurnTime() < extraFuelPerSmeltUnits) {
            return DoubleSmeltResult.insufficientFuel(result.clone());
        }

        if (extraFuelPerSmeltUnits > 0) {
            furnace.setBurnTime((short) Math.max(0, furnace.getBurnTime() - extraFuelPerSmeltUnits));
            furnace.update(true, false);
        }

        final ItemStack doubledResult = result.clone();
        doubledResult.setAmount(Math.min(
            doubledResult.getMaxStackSize(),
            Math.max(1, doubledResult.getAmount() * 2)
        ));
        return new DoubleSmeltResult(doubledResult, true, false);
    }

    /**
     * Returns a bound salvage-block item for one Armory chunk.
     *
     * @param player player receiving the item
     * @param townChunk Armory chunk that owns the item
     * @return bound salvage-block item
     */
    public @NotNull ItemStack createSalvageBlockItem(final @NotNull Player player, final @NotNull RTownChunk townChunk) {
        return SalvageBlock.getSalvageBlockItem(
            this.plugin,
            player,
            townChunk.getTown().getTownUUID(),
            townChunk.getWorldName(),
            townChunk.getX(),
            townChunk.getZ()
        );
    }

    /**
     * Returns a bound repair-block item for one Armory chunk.
     *
     * @param player player receiving the item
     * @param townChunk Armory chunk that owns the item
     * @return bound repair-block item
     */
    public @NotNull ItemStack createRepairBlockItem(final @NotNull Player player, final @NotNull RTownChunk townChunk) {
        return RepairBlock.getRepairBlockItem(
            this.plugin,
            player,
            townChunk.getTown().getTownUUID(),
            townChunk.getWorldName(),
            townChunk.getX(),
            townChunk.getZ()
        );
    }

    private boolean isArmoryChunk(final @Nullable RTownChunk townChunk) {
        return townChunk != null && townChunk.getChunkType() == ChunkType.ARMORY;
    }

    private int repairInventorySlot(final @Nullable ItemStack itemStack, final @NotNull Consumer<ItemStack> setter) {
        final ItemStack repairedItem = this.repairItem(itemStack);
        if (repairedItem == null) {
            return 0;
        }
        setter.accept(repairedItem);
        return 1;
    }

    private @Nullable ItemStack repairItem(final @Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return null;
        }

        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (!(itemMeta instanceof Damageable damageable) || !damageable.hasDamage() || damageable.getDamage() <= 0) {
            return null;
        }

        final ItemStack repairedItem = itemStack.clone();
        final ItemMeta repairedMeta = repairedItem.getItemMeta();
        if (!(repairedMeta instanceof Damageable repairedDamageable)) {
            return null;
        }
        repairedDamageable.setDamage(0);
        repairedItem.setItemMeta(repairedMeta);
        return repairedItem;
    }

    private double resolveDurabilityFactor(final @NotNull ItemStack itemStack) {
        final int maxDurability = itemStack.getType().getMaxDurability();
        if (maxDurability <= 0) {
            return 1.0D;
        }

        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (!(itemMeta instanceof Damageable damageable)) {
            return 1.0D;
        }
        return Math.clamp((maxDurability - (double) damageable.getDamage()) / (double) maxDurability, 0.0D, 1.0D);
    }

    private int resolveRestoreAmount(final @NotNull Material material, final double repairPercent) {
        final int maxDurability = material.getMaxDurability();
        if (maxDurability <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.round(maxDurability * (repairPercent / 100.0D)));
    }

    private void consumeMainHandItem(final @NotNull Player player) {
        final PlayerInventory inventory = player.getInventory();
        final ItemStack heldItem = inventory.getItemInMainHand();
        if (heldItem == null || heldItem.isEmpty()) {
            return;
        }

        if (heldItem.getAmount() <= 1) {
            inventory.setItemInMainHand(null);
            return;
        }
        final ItemStack updatedItem = heldItem.clone();
        updatedItem.setAmount(heldItem.getAmount() - 1);
        inventory.setItemInMainHand(updatedItem);
    }

    private boolean consumeInventoryMaterial(
        final @NotNull Player player,
        final @NotNull Material material,
        final int amount
    ) {
        if (amount <= 0) {
            return true;
        }

        final PlayerInventory inventory = player.getInventory();
        final ItemStack[] contents = inventory.getContents();
        int remaining = amount;
        for (final ItemStack itemStack : contents) {
            if (itemStack == null || itemStack.isEmpty() || itemStack.getType() != material) {
                continue;
            }
            remaining -= itemStack.getAmount();
            if (remaining <= 0) {
                break;
            }
        }
        if (remaining > 0) {
            return false;
        }

        int toConsume = amount;
        for (int slot = 0; slot < contents.length && toConsume > 0; slot++) {
            final ItemStack itemStack = contents[slot];
            if (itemStack == null || itemStack.isEmpty() || itemStack.getType() != material) {
                continue;
            }

            if (itemStack.getAmount() <= toConsume) {
                toConsume -= itemStack.getAmount();
                contents[slot] = null;
                continue;
            }

            final ItemStack updatedItem = itemStack.clone();
            updatedItem.setAmount(itemStack.getAmount() - toConsume);
            contents[slot] = updatedItem;
            toConsume = 0;
        }
        inventory.setContents(contents);
        return true;
    }

    private void giveItems(final @NotNull Player player, final @NotNull List<ItemStack> items) {
        for (final ItemStack itemStack : items) {
            if (itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            player.getInventory().addItem(itemStack)
                .values()
                .forEach(overflow -> player.getWorld().dropItemNaturally(player.getLocation(), overflow));
        }
    }

    private @NotNull ArmoryConfigSection.RepairFamilySettings resolveRepairSettings(final @NotNull RepairFamily repairFamily) {
        final var repairBlockSettings = this.plugin.getArmoryConfig().getRepairBlock();
        return switch (repairFamily) {
            case IRON -> repairBlockSettings.iron();
            case GOLD -> repairBlockSettings.gold();
            case DIAMOND -> repairBlockSettings.diamond();
            case NETHERITE -> repairBlockSettings.netherite();
        };
    }

    private static @NotNull Map<Material, RepairFamily> createRepairFamilies() {
        final Map<Material, RepairFamily> families = new EnumMap<>(Material.class);
        registerRepairFamily(families, RepairFamily.IRON,
            Material.IRON_SWORD, Material.IRON_SHOVEL, Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_HOE,
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS
        );
        registerRepairFamily(families, RepairFamily.GOLD,
            Material.GOLDEN_SWORD, Material.GOLDEN_SHOVEL, Material.GOLDEN_PICKAXE, Material.GOLDEN_AXE, Material.GOLDEN_HOE,
            Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS
        );
        registerRepairFamily(families, RepairFamily.DIAMOND,
            Material.DIAMOND_SWORD, Material.DIAMOND_SHOVEL, Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_HOE,
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS
        );
        registerRepairFamily(families, RepairFamily.NETHERITE,
            Material.NETHERITE_SWORD, Material.NETHERITE_SHOVEL, Material.NETHERITE_PICKAXE, Material.NETHERITE_AXE, Material.NETHERITE_HOE,
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS
        );
        return Map.copyOf(families);
    }

    private static void registerRepairFamily(
        final @NotNull Map<Material, RepairFamily> repairFamilies,
        final @NotNull RepairFamily repairFamily,
        final @NotNull Material... supportedMaterials
    ) {
        for (final Material supportedMaterial : supportedMaterials) {
            repairFamilies.put(supportedMaterial, repairFamily);
        }
    }

    private static @NotNull Map<Material, SalvageRecipe> createSalvageRecipes() {
        final Map<Material, SalvageRecipe> recipes = new EnumMap<>(Material.class);
        registerFamilyRecipes(recipes, Material.COBBLESTONE,
            Material.STONE_SWORD, Material.STONE_SHOVEL, Material.STONE_PICKAXE, Material.STONE_AXE, Material.STONE_HOE,
            null, null, null, null
        );
        registerFamilyRecipes(recipes, Material.IRON_INGOT,
            Material.IRON_SWORD, Material.IRON_SHOVEL, Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_HOE,
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS
        );
        registerFamilyRecipes(recipes, Material.GOLD_INGOT,
            Material.GOLDEN_SWORD, Material.GOLDEN_SHOVEL, Material.GOLDEN_PICKAXE, Material.GOLDEN_AXE, Material.GOLDEN_HOE,
            Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS
        );
        registerFamilyRecipes(recipes, Material.DIAMOND,
            Material.DIAMOND_SWORD, Material.DIAMOND_SHOVEL, Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_HOE,
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS
        );
        registerFamilyRecipes(recipes, Material.LEATHER,
            null, null, null, null, null,
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS
        );
        registerNetheriteRecipes(recipes);
        recipes.put(Material.SHIELD, new SalvageRecipe(List.of(
            new MaterialAmount(Material.OAK_PLANKS, 6),
            new MaterialAmount(Material.IRON_INGOT, 1)
        )));
        recipes.put(Material.BOW, new SalvageRecipe(List.of(
            new MaterialAmount(Material.STICK, 3),
            new MaterialAmount(Material.STRING, 3)
        )));
        recipes.put(Material.CROSSBOW, new SalvageRecipe(List.of(
            new MaterialAmount(Material.STICK, 3),
            new MaterialAmount(Material.STRING, 2),
            new MaterialAmount(Material.IRON_INGOT, 1),
            new MaterialAmount(Material.TRIPWIRE_HOOK, 1)
        )));
        recipes.put(Material.FISHING_ROD, new SalvageRecipe(List.of(
            new MaterialAmount(Material.STICK, 3),
            new MaterialAmount(Material.STRING, 2)
        )));
        recipes.put(Material.SHEARS, new SalvageRecipe(List.of(new MaterialAmount(Material.IRON_INGOT, 2))));
        recipes.put(Material.FLINT_AND_STEEL, new SalvageRecipe(List.of(
            new MaterialAmount(Material.IRON_INGOT, 1),
            new MaterialAmount(Material.FLINT, 1)
        )));
        recipes.put(Material.TURTLE_HELMET, new SalvageRecipe(List.of(new MaterialAmount(Material.TURTLE_SCUTE, 5))));
        recipes.put(Material.WOLF_ARMOR, new SalvageRecipe(List.of(new MaterialAmount(Material.ARMADILLO_SCUTE, 6))));
        recipes.put(Material.MACE, new SalvageRecipe(List.of(
            new MaterialAmount(Material.BREEZE_ROD, 1),
            new MaterialAmount(Material.HEAVY_CORE, 1)
        )));
        return Map.copyOf(recipes);
    }

    private static void registerFamilyRecipes(
        final @NotNull Map<Material, SalvageRecipe> recipes,
        final @NotNull Material coreMaterial,
        final @Nullable Material sword,
        final @Nullable Material shovel,
        final @Nullable Material pickaxe,
        final @Nullable Material axe,
        final @Nullable Material hoe,
        final @Nullable Material helmet,
        final @Nullable Material chestplate,
        final @Nullable Material leggings,
        final @Nullable Material boots
    ) {
        putIfPresent(recipes, sword, List.of(new MaterialAmount(coreMaterial, 2), new MaterialAmount(Material.STICK, 1)));
        putIfPresent(recipes, shovel, List.of(new MaterialAmount(coreMaterial, 1), new MaterialAmount(Material.STICK, 2)));
        putIfPresent(recipes, pickaxe, List.of(new MaterialAmount(coreMaterial, 3), new MaterialAmount(Material.STICK, 2)));
        putIfPresent(recipes, axe, List.of(new MaterialAmount(coreMaterial, 3), new MaterialAmount(Material.STICK, 2)));
        putIfPresent(recipes, hoe, List.of(new MaterialAmount(coreMaterial, 2), new MaterialAmount(Material.STICK, 2)));
        putIfPresent(recipes, helmet, List.of(new MaterialAmount(coreMaterial, 5)));
        putIfPresent(recipes, chestplate, List.of(new MaterialAmount(coreMaterial, 8)));
        putIfPresent(recipes, leggings, List.of(new MaterialAmount(coreMaterial, 7)));
        putIfPresent(recipes, boots, List.of(new MaterialAmount(coreMaterial, 4)));
    }

    private static void registerNetheriteRecipes(final @NotNull Map<Material, SalvageRecipe> recipes) {
        putIfPresent(recipes, Material.NETHERITE_SWORD, List.of(
            new MaterialAmount(Material.NETHERITE_INGOT, 1),
            new MaterialAmount(Material.DIAMOND, 2),
            new MaterialAmount(Material.STICK, 1)
        ));
        putIfPresent(recipes, Material.NETHERITE_SHOVEL, List.of(
            new MaterialAmount(Material.NETHERITE_INGOT, 1),
            new MaterialAmount(Material.DIAMOND, 1),
            new MaterialAmount(Material.STICK, 2)
        ));
        putIfPresent(recipes, Material.NETHERITE_PICKAXE, List.of(
            new MaterialAmount(Material.NETHERITE_INGOT, 1),
            new MaterialAmount(Material.DIAMOND, 3),
            new MaterialAmount(Material.STICK, 2)
        ));
        putIfPresent(recipes, Material.NETHERITE_AXE, List.of(
            new MaterialAmount(Material.NETHERITE_INGOT, 1),
            new MaterialAmount(Material.DIAMOND, 3),
            new MaterialAmount(Material.STICK, 2)
        ));
        putIfPresent(recipes, Material.NETHERITE_HOE, List.of(
            new MaterialAmount(Material.NETHERITE_INGOT, 1),
            new MaterialAmount(Material.DIAMOND, 2),
            new MaterialAmount(Material.STICK, 2)
        ));
        putIfPresent(recipes, Material.NETHERITE_HELMET, List.of(
            new MaterialAmount(Material.NETHERITE_INGOT, 1),
            new MaterialAmount(Material.DIAMOND, 5)
        ));
        putIfPresent(recipes, Material.NETHERITE_CHESTPLATE, List.of(
            new MaterialAmount(Material.NETHERITE_INGOT, 1),
            new MaterialAmount(Material.DIAMOND, 8)
        ));
        putIfPresent(recipes, Material.NETHERITE_LEGGINGS, List.of(
            new MaterialAmount(Material.NETHERITE_INGOT, 1),
            new MaterialAmount(Material.DIAMOND, 7)
        ));
        putIfPresent(recipes, Material.NETHERITE_BOOTS, List.of(
            new MaterialAmount(Material.NETHERITE_INGOT, 1),
            new MaterialAmount(Material.DIAMOND, 4)
        ));
    }

    private static void putIfPresent(
        final @NotNull Map<Material, SalvageRecipe> recipes,
        final @Nullable Material outputMaterial,
        final @NotNull List<MaterialAmount> materials
    ) {
        if (outputMaterial != null) {
            recipes.put(outputMaterial, new SalvageRecipe(List.copyOf(materials)));
        }
    }

    private enum RepairFamily {
        IRON(Material.IRON_INGOT),
        GOLD(Material.GOLD_INGOT),
        DIAMOND(Material.DIAMOND),
        NETHERITE(Material.NETHERITE_INGOT);

        private final Material repairMaterial;

        RepairFamily(final @NotNull Material repairMaterial) {
            this.repairMaterial = repairMaterial;
        }

        private @NotNull Material repairMaterial() {
            return this.repairMaterial;
        }
    }

    private record MaterialAmount(@NotNull Material material, int amount) {
    }

    private record SalvageRecipe(@NotNull List<MaterialAmount> materials) {
    }

    /**
     * Status for one free-repair attempt.
     */
    public enum FreeRepairStatus {
        SUCCESS,
        NO_PERMISSION,
        INVALID_CHUNK,
        LOCKED,
        COOLING_DOWN,
        NOTHING_TO_REPAIR,
        FAILED
    }

    /**
     * Result of one free-repair attempt.
     *
     * @param status repair outcome
     * @param repairedItems number of equipped items repaired
     * @param cooldownRemainingMillis remaining cooldown in milliseconds
     */
    public record FreeRepairResult(
        @NotNull FreeRepairStatus status,
        int repairedItems,
        long cooldownRemainingMillis
    ) {

        private static @NotNull FreeRepairResult success(final int repairedItems, final long cooldownRemainingMillis) {
            return new FreeRepairResult(FreeRepairStatus.SUCCESS, repairedItems, Math.max(0L, cooldownRemainingMillis));
        }

        private static @NotNull FreeRepairResult noPermission() {
            return new FreeRepairResult(FreeRepairStatus.NO_PERMISSION, 0, 0L);
        }

        private static @NotNull FreeRepairResult invalidChunk() {
            return new FreeRepairResult(FreeRepairStatus.INVALID_CHUNK, 0, 0L);
        }

        private static @NotNull FreeRepairResult locked() {
            return new FreeRepairResult(FreeRepairStatus.LOCKED, 0, 0L);
        }

        private static @NotNull FreeRepairResult coolingDown(final long cooldownRemainingMillis) {
            return new FreeRepairResult(FreeRepairStatus.COOLING_DOWN, 0, Math.max(0L, cooldownRemainingMillis));
        }

        private static @NotNull FreeRepairResult nothingToRepair() {
            return new FreeRepairResult(FreeRepairStatus.NOTHING_TO_REPAIR, 0, 0L);
        }

        private static @NotNull FreeRepairResult failed() {
            return new FreeRepairResult(FreeRepairStatus.FAILED, 0, 0L);
        }
    }

    /**
     * Status for one salvage-block action.
     */
    public enum SalvageStatus {
        SUCCESS,
        SUCCESS_EMPTY,
        NO_PERMISSION,
        INVALID_CHUNK,
        LOCKED,
        NO_BLOCK,
        UNSUPPORTED_ITEM,
        FAILED
    }

    /**
     * Result of one salvage-block action.
     *
     * @param status salvage outcome
     * @param recoveredMaterialCount total material items returned to the player
     */
    public record SalvageResult(
        @NotNull SalvageStatus status,
        int recoveredMaterialCount
    ) {

        private static @NotNull SalvageResult success(final int recoveredMaterialCount) {
            return new SalvageResult(SalvageStatus.SUCCESS, Math.max(0, recoveredMaterialCount));
        }

        private static @NotNull SalvageResult successEmpty() {
            return new SalvageResult(SalvageStatus.SUCCESS_EMPTY, 0);
        }

        private static @NotNull SalvageResult noPermission() {
            return new SalvageResult(SalvageStatus.NO_PERMISSION, 0);
        }

        private static @NotNull SalvageResult invalidChunk() {
            return new SalvageResult(SalvageStatus.INVALID_CHUNK, 0);
        }

        private static @NotNull SalvageResult locked() {
            return new SalvageResult(SalvageStatus.LOCKED, 0);
        }

        private static @NotNull SalvageResult noBlock() {
            return new SalvageResult(SalvageStatus.NO_BLOCK, 0);
        }

        private static @NotNull SalvageResult unsupportedItem() {
            return new SalvageResult(SalvageStatus.UNSUPPORTED_ITEM, 0);
        }

        private static @NotNull SalvageResult failed() {
            return new SalvageResult(SalvageStatus.FAILED, 0);
        }
    }

    /**
     * Status for one repair-block action.
     */
    public enum RepairStatus {
        SUCCESS,
        NO_PERMISSION,
        INVALID_CHUNK,
        LOCKED,
        NO_BLOCK,
        UNSUPPORTED_ITEM,
        FULLY_REPAIRED,
        NOT_ENOUGH_MATERIALS,
        FAILED
    }

    /**
     * Result of one repair-block action.
     *
     * @param status repair outcome
     * @param repairMaterial repair material required for the supported family
     * @param materialCost configured material cost for the repair
     * @param repairPercent configured percentage restored by the repair
     */
    public record RepairResult(
        @NotNull RepairStatus status,
        @Nullable Material repairMaterial,
        int materialCost,
        double repairPercent
    ) {

        private static @NotNull RepairResult success(
            final @NotNull Material repairMaterial,
            final int materialCost,
            final double repairPercent
        ) {
            return new RepairResult(RepairStatus.SUCCESS, repairMaterial, materialCost, repairPercent);
        }

        private static @NotNull RepairResult noPermission() {
            return new RepairResult(RepairStatus.NO_PERMISSION, null, 0, 0.0D);
        }

        private static @NotNull RepairResult invalidChunk() {
            return new RepairResult(RepairStatus.INVALID_CHUNK, null, 0, 0.0D);
        }

        private static @NotNull RepairResult locked() {
            return new RepairResult(RepairStatus.LOCKED, null, 0, 0.0D);
        }

        private static @NotNull RepairResult noBlock() {
            return new RepairResult(RepairStatus.NO_BLOCK, null, 0, 0.0D);
        }

        private static @NotNull RepairResult unsupportedItem() {
            return new RepairResult(RepairStatus.UNSUPPORTED_ITEM, null, 0, 0.0D);
        }

        private static @NotNull RepairResult fullyRepaired() {
            return new RepairResult(RepairStatus.FULLY_REPAIRED, null, 0, 0.0D);
        }

        private static @NotNull RepairResult notEnoughMaterials(
            final @NotNull Material repairMaterial,
            final int materialCost
        ) {
            return new RepairResult(RepairStatus.NOT_ENOUGH_MATERIALS, repairMaterial, materialCost, 0.0D);
        }

        private static @NotNull RepairResult failed() {
            return new RepairResult(RepairStatus.FAILED, null, 0, 0.0D);
        }
    }

    /**
     * Result of one double-smelt evaluation.
     *
     * @param result resulting item stack after bonus processing
     * @param bonusApplied whether the smelt result was doubled
     * @param insufficientFuel whether the smelt stayed normal because the extra surcharge could not be paid
     */
    public record DoubleSmeltResult(
        @Nullable ItemStack result,
        boolean bonusApplied,
        boolean insufficientFuel
    ) {

        private static @NotNull DoubleSmeltResult noBonus(final @Nullable ItemStack result) {
            return new DoubleSmeltResult(result, false, false);
        }

        private static @NotNull DoubleSmeltResult insufficientFuel(final @Nullable ItemStack result) {
            return new DoubleSmeltResult(result, false, true);
        }
    }
}
