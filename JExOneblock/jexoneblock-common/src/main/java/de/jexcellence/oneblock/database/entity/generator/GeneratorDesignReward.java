package de.jexcellence.oneblock.database.entity.generator;

import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

/**
 * Database entity for generator design rewards/bonuses.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
@Entity
@Table(name = "oneblock_generator_design_rewards")
public class GeneratorDesignReward {
    
    public enum RewardType {
        SPEED_BONUS,
        XP_BONUS,
        FORTUNE_BONUS,
        DROP_CHANCE,
        SPECIAL_DROP,
        AUTOMATION_UNLOCK,
        CUSTOM
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    private GeneratorDesign design;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 32)
    private RewardType rewardType;
    
    @Column(name = "name_key", nullable = false, length = 128)
    private String nameKey;
    
    @Column(name = "description_key", nullable = false, length = 128)
    private String descriptionKey;
    
    @Column(name = "reward_value", nullable = false)
    private Double value;
    
    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;
    
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    public GeneratorDesignReward() {}
    
    public GeneratorDesignReward(@NotNull RewardType rewardType, double value) {
        this.rewardType = rewardType;
        this.value = value;
        this.nameKey = "generator.reward." + rewardType.name().toLowerCase() + ".name";
        this.descriptionKey = "generator.reward." + rewardType.name().toLowerCase() + ".description";
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public GeneratorDesign getDesign() { return design; }
    public void setDesign(GeneratorDesign design) { this.design = design; }
    
    public RewardType getRewardType() { return rewardType; }
    public void setRewardType(RewardType rewardType) { this.rewardType = rewardType; }
    
    public String getNameKey() { return nameKey; }
    public void setNameKey(String nameKey) { this.nameKey = nameKey; }
    
    public String getDescriptionKey() { return descriptionKey; }
    public void setDescriptionKey(String descriptionKey) { this.descriptionKey = descriptionKey; }
    
    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }
    
    public String getExtraData() { return extraData; }
    public void setExtraData(String extraData) { this.extraData = extraData; }
    
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    
    public String getFormattedValue() {
        return switch (rewardType) {
            case SPEED_BONUS, XP_BONUS, FORTUNE_BONUS -> String.format("+%.0f%%", value * 100);
            case DROP_CHANCE -> String.format("%.1f%%", value * 100);
            default -> String.valueOf(value);
        };
    }
    
    @Override
    public String toString() {
        return "GeneratorDesignReward{" +
                "rewardType=" + rewardType +
                ", value=" + value +
                '}';
    }
}
