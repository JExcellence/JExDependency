package com.raindropcentral.rdq.quest.reward;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.reward.BaseReward;
import com.raindropcentral.rdq.database.repository.RRewardRepository;
import com.raindropcentral.rplatform.config.icon.IconSection;
import com.raindropcentral.rplatform.reward.AbstractReward;
import com.raindropcentral.rplatform.reward.impl.CommandReward;
import com.raindropcentral.rplatform.reward.impl.CurrencyReward;
import com.raindropcentral.rplatform.reward.impl.ExperienceReward;
import com.raindropcentral.rplatform.reward.impl.ExperienceReward.ExperienceType;
import com.raindropcentral.rplatform.reward.impl.ItemReward;
import com.raindropcentral.rplatform.reward.impl.TitleReward;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Factory for creating BaseReward entities from quest reward configurations.
 * <p>
 * This factory converts reward sections from YAML quest definitions into
 * concrete BaseReward entities that can be persisted to the database and
 * distributed to players.
 *
 * @author RaindropCentral
 * @version 2.0.0
 */
public class QuestRewardFactory {
    
    private static final Logger LOGGER = Logger.getLogger(QuestRewardFactory.class.getName());
    
    private final RDQ plugin;
    private final RRewardRepository rewardRepository;
    
    /**
     * Constructs a new quest reward factory.
     *
     * @param plugin the RDQ plugin instance
     */
    public QuestRewardFactory(@NotNull final RDQ plugin) {
        this.plugin = plugin;
        this.rewardRepository = plugin.getRewardRepository();
    }
    
    /**
     * Creates a BaseReward entity from a reward configuration map.
     * <p>
     * Note: IconSection is optional. If not present in rewardData, a default icon
     * will be generated based on the reward type.
     *
     * @param rewardKey  the reward key from YAML
     * @param rewardData the reward configuration map
     * @return the created BaseReward entity, or null if creation failed
     */
    @Nullable
    public BaseReward createFromMap(
            @NotNull final String rewardKey,
            @NotNull final Map<String, Object> rewardData
    ) {
        try {
            final Object typeObj = rewardData.get("type");
            if (typeObj == null) {
                LOGGER.warning("Reward " + rewardKey + " has no type specified");
                return null;
            }
            
            final String type = typeObj.toString();
            
            AbstractReward abstractReward = switch (type.toUpperCase()) {
                case "CURRENCY" -> createCurrencyReward(rewardData);
                case "ITEM" -> createItemReward(rewardData);
                case "EXPERIENCE" -> createExperienceReward(rewardData);
                case "COMMAND" -> createCommandReward(rewardData);
                case "TITLE" -> createTitleReward(rewardData);
                default -> {
                    LOGGER.warning("Unknown reward type: " + type);
                    yield null;
                }
            };
            
            if (abstractReward != null) {
                // Get icon from config if present, otherwise generate default
                IconSection icon = (IconSection) rewardData.get("icon");
                
                if (icon == null) {
                    LOGGER.fine("Reward " + rewardKey + " has no icon section - generating default icon");
                    icon = createDefaultIcon(type, rewardData);
                }
                
                // Wrap in BaseReward entity
                final BaseReward baseReward = new BaseReward(abstractReward, icon);
                
                // Save to database
                final BaseReward saved = rewardRepository.create(baseReward);
                LOGGER.fine("Created " + type + " reward: " + rewardKey);
                
                return saved;
            }
            
            return null;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create reward: " + rewardKey, e);
            return null;
        }
    }
    
    /**
     * Creates a currency reward.
     */
    @NotNull
    private AbstractReward createCurrencyReward(@NotNull final Map<String, Object> data) {
        final String currencyId = data.getOrDefault("currency", "coins").toString();
        final Object amountObj = data.get("amount");
        final double amount = amountObj instanceof Number ? ((Number) amountObj).doubleValue() : 0.0;
        
        return new CurrencyReward(currencyId, amount);
    }
    
