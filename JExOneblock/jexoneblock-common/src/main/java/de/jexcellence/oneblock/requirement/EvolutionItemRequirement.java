package de.jexcellence.oneblock.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Evolution Item Requirement - requires items for evolution advancement.
 * Extends RPlatform's AbstractRequirement for unified requirement handling.
 *
 * @author JExcellence
 * @version 2.1.0
 * @since 2.0.0
 */
@JsonTypeName("EVOLUTION_ITEM")
public class EvolutionItemRequirement extends AbstractRequirement {

    private static final Logger LOGGER = Logger.getLogger("JExOneblock");

    @JsonProperty("requiredItems")
    private final List<ItemStack> requiredItems;

    @JsonProperty("exactMatch")
    private final boolean exactMatch;

    @JsonProperty("checkStorage")
    private final boolean checkStorage;

    @JsonProperty("evolutionName")
    @Nullable
    private final String evolutionName;

    @JsonCreator
    public EvolutionItemRequirement(
            @JsonProperty("requiredItems") @NotNull List<ItemStack> requiredItems,
            @JsonProperty("exactMatch") @Nullable Boolean exactMatch,
            @JsonProperty("checkStorage") @Nullable Boolean checkStorage,
            @JsonProperty("evolutionName") @Nullable String evolutionName,
            @JsonProperty("consumeOnComplete") @Nullable Boolean consumeOnComplete
    ) {
        super("ITEM", consumeOnComplete != null ? consumeOnComplete : true);
        this.requiredItems = new ArrayList<>(requiredItems);
        this.exactMatch = exactMatch != null ? exactMatch : false;
        this.checkStorage = checkStorage != null ? checkStorage : false;
        this.evolutionName = evolutionName;
    }

    public EvolutionItemRequirement(@NotNull List<ItemStack> requiredItems) {
        this(requiredItems, false, false, null, true);
    }

    public EvolutionItemRequirement(@NotNull Material material, int amount) {
        this(List.of(new ItemStack(material, amount)), false, false, null, true);
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        for (ItemStack required : requiredItems) {
            if (getAvailableAmount(player, required) < required.getAmount()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        if (requiredItems.isEmpty()) return 1.0;

        double totalProgress = 0;
        for (ItemStack required : requiredItems) {
            int available = getAvailableAmount(player, required);
            totalProgress += Math.min(1.0, (double) available / required.getAmount());
        }
        return totalProgress / requiredItems.size();
    }

    @Override
    public void consume(@NotNull Player player) {
        if (!shouldConsume()) return;

        for (ItemStack required : requiredItems) {
            int toConsume = required.getAmount();

            // Consume from inventory first
            int consumed = consumeFromInventory(player, required, toConsume);
            toConsume -= consumed;

            // Then from storage if enabled
            if (toConsume > 0 && checkStorage) {
                consumeFromStorage(player, required, toConsume);
            }
        }
    }

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "evolution.requirement.item";
    }

    private int getAvailableAmount(@NotNull Player player, @NotNull ItemStack required) {
        int total = getInventoryAmount(player, required);
        if (checkStorage) {
            total += getStorageAmount(player, required);
        }
        return total;
    }

    private int getInventoryAmount(@NotNull Player player, @NotNull ItemStack required) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && itemMatches(item, required)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private int getStorageAmount(@NotNull Player player, @NotNull ItemStack required) {
        // Would integrate with OneBlock storage system
        return 0;
    }

    private int consumeFromInventory(@NotNull Player player, @NotNull ItemStack required, int amount) {
        int remaining = amount;

        for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && itemMatches(item, required)) {
                int toTake = Math.min(remaining, item.getAmount());
                if (toTake >= item.getAmount()) {
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - toTake);
                }
                remaining -= toTake;
            }
        }

        return amount - remaining;
    }

    private void consumeFromStorage(@NotNull Player player, @NotNull ItemStack required, int amount) {
        // Would integrate with OneBlock storage system
        LOGGER.fine("Storage consumption not yet implemented");
    }

    private boolean itemMatches(@NotNull ItemStack item, @NotNull ItemStack required) {
        if (exactMatch) {
            return item.isSimilar(required);
        }
        return item.getType() == required.getType();
    }

    @NotNull
    public List<ItemStack> getRequiredItems() {
        return new ArrayList<>(requiredItems);
    }

    public boolean isExactMatch() {
        return exactMatch;
    }

    public boolean isCheckStorage() {
        return checkStorage;
    }

    @Nullable
    public String getEvolutionName() {
        return evolutionName;
    }

    @Override
    public String toString() {
        return "EvolutionItemRequirement{items=" + requiredItems.size() +
                ", exactMatch=" + exactMatch + ", evolution='" + evolutionName + "'}";
    }
}
