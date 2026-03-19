package de.jexcellence.oneblock.database.entity.evolution;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Factory for creating custom evolution items with special properties
 * Replaces EnhancedStageItemFactory with cleaner implementation
 */
public final class EvolutionItemFactory {
    
    private EvolutionItemFactory() {}
    
    // ========== Divine Weapons ==========
    
    public static ItemStack createBowOfArtemis(@Nullable Player player) {
        return createEnchantedItem(Material.BOW, "§a§lBow of Artemis", List.of(
            "§7The huntress goddess's divine bow",
            "§8Blessed by the moon and forest spirits",
            "",
            "§6[UNIQUE] §eEvolution Artemis",
            player != null ? "§7Bound to: " + player.getName() : ""
        ), new EnchantmentData(Enchantment.POWER, 5), new EnchantmentData(Enchantment.PUNCH, 2),
           new EnchantmentData(Enchantment.UNBREAKING, 3), new EnchantmentData(Enchantment.INFINITY, 1));
    }
    
    public static ItemStack createHammerOfHephaestus(@Nullable Player player) {
        return createEnchantedItem(Material.NETHERITE_AXE, "§6§lHammer of Hephaestus", List.of(
            "§7The divine forge hammer of the god of fire",
            "§8Can shape any metal with divine precision",
            "",
            "§6[UNIQUE] §eEvolution Hephaestus",
            player != null ? "§7Wielded by: " + player.getName() : ""
        ), new EnchantmentData(Enchantment.EFFICIENCY, 10), new EnchantmentData(Enchantment.UNBREAKING, 10),
           new EnchantmentData(Enchantment.FORTUNE, 5));
    }
    
    public static ItemStack createWingedBoots(@Nullable Player player) {
        ItemStack boots = new ItemStack(Material.GOLDEN_BOOTS);
        ItemMeta meta = boots.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§lWinged Boots of Hermes");
            meta.setLore(List.of(
                "§7The divine sandals of the messenger god",
                "§8Grant incredible speed and agility",
                "",
                "§6[UNIQUE] §eEvolution Hermes",
                player != null ? "§7Swift traveler: " + player.getName() : ""
            ));
            meta.addEnchant(Enchantment.FEATHER_FALLING, 10, true);
            meta.addEnchant(Enchantment.DEPTH_STRIDER, 3, true);
            meta.addEnchant(Enchantment.SOUL_SPEED, 3, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            boots.setItemMeta(meta);
        }
        return boots;
    }

    
    // ========== Technology Items ==========
    
    public static ItemStack createMasterJewelerKit(@Nullable Player player) {
        return createEnchantedItem(Material.DIAMOND, "§b§lMaster Jeweler's Kit", List.of(
            "§7The ultimate crafting tool for gems",
            "§8Cuts and polishes with perfect precision",
            "",
            "§6[TRANSCENDENT] §eEvolution Diamond",
            player != null ? "§7Master: " + player.getName() : ""
        ));
    }
    
    public static ItemStack createFusionReactorCore(@Nullable Player player) {
        return createEnchantedItem(Material.NETHER_STAR, "§c§lFusion Reactor Core", List.of(
            "§7The heart of nuclear fusion power",
            "§8Generates unlimited clean energy",
            "",
            "§6[OMNIPOTENT] §eEvolution Nuclear",
            player != null ? "§7Engineer: " + player.getName() : ""
        ));
    }
    
    public static ItemStack createAssemblyLineController(@Nullable Player player) {
        return createEnchantedItem(Material.COMPARATOR, "§6§lAssembly Line Controller", List.of(
            "§7Ultimate factory automation device",
            "§8Controls entire production lines",
            "",
            "§6[OMNIPOTENT] §eEvolution Factory",
            player != null ? "§7Industrialist: " + player.getName() : ""
        ));
    }
    
    public static ItemStack createMolecularAssembler(@Nullable Player player) {
        return createEnchantedItem(Material.END_CRYSTAL, "§d§lMolecular Assembler", List.of(
            "§7Manipulates matter at atomic level",
            "§8Creates anything from base elements",
            "",
            "§6[OMNIPOTENT] §eEvolution Nano",
            player != null ? "§7Scientist: " + player.getName() : ""
        ));
    }
    
