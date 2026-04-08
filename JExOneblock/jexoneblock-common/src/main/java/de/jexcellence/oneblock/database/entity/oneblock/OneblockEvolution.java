package de.jexcellence.oneblock.database.entity.oneblock;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import de.jexcellence.hibernate.entity.BaseEntity;
import de.jexcellence.oneblock.database.entity.evolution.EvolutionBlock;
import de.jexcellence.oneblock.database.entity.evolution.EvolutionEntity;
import de.jexcellence.oneblock.database.entity.evolution.EvolutionItem;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import jakarta.persistence.*;
import lombok.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for OneblockEvolution with modern inheritance
 * Replaces JEIslandStage with evolution-based naming and Lombok optimization
 */
@Entity
@Table(name = "oneblock_evolutions", indexes = {
    @Index(name = "idx_evolution_name", columnList = "evolution_name"),
    @Index(name = "idx_evolution_level", columnList = "level"),
    @Index(name = "idx_evolution_disabled", columnList = "is_disabled")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "evolution_type", discriminatorType = DiscriminatorType.STRING)
@NamedEntityGraphs({
    @NamedEntityGraph(
        name = "Evolution.withAllContent",
        attributeNodes = {
            @NamedAttributeNode("blocks"),
            @NamedAttributeNode("entities"),
            @NamedAttributeNode("items")
        }
    ),
    @NamedEntityGraph(
        name = "Evolution.withBlocks",
        attributeNodes = {
            @NamedAttributeNode("blocks")
        }
    ),
    @NamedEntityGraph(
        name = "Evolution.withEntities",
        attributeNodes = {
            @NamedAttributeNode("entities")
        }
    ),
    @NamedEntityGraph(
        name = "Evolution.withItems",
        attributeNodes = {
            @NamedAttributeNode("items")
        }
    ),
    @NamedEntityGraph(
        name = "Evolution.summary",
        attributeNodes = {}
    )
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "evolutionName", callSuper = false)
public abstract class OneblockEvolution extends BaseEntity {
    
    @Column(name = "evolution_name", nullable = false, unique = true, length = 100)
    private String evolutionName;
    
    @Column(name = "level", nullable = false)
    private int level;
    
    @Column(name = "experience_to_pass", nullable = false)
    private int experienceToPass;
    
    @Column(name = "showcase_material")
    @Enumerated(EnumType.STRING)
    private Material showcase;
    
    @Column(name = "is_disabled", nullable = false)
    private boolean isDisabled = false;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @OneToMany(mappedBy = "evolution", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<EvolutionBlock> blocks = new ArrayList<>();
    
    @OneToMany(mappedBy = "evolution", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<EvolutionEntity> entities = new ArrayList<>();
    
    @OneToMany(mappedBy = "evolution", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<EvolutionItem> items = new ArrayList<>();

    /**
     * Requirements that must be met to advance to this evolution.
     * Stored as JSON in the database for flexibility.
     */
    @Column(name = "requirements_json", columnDefinition = "TEXT")
    private String requirementsJson;

    /**
     * Transient list of parsed requirements (not persisted directly).
     */
    @Transient
    private List<AbstractRequirement> requirements = new ArrayList<>();

    /**
     * Constructor for basic evolution creation
     * @param evolutionName the name of the evolution
     * @param level the evolution level
     * @param experienceToPass experience required to pass this evolution
     */
    public OneblockEvolution(String evolutionName, int level, int experienceToPass) {
        this.evolutionName = evolutionName;
        this.level = level;
        this.experienceToPass = experienceToPass;
        this.isDisabled = false;
        this.blocks = new ArrayList<>();
        this.entities = new ArrayList<>();
        this.items = new ArrayList<>();
        this.requirements = new ArrayList<>();
    }
    
    // ==================== Getter/Setter Methods ====================
    
    /**
     * Gets the evolution name
     * @return the evolution name
     */
    public String getEvolutionName() {
        return evolutionName;
    }
    
    /**
     * Sets the evolution name
     * @param evolutionName the evolution name
     */
    public void setEvolutionName(String evolutionName) {
        this.evolutionName = evolutionName;
    }
    
    /**
     * Gets the evolution level
     * @return the evolution level
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Sets the evolution level
     * @param level the evolution level
     */
    public void setLevel(int level) {
        this.level = level;
    }
    
    /**
     * Gets the experience required to pass this evolution
     * @return the experience to pass
     */
    public int getExperienceToPass() {
        return experienceToPass;
    }
    
    /**
     * Sets the experience required to pass this evolution
     * @param experienceToPass the experience to pass
     */
    public void setExperienceToPass(int experienceToPass) {
        this.experienceToPass = experienceToPass;
    }
    
    /**
     * Gets the showcase material
     * @return the showcase material
     */
    public Material getShowcase() {
        return showcase;
    }
    
    /**
     * Sets the showcase material
     * @param showcase the showcase material
     */
    public void setShowcase(Material showcase) {
        this.showcase = showcase;
    }
    
    /**
     * Gets the description
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the description
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Checks if this evolution is disabled
     * @return true if disabled
     */
    public boolean isDisabled() {
        return isDisabled;
    }
    
    /**
     * Sets the disabled status
     * @param disabled the disabled status
     */
    public void setDisabled(boolean disabled) {
        this.isDisabled = disabled;
    }
    
    // ==================== Requirement Methods ====================
    
    /**
     * Gets the requirements for this evolution
     * @return list of requirements
     */
    public @NotNull List<AbstractRequirement> getRequirements() {
        if (requirements == null) {
            requirements = new ArrayList<>();
        }
        return requirements;
    }
    
    /**
     * Sets the requirements for this evolution
     * @param requirements the requirements to set
     */
    public void setRequirements(@NotNull List<AbstractRequirement> requirements) {
        this.requirements = new ArrayList<>(requirements);
    }
    
    /**
     * Adds a requirement to this evolution
     * @param requirement the requirement to add
     */
    public void addRequirement(@NotNull AbstractRequirement requirement) {
        if (requirements == null) {
            requirements = new ArrayList<>();
        }
        requirements.add(requirement);
    }
    
    /**
     * Checks if this evolution has any requirements
     * @return true if evolution has requirements
     */
    public boolean hasRequirements() {
        return requirements != null && !requirements.isEmpty();
    }
    
    /**
     * Checks if a player meets all requirements for this evolution
     * @param player the player to check
     * @return true if all requirements are met
     */
    public boolean areRequirementsMet(@NotNull Player player) {
        if (!hasRequirements()) {
            return true;
        }
        return requirements.stream().allMatch(req -> req.isMet(player));
    }
    
    /**
     * Calculates the overall progress towards meeting all requirements
     * @param player the player to check
     * @return progress value between 0.0 and 1.0
     */
    public double calculateRequirementProgress(@NotNull Player player) {
        if (!hasRequirements()) {
            return 1.0;
        }
        return requirements.stream()
            .mapToDouble(req -> req.calculateProgress(player))
            .average()
            .orElse(1.0);
    }
    
    /**
     * Consumes all requirements for this evolution
     * @param player the player to consume from
     */
    public void consumeRequirements(@NotNull Player player) {
        if (!hasRequirements()) {
            return;
        }
        requirements.forEach(req -> req.consume(player));
    }
    
    /**
     * Gets the JSON representation of requirements
     * @return JSON string of requirements
     */
    public String getRequirementsJson() {
        return requirementsJson;
    }
    
    /**
     * Sets the JSON representation of requirements
     * @param requirementsJson JSON string of requirements
     */
    public void setRequirementsJson(String requirementsJson) {
        this.requirementsJson = requirementsJson;
    }
    
    // Content retrieval methods by rarity using modern Java features
    
    /**
     * Gets all blocks for a specific rarity level using modern Java streams
     * @param rarity the rarity to filter by
     * @return list of materials for the specified rarity
     */
    public @NotNull List<Material> getBlocksByRarity(@NotNull EEvolutionRarityType rarity) {
        return blocks.stream()
            .filter(block -> block.getRarity() == rarity && block.isValid())
            .flatMap(block -> block.getMaterials().stream())
            .distinct()
            .toList();
    }
    
    /**
     * Gets all items for a specific rarity level using modern Java streams
     * @param rarity the rarity to filter by
     * @return list of ItemStacks for the specified rarity
     */
    public @NotNull List<ItemStack> getItemsByRarity(@NotNull EEvolutionRarityType rarity) {
        return items.stream()
            .filter(item -> item.getRarity() == rarity && item.isValid())
            .flatMap(item -> item.getItemStacks().stream())
            .map(ItemStack::clone)
            .toList();
    }
    
    /**
     * Gets all entity spawn eggs for a specific rarity level using modern Java streams
     * @param rarity the rarity to filter by
     * @return list of spawn egg materials for the specified rarity
     */
    public @NotNull List<Material> getEntitiesByRarity(@NotNull EEvolutionRarityType rarity) {
        return entities.stream()
            .filter(entity -> entity.getRarity() == rarity && entity.isValid())
            .flatMap(entity -> entity.getSpawnEggs().stream())
            .distinct()
            .toList();
    }
    
    // Content management methods
    
    /**
     * Adds a block configuration to this evolution
     * @param evolutionBlock the block configuration to add
     */
    public void addBlock(@NotNull EvolutionBlock evolutionBlock) {
        evolutionBlock.setEvolution(this);
        this.blocks.add(evolutionBlock);
    }
    
    /**
     * Adds an entity configuration to this evolution
     * @param evolutionEntity the entity configuration to add
     */
    public void addEntity(@NotNull EvolutionEntity evolutionEntity) {
        evolutionEntity.setEvolution(this);
        this.entities.add(evolutionEntity);
    }
    
    /**
     * Adds an item configuration to this evolution
     * @param evolutionItem the item configuration to add
     */
    public void addItem(@NotNull EvolutionItem evolutionItem) {
        evolutionItem.setEvolution(this);
        this.items.add(evolutionItem);
    }
    
    /**
     * Checks if this evolution has content for a specific rarity using modern predicates
     * @param rarity the rarity to check
     * @return true if evolution has blocks, entities, or items for this rarity
     */
    public boolean hasContentForRarity(@NotNull EEvolutionRarityType rarity) {
        return blocks.stream().anyMatch(block -> block.getRarity() == rarity && block.isValid()) ||
               entities.stream().anyMatch(entity -> entity.getRarity() == rarity && entity.isValid()) ||
               items.stream().anyMatch(item -> item.getRarity() == rarity && item.isValid());
    }
    
    /**
     * Checks if this evolution is ready for use with enhanced validation
     * @return true if evolution has at least one valid content type and is not disabled
     */
    public boolean isReady() {
        return !isDisabled && 
               (blocks.stream().anyMatch(EvolutionBlock::isValid) ||
                entities.stream().anyMatch(EvolutionEntity::isValid) ||
                items.stream().anyMatch(EvolutionItem::isValid));
    }
    
    /**
     * Gets a random block material from this evolution
     * @return Optional containing a random material, or empty if no blocks available
     */
    public @NotNull java.util.Optional<Material> getRandomBlock() {
        List<Material> allBlocks = new ArrayList<>();
        for (EEvolutionRarityType rarity : EEvolutionRarityType.values()) {
            allBlocks.addAll(getBlocksByRarity(rarity));
        }
        if (allBlocks.isEmpty()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(allBlocks.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(allBlocks.size())));
    }
    
    @Override
    public String toString() {
        return "OneblockEvolution{" +
                "evolutionName='" + evolutionName + '\'' +
                ", level=" + level +
                ", experienceToPass=" + experienceToPass +
                ", isDisabled=" + isDisabled +
                ", isReady=" + isReady() +
                '}';
    }
}