    /**
     * Creates an item reward.
     */
    @NotNull
    private AbstractReward createItemReward(@NotNull final Map<String, Object> data) {
        final String materialStr = data.getOrDefault("material", "DIAMOND").toString();
        final Object amountObj = data.get("amount");
        final int amount = amountObj instanceof Number ? ((Number) amountObj).intValue() : 1;
        
        // Parse material
        final Material material;
        try {
            material = Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid material: " + materialStr + ", using DIAMOND");
            return new ItemReward(new ItemStack(Material.DIAMOND, amount));
        }
        
        // Create base ItemStack
        final ItemStack item = new ItemStack(material, amount);
        
        // Apply name and lore if present
        if (data.containsKey("name") || data.containsKey("lore")) {
            final ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                final MiniMessage mm = MiniMessage.miniMessage();
                
                // Set display name
                if (data.containsKey("name")) {
                    final String name = data.get("name").toString();
                    meta.displayName(mm.deserialize(name));
                }
                
                // Set lore
                if (data.containsKey("lore")) {
                    @SuppressWarnings("unchecked")
                    final List<String> loreStrings = (List<String>) data.get("lore");
                    final List<Component> lore = loreStrings.stream()
                            .map(mm::deserialize)
                            .collect(Collectors.toList());
                    meta.lore(lore);
                }
                
                item.setItemMeta(meta);
            }
        }
        
        return new ItemReward(item, amount);
    }
    
    /**
     * Creates an experience reward.
     */
    @NotNull
    private AbstractReward createExperienceReward(@NotNull final Map<String, Object> data) {
        final Object amountObj = data.get("amount");
        final int amount = amountObj instanceof Number ? ((Number) amountObj).intValue() : 0;
        
        final String typeStr = data.getOrDefault("type", "POINTS").toString();
        final ExperienceType type = "LEVELS".equalsIgnoreCase(typeStr) 
                ? ExperienceType.LEVELS 
                : ExperienceType.POINTS;
        
        return new ExperienceReward(amount, type);
    }
    
    /**
     * Creates a command reward.
     */
    @NotNull
    private AbstractReward createCommandReward(@NotNull final Map<String, Object> data) {
        final String command = data.getOrDefault("command", "").toString();
        final boolean asPlayer = Boolean.parseBoolean(data.getOrDefault("asPlayer", "false").toString());
        final boolean executeAsPlayer = asPlayer; // executeAsPlayer parameter
        
        final Object delayObj = data.get("delay");
        final long delayTicks = delayObj instanceof Number ? ((Number) delayObj).longValue() : 0L;
        
        return new CommandReward(command, executeAsPlayer, delayTicks);
    }
    
    /**
     * Creates a title reward.
     */
    @NotNull
    private AbstractReward createTitleReward(@NotNull final Map<String, Object> data) {
        final String titleId = data.getOrDefault("titleId", "").toString();
        final String displayName = data.getOrDefault("displayName", titleId).toString();
        
        return new TitleReward(titleId, displayName);
    }
    
    /**
     * Creates a default icon for a reward based on its type.
     * <p>
     * This is a temporary solution that creates a simple IconSection without using
     * the config mapper system. The icon will have basic properties but won't support
     * advanced features like expressions or dynamic values.
     *
     * @param type the reward type
     * @param rewardData the reward configuration data
     * @return a default IconSection
     */
    @NotNull
    private IconSection createDefaultIcon(
            @NotNull final String type,
            @NotNull final Map<String, Object> rewardData
    ) {
        // Create a minimal EvaluationEnvironmentBuilder
        // This is a workaround since IconSection requires it
        final de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder envBuilder =
                new de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder();
        
        final IconSection icon = new IconSection(envBuilder);
        
        // Set material based on reward type
        final Material material = switch (type.toUpperCase()) {
            case "CURRENCY" -> Material.GOLD_INGOT;
            case "ITEM" -> {
                if (rewardData.containsKey("material")) {
                    try {
                        yield Material.valueOf(rewardData.get("material").toString().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        yield Material.CHEST;
                    }
                }
                yield Material.CHEST;
            }
            case "EXPERIENCE" -> Material.EXPERIENCE_BOTTLE;
            case "COMMAND" -> Material.COMMAND_BLOCK;
            case "TITLE" -> Material.NAME_TAG;
            default -> Material.PAPER;
        };
        
        icon.setMaterial(material.name());
        icon.setDisplayNameKey("reward." + type.toLowerCase() + ".name");
        icon.setDescriptionKey("reward." + type.toLowerCase() + ".description");
        
        return icon;
    }
}