    public static ItemStack createGalacticCommandCenter(@Nullable Player player) {
        return createEnchantedItem(Material.BEACON, "§5§lGalactic Command Center", List.of(
            "§7Controls entire star systems",
            "§8The seat of galactic power",
            "",
            "§6[OMNIPOTENT] §eEvolution Galactic",
            player != null ? "§7Commander: " + player.getName() : ""
        ));
    }
    
    // ========== Cosmic Items ==========
    
    public static ItemStack createRealityManipulator(@Nullable Player player) {
        return createEnchantedItem(Material.NETHER_STAR, "§5§lReality Manipulator", List.of(
            "§7Bends the fabric of reality",
            "§8Rewrites the laws of physics",
            "",
            "§6[OMNIPOTENT] §eEvolution Universal",
            player != null ? "§7Reality Weaver: " + player.getName() : ""
        ));
    }
    
    public static ItemStack createCosmicSingularity(@Nullable Player player) {
        return createEnchantedItem(Material.DRAGON_EGG, "§d§lCosmic Singularity", List.of(
            "§7A point of infinite density",
            "§8Contains the power of creation",
            "",
            "§6[OMNIPOTENT] §eEvolution Cosmic",
            player != null ? "§7Cosmic Lord: " + player.getName() : ""
        ));
    }
    
    public static ItemStack createMultiverseGateway(@Nullable Player player) {
        return createEnchantedItem(Material.END_PORTAL_FRAME, "§5§lMultiverse Gateway", List.of(
            "§7Opens portals to parallel realities",
            "§8Travel between infinite dimensions",
            "",
            "§6[OMNIPOTENT] §eEvolution Dimensional",
            player != null ? "§7Dimension Walker: " + player.getName() : ""
        ));
    }
    
    // ========== Nature & Element Items ==========
    
    public static ItemStack createAlphiriusHoe(@Nullable Player player) {
        return createEnchantedItem(Material.NETHERITE_HOE, "§a§lAlphirius Hoe", List.of(
            "§7The ultimate farming tool of Earth",
            "§8Can cultivate any terrain",
            "",
            "§6[UNIQUE] §eEvolution Earth",
            player != null ? "§7Earth Guardian: " + player.getName() : ""
        ), new EnchantmentData(Enchantment.UNBREAKING, 10), new EnchantmentData(Enchantment.EFFICIENCY, 10));
    }
    
    public static ItemStack createExplorerCompass(@Nullable Player player) {
        return createEnchantedItem(Material.RECOVERY_COMPASS, "§e§lExplorer's Compass", List.of(
            "§7Points to undiscovered treasures",
            "§8Never lose your way again",
            "",
            "§6[UNIQUE] §eEvolution Explorer",
            player != null ? "§7Explorer: " + player.getName() : ""
        ));
    }

    
    // ========== Medieval & Fantasy Items ==========
    
    public static ItemStack createDragonSoulOrb(@Nullable Player player) {
        return createEnchantedItem(Material.DRAGON_EGG, "§c§lDragon Soul Orb", List.of(
            "§7Contains the essence of dragons",
            "§8Grants draconic power to its wielder",
            "",
            "§6[OMNIPOTENT] §eEvolution Dragon",
            player != null ? "§7Dragon Master: " + player.getName() : ""
        ));
    }
    
    public static ItemStack createHolyGrail(@Nullable Player player) {
        return createEnchantedItem(Material.GOLDEN_APPLE, "§e§lHoly Grail", List.of(
            "§7The legendary cup of eternal life",
            "§8Grants divine protection and healing",
            "",
            "§6[OMNIPOTENT] §eEvolution Crusader",
            player != null ? "§7Crusader: " + player.getName() : ""
        ));
    }
    
