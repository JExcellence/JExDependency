package de.jexcellence.oneblock.type;

import lombok.Getter;

@Getter
public enum EEvolutionRarityType {
    
    COMMON("Common", "<white>", 0.1, 1.0, 1),
    UNCOMMON("Uncommon", "<green>", 0.2, 1.5, 2),
    RARE("Rare", "<blue>", 0.5, 2.0, 5),
    EPIC("Epic", "<dark_purple>", 1.0, 3.0, 10),
    LEGENDARY("Legendary", "<gold>", 2.0, 5.0, 25),
    SPECIAL("Special", "<red>", 5.0, 8.0, 50),
    UNIQUE("Unique", "<aqua>", 10.0, 12.0, 100),
    MYTHICAL("Mythical", "<light_purple>", 20.0, 20.0, 250),
    DIVINE("Divine", "<yellow>", 50.0, 35.0, 500),
    CELESTIAL("Celestial", "<dark_aqua>", 100.0, 50.0, 1000),
    TRANSCENDENT("Transcendent", "<dark_blue>", 250.0, 75.0, 2500),
    ETHEREAL("Ethereal", "<gray>", 500.0, 100.0, 5000),
    COSMIC("Cosmic", "<dark_red>", 1000.0, 150.0, 10000),
    INFINITE("Infinite", "<dark_gray>", 2500.0, 250.0, 25000),
    OMNIPOTENT("Omnipotent", "<black>", 5000.0, 500.0, 50000),
    RESERVED("Reserved", "<obfuscated>", 10000.0, 1000.0, 100000);

    private final String displayName;
    private final String colorCode;
    private final double passiveXpValue;
    private final double dropRateMultiplier;
    private final int prestigePointValue;
    
    EEvolutionRarityType(String displayName, String colorCode, double passiveXpValue, 
                        double dropRateMultiplier, int prestigePointValue) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.passiveXpValue = passiveXpValue;
        this.dropRateMultiplier = dropRateMultiplier;
        this.prestigePointValue = prestigePointValue;
    }
    
    public String getFormattedName() {
        return colorCode + displayName;
    }
    
    public int getTier() {
        return this.ordinal();
    }
    
    public int getLevel() {
        return this.ordinal();
    }
    
    public boolean isHigherThan(EEvolutionRarityType other) {
        return this.ordinal() > other.ordinal();
    }
    
    public EEvolutionRarityType getNextTier() {
        EEvolutionRarityType[] rarities = EEvolutionRarityType.values();
        int currentIndex = this.ordinal();
        return currentIndex < rarities.length - 1 ? rarities[currentIndex + 1] : null;
    }
    
    public EEvolutionRarityType getPreviousTier() {
        int currentIndex = this.ordinal();
        return currentIndex > 0 ? EEvolutionRarityType.values()[currentIndex - 1] : null;
    }
    
    public static EEvolutionRarityType getByTier(int tier) {
        EEvolutionRarityType[] rarities = EEvolutionRarityType.values();
        return tier >= 0 && tier < rarities.length ? rarities[tier] : COMMON;
    }
    
    public boolean isMaxRarity() {
        return this == RESERVED;
    }
    
    public EEvolutionRarityType getNext() {
        EEvolutionRarityType[] rarities = EEvolutionRarityType.values();
        int currentIndex = this.ordinal();
        return currentIndex < rarities.length - 1 ? rarities[currentIndex + 1] : this;
    }

}