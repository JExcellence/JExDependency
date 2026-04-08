package de.jexcellence.oneblock.structure;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class StructurePreview {

    private final String layerName;
    private final Material[][] pattern;
    private final Map<Material, Integer> requiredMaterials;

    public StructurePreview(
            @NotNull String layerName,
            @NotNull Material[][] pattern,
            @NotNull Map<Material, Integer> requiredMaterials
    ) {
        this.layerName = layerName;
        this.pattern = copyPattern(pattern);
        this.requiredMaterials = new HashMap<>(requiredMaterials);
    }

    @NotNull
    private Material[][] copyPattern(@NotNull Material[][] original) {
        var copy = new Material[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = new Material[original[i].length];
            System.arraycopy(original[i], 0, copy[i], 0, original[i].length);
        }
        return copy;
    }

    @NotNull
    public String getLayerName() {
        return layerName;
    }

    @NotNull
    public Material[][] getPattern() {
        return copyPattern(pattern);
    }

    @NotNull
    public Map<Material, Integer> getRequiredMaterials() {
        return new HashMap<>(requiredMaterials);
    }

    public int getWidth() {
        return pattern.length > 0 ? pattern[0].length : 0;
    }

    public int getDepth() {
        return pattern.length;
    }

    public int getBlockCount() {
        var count = 0;
        for (var row : pattern) {
            for (var material : row) {
                if (material != Material.AIR) {
                    count++;
                }
            }
        }
        return count;
    }

    @NotNull
    public Material getMaterialAt(int x, int z) {
        if (x >= 0 && x < getWidth() && z >= 0 && z < getDepth()) {
            return pattern[z][x];
        }
        return Material.AIR;
    }

    @NotNull
    public String getTextRepresentation() {
        var sb = new StringBuilder();
        sb.append("§6").append(layerName).append("§r ");
        
        for (int z = 0; z < getDepth(); z++) {
            for (int x = 0; x < getWidth(); x++) {
                var material = pattern[z][x];
                if (material == Material.AIR) {
                    sb.append("§8·");
                } else {
                    sb.append("§f").append(getMaterialSymbol(material));
                }
            }
            sb.append(" ");
        }
        
        return sb.toString();
    }

    @NotNull
    private String getMaterialSymbol(@NotNull Material material) {
        return switch (material) {
            case COBBLESTONE -> "C";
            case STONE -> "S";
            case IRON_BLOCK -> "I";
            case GOLD_BLOCK -> "G";
            case DIAMOND_BLOCK -> "D";
            case EMERALD_BLOCK -> "E";
            case NETHERITE_BLOCK -> "N";
            case WATER -> "W";
            case LAVA -> "L";
            case BEACON -> "B";
            case HOPPER -> "H";
            case CHEST -> "T";
            case FURNACE -> "F";
            case REDSTONE_BLOCK -> "R";
            case OBSIDIAN -> "O";
            default -> "?";
        };
    }

    @NotNull
    public String getRequiredMaterialsList() {
        var sb = new StringBuilder();
        sb.append("§6Required Materials: ");
        
        for (var entry : requiredMaterials.entrySet()) {
            var material = entry.getKey();
            var count = entry.getValue();
            
            sb.append("§7- §f").append(formatMaterialName(material))
              .append(" §7x").append(count).append(" ");
        }
        
        return sb.toString();
    }

    @NotNull
    private String formatMaterialName(@NotNull Material material) {
        var name = material.name().toLowerCase().replace('_', ' ');
        var formatted = new StringBuilder();
        
        var capitalizeNext = true;
        for (var c : name.toCharArray()) {
            if (c == ' ') {
                formatted.append(' ');
                capitalizeNext = true;
            } else if (capitalizeNext) {
                formatted.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                formatted.append(c);
            }
        }
        
        return formatted.toString();
    }

    public boolean containsMaterial(@NotNull Material material) {
        return requiredMaterials.containsKey(material);
    }

    public int getMaterialCount(@NotNull Material material) {
        return requiredMaterials.getOrDefault(material, 0);
    }

    @Override
    public String toString() {
        return "StructurePreview{" +
                "layerName='" + layerName + '\'' +
                ", size=" + getWidth() + "x" + getDepth() +
                ", blocks=" + getBlockCount() +
                ", materials=" + requiredMaterials.size() +
                '}';
    }
}