    public static ItemStack createFortressGateKey(@Nullable Player player) {
        return createEnchantedItem(Material.TRIPWIRE_HOOK, "§7§lFortress Gate Key", List.of(
            "§7Opens any fortress gate",
            "§8The ultimate siege tool",
            "",
            "§6[OMNIPOTENT] §eEvolution Castle",
            player != null ? "§7Siege Master: " + player.getName() : ""
        ));
    }
    
    // ========== Digital & Cyber Items ==========
    
    public static ItemStack createNeuralInterface(@Nullable Player player) {
        return createEnchantedItem(Material.SCULK_SENSOR, "§b§lNeural Interface", List.of(
            "§7Direct brain-computer connection",
            "§8Control machines with thought",
            "",
            "§6[OMNIPOTENT] §eEvolution Cyber",
            player != null ? "§7Cyborg: " + player.getName() : ""
        ));
    }
    
    public static ItemStack createQuantumProcessor(@Nullable Player player) {
        return createEnchantedItem(Material.REDSTONE_BLOCK, "§c§lQuantum Processor", List.of(
            "§7Computes in parallel universes",
            "§8Infinite processing power",
            "",
            "§6[OMNIPOTENT] §eEvolution Digital",
            player != null ? "§7Digital Architect: " + player.getName() : ""
        ));
    }
    
    public static ItemStack createTeslaCoil(@Nullable Player player) {
        return createEnchantedItem(Material.LIGHTNING_ROD, "§e§lTesla Coil", List.of(
            "§7Harnesses unlimited electricity",
            "§8Powers entire civilizations",
            "",
            "§6[OMNIPOTENT] §eEvolution Electric",
            player != null ? "§7Electrician: " + player.getName() : ""
        ));
    }
    
    // ========== Bio & Life Items ==========
    
    public static ItemStack createGenesisEngine(@Nullable Player player) {
        return createEnchantedItem(Material.HEART_OF_THE_SEA, "§a§lGenesis Engine", List.of(
            "§7Creates life from nothing",
            "§8The ultimate bio-engineering device",
            "",
            "§6[OMNIPOTENT] §eEvolution Bio",
            player != null ? "§7Life Giver: " + player.getName() : ""
        ));
    }
    
    public static ItemStack createEdenFruit(@Nullable Player player) {
        return createEnchantedItem(Material.ENCHANTED_GOLDEN_APPLE, "§6§lFruit of Eden", List.of(
            "§7The forbidden fruit of paradise",
            "§8Grants ultimate knowledge and power",
            "",
            "§6[OMNIPOTENT] §eEvolution Eden",
            player != null ? "§7Chosen One: " + player.getName() : ""
        ));
    }
    
    public static ItemStack createTreeOfLife(@Nullable Player player) {
        return createEnchantedItem(Material.OAK_SAPLING, "§a§lTree of Life Seed", List.of(
            "§7Grows into the Tree of Life",
            "§8Source of all living things",
            "",
            "§6[OMNIPOTENT] §eEvolution Eden",
            player != null ? "§7Guardian: " + player.getName() : ""
        ));
    }
    
    // ========== Void & End Items ==========
    
    public static ItemStack createVoidWalkerCrown(@Nullable Player player) {
        ItemStack crown = new ItemStack(Material.NETHERITE_HELMET);
        ItemMeta meta = crown.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§5§lVoid Walker's Crown");
            meta.setLore(List.of(
                "§7Crown of the End dimension ruler",
                "§8Grants immunity to void damage",
                "",
                "§6[OMNIPOTENT] §eEvolution End",
                player != null ? "§7Void Walker: " + player.getName() : ""
            ));
            meta.addEnchant(Enchantment.PROTECTION, 10, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.RESPIRATION, 3, true);
            crown.setItemMeta(meta);
        }
        return crown;
    }
    
    // ========== Art & Culture Items ==========
    
    public static ItemStack createDivinePalette(@Nullable Player player) {
        return createEnchantedItem(Material.PAINTING, "§d§lDivine Palette", List.of(
            "§7Paints reality into existence",
            "§8The ultimate artistic tool",
            "",
            "§6[OMNIPOTENT] §eEvolution Artist",
            player != null ? "§7Divine Artist: " + player.getName() : ""
        ));
    }

    
    // ========== Magical Items ==========
    
    public static ItemStack createCursedBook(@Nullable Player player) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§5§lCursed Tome");
            meta.setLore(List.of(
                "§7A book filled with dark magic",
                "§8Contains forbidden knowledge",
                "",
                "§6[EPIC] §eEvolution Argon",
                player != null ? "§7Cursed by: " + player.getName() : ""
            ));
            meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
            meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
            book.setItemMeta(meta);
        }
        return book;
    }
    
    public static ItemStack createYeetFruit(@Nullable Player player) {
        ItemStack fruit = new ItemStack(Material.CHORUS_FRUIT);
        ItemMeta meta = fruit.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d§lYeet Fruit");
            meta.setLore(List.of(
                "§7A magical fruit from the End",
                "§8Teleports you to random locations",
                "",
                "§6[SPECIAL] §eEvolution Argon",
                player != null ? "§7Yeeted by: " + player.getName() : ""
            ));
            fruit.setItemMeta(meta);
        }
        return fruit;
    }
    
    // ========== Helper Methods ==========
    
    private static ItemStack createEnchantedItem(Material material, String name, List<String> lore, EnchantmentData... enchantments) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            for (EnchantmentData enchant : enchantments) {
                meta.addEnchant(enchant.enchantment(), enchant.level(), true);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private record EnchantmentData(Enchantment enchantment, int level) {}
    
    // ========== Krypton Items ==========
    
    public static ItemStack createKryptonHelmet(@Nullable Player player) {
        return createEnchantedItem(Material.NETHERITE_HELMET, "§b§lKrypton Helmet", List.of(
            "§7Helmet forged from pure Krypton",
            "§8Provides ultimate protection",
            "",
            "§6[UNIQUE] §eEvolution Krypton",
            player != null ? "§7Worn by: " + player.getName() : ""
        ), new EnchantmentData(Enchantment.PROTECTION, 10), new EnchantmentData(Enchantment.UNBREAKING, 10));
    }
    
    public static ItemStack createKryptonChestplate(@Nullable Player player) {
        return createEnchantedItem(Material.NETHERITE_CHESTPLATE, "§b§lKrypton Chestplate", List.of(
            "§7Chestplate forged from pure Krypton",
            "§8Provides ultimate protection",
            "",
            "§6[UNIQUE] §eEvolution Krypton",
            player != null ? "§7Worn by: " + player.getName() : ""
        ), new EnchantmentData(Enchantment.PROTECTION, 10), new EnchantmentData(Enchantment.UNBREAKING, 10));
    }
    
    public static ItemStack createKryptonLeggings(@Nullable Player player) {
        return createEnchantedItem(Material.NETHERITE_LEGGINGS, "§b§lKrypton Leggings", List.of(
            "§7Leggings forged from pure Krypton",
            "§8Provides ultimate protection",
            "",
            "§6[UNIQUE] §eEvolution Krypton",
            player != null ? "§7Worn by: " + player.getName() : ""
        ), new EnchantmentData(Enchantment.PROTECTION, 10), new EnchantmentData(Enchantment.UNBREAKING, 10));
    }
    
    public static ItemStack createKryptonBoots(@Nullable Player player) {
        return createEnchantedItem(Material.NETHERITE_BOOTS, "§b§lKrypton Boots", List.of(
            "§7Boots forged from pure Krypton",
            "§8Provides ultimate protection",
            "",
            "§6[UNIQUE] §eEvolution Krypton",
            player != null ? "§7Worn by: " + player.getName() : ""
        ), new EnchantmentData(Enchantment.PROTECTION, 10), new EnchantmentData(Enchantment.UNBREAKING, 10),
           new EnchantmentData(Enchantment.FEATHER_FALLING, 4));
    }
    
    public static ItemStack createKryptonSword(@Nullable Player player) {
        return createEnchantedItem(Material.NETHERITE_SWORD, "§b§lKrypton Sword", List.of(
            "§7Sword forged from pure Krypton",
            "§8Cuts through anything",
            "",
            "§6[UNIQUE] §eEvolution Krypton",
            player != null ? "§7Wielded by: " + player.getName() : ""
        ), new EnchantmentData(Enchantment.SHARPNESS, 10), new EnchantmentData(Enchantment.UNBREAKING, 10),
           new EnchantmentData(Enchantment.LOOTING, 3));
    }
    
    // ========== Other Evolution Items ==========
    
    public static ItemStack createInfernalPortalKey(@Nullable Player player) {
        return createEnchantedItem(Material.BLAZE_ROD, "§c§lInfernal Portal Key", List.of(
            "§7Opens portals to the deepest Nether",
            "§8Burns with eternal flame",
            "",
            "§6[UNIQUE] §eEvolution Nether",
            player != null ? "§7Keeper: " + player.getName() : ""
        ));
    }
    
    public static ItemStack createProbabilityEngine(@Nullable Player player) {
        return createEnchantedItem(Material.END_CRYSTAL, "§d§lProbability Engine", List.of(
            "§7Manipulates probability itself",
            "§8Bends luck to your will",
            "",
            "§6[UNIQUE] §eEvolution Quantum",
            player != null ? "§7Probability Master: " + player.getName() : ""
        ));
    }
    
    public static ItemStack createStellarForge(@Nullable Player player) {
        return createEnchantedItem(Material.BEACON, "§e§lStellar Forge", List.of(
            "§7A forge powered by starlight",
            "§8Creates items of cosmic power",
            "",
            "§6[UNIQUE] §eEvolution Stellar",
            player != null ? "§7Star Smith: " + player.getName() : ""
        ));
    }
    
    public static ItemStack createLunarCrystal(@Nullable Player player) {
        return createEnchantedItem(Material.AMETHYST_SHARD, "§9§lLunar Crystal", List.of(
            "§7A crystal infused with moonlight",
            "§8Glows with ethereal power",
            "",
            "§6[UNIQUE] §eEvolution Moon",
            player != null ? "§7Moon Child: " + player.getName() : ""
        ));
    }
    
    public static ItemStack createKnightShield(@Nullable Player player) {
        return createEnchantedItem(Material.SHIELD, "§7§lKnight's Shield", List.of(
            "§7A shield of legendary knights",
            "§8Protects against all attacks",
            "",
            "§6[UNIQUE] §eEvolution Knight",
            player != null ? "§7Knight: " + player.getName() : ""
        ), new EnchantmentData(Enchantment.UNBREAKING, 10));
    }
    
    public static ItemStack createSlowFallingPotion(@Nullable Player player) {
        ItemStack potion = new ItemStack(Material.POTION);
        ItemMeta meta = potion.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§lHelium Elixir");
            meta.setLore(List.of(
                "§7A potion infused with helium",
                "§8Grants the ability to float",
                "",
                "§6[LEGENDARY] §eEvolution Helium",
                player != null ? "§7Brewed for: " + player.getName() : ""
            ));
            potion.setItemMeta(meta);
        }
        return potion;
    }
    
    public static ItemStack createExcalibur(@Nullable Player player) {
        return createEnchantedItem(Material.NETHERITE_SWORD, "§e§lExcalibur", List.of(
            "§7The legendary sword of kings",
            "§8Only the worthy may wield it",
            "",
            "§6[OMNIPOTENT] §eEvolution Knight",
            player != null ? "§7King: " + player.getName() : ""
        ), new EnchantmentData(Enchantment.SHARPNESS, 10), new EnchantmentData(Enchantment.UNBREAKING, 10),
           new EnchantmentData(Enchantment.FIRE_ASPECT, 2), new EnchantmentData(Enchantment.LOOTING, 5));
    }
    
    public static ItemStack createMoonRocket(@Nullable Player player) {
        return createEnchantedItem(Material.FIREWORK_ROCKET, "§9§lMoon Rocket", List.of(
            "§7A rocket to reach the moon",
            "§8Powered by lunar energy",
            "",
            "§6[UNIQUE] §eEvolution Moon",
            player != null ? "§7Astronaut: " + player.getName() : ""
        ));
    }
}
