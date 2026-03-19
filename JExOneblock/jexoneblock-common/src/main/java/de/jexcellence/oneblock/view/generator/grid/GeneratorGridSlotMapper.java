package de.jexcellence.oneblock.view.generator.grid;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Utility class for mapping between generator grid positions and inventory slot numbers.
 * Adapted from RDQ's GridSlotMapper but designed for generator structure visualization.
 */
public class GeneratorGridSlotMapper {
    
    private static final Map<GeneratorGridPosition, Integer> STRUCTURE_SLOT_MAPPING = createStructureSlotMapping();
    private static final List<Integer> ALL_STRUCTURE_SLOT_NUMBERS = createAllStructureSlotNumbers();
    
    // Navigation slot constants (matching RDQ pattern)
    public static final int NAVIGATION_LEFT_SLOT = 18;
    public static final int NAVIGATION_RIGHT_SLOT = 26;
    public static final int NAVIGATION_UP_SLOT = 4;
    public static final int NAVIGATION_DOWN_SLOT = 49;
    public static final int BACK_BUTTON_SLOT = 45;
    public static final int CENTER_VIEW_SLOT = 53;
    public static final int LAYER_UP_SLOT = 8;
    public static final int LAYER_DOWN_SLOT = 44;
    public static final int INFO_SLOT = 0;
    
    /**
     * Gets the inventory slot number for a given grid position.
     */
    public static @Nullable Integer getSlotForPosition(
        final @NotNull GeneratorGridPosition position
    ) {
        return STRUCTURE_SLOT_MAPPING.get(position);
    }
    
    /**
     * Gets the grid position for a given inventory slot number.
     */
    public static @Nullable GeneratorGridPosition getPositionForSlot(
        final int slotNumber
    ) {
        for (final Map.Entry<GeneratorGridPosition, Integer> entry : STRUCTURE_SLOT_MAPPING.entrySet()) {
            if (entry.getValue().equals(slotNumber)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * Gets all slot numbers that can contain structure content.
     */
    public static @NotNull List<Integer> getAllStructureSlotNumbers() {
        return new ArrayList<>(ALL_STRUCTURE_SLOT_NUMBERS);
    }
    
    /**
     * Gets a copy of the complete slot mapping.
     */
    public static @NotNull Map<GeneratorGridPosition, Integer> getSlotMapping() {
        return new HashMap<>(STRUCTURE_SLOT_MAPPING);
    }
    
    /**
     * Checks if a slot number is a valid structure content slot.
     */
    public static boolean isValidStructureSlot(
        final int slotNumber
    ) {
        return ALL_STRUCTURE_SLOT_NUMBERS.contains(slotNumber);
    }
    
    /**
     * Checks if a slot is reserved for navigation/UI elements.
     */
    public static boolean isNavigationSlot(
        final int slotNumber
    ) {
        return slotNumber == NAVIGATION_LEFT_SLOT ||
               slotNumber == NAVIGATION_RIGHT_SLOT ||
               slotNumber == NAVIGATION_UP_SLOT ||
               slotNumber == NAVIGATION_DOWN_SLOT ||
               slotNumber == BACK_BUTTON_SLOT ||
               slotNumber == CENTER_VIEW_SLOT ||
               slotNumber == LAYER_UP_SLOT ||
               slotNumber == LAYER_DOWN_SLOT ||
               slotNumber == INFO_SLOT;
    }
    
    private static @NotNull Map<GeneratorGridPosition, Integer> createStructureSlotMapping() {
        final Map<GeneratorGridPosition, Integer> mapping = new HashMap<>();
        
        // Row 0 (top row, excluding navigation slots)
        mapping.put(new GeneratorGridPosition(0, 0), 1);
        mapping.put(new GeneratorGridPosition(1, 0), 2);
        mapping.put(new GeneratorGridPosition(2, 0), 3);
        mapping.put(new GeneratorGridPosition(3, 0), 5);
        mapping.put(new GeneratorGridPosition(4, 0), 6);
        mapping.put(new GeneratorGridPosition(5, 0), 7);
        
        // Row 1
        mapping.put(new GeneratorGridPosition(0, 1), 9);
        mapping.put(new GeneratorGridPosition(1, 1), 10);
        mapping.put(new GeneratorGridPosition(2, 1), 11);
        mapping.put(new GeneratorGridPosition(3, 1), 12);
        mapping.put(new GeneratorGridPosition(4, 1), 13);
        mapping.put(new GeneratorGridPosition(5, 1), 14);
        mapping.put(new GeneratorGridPosition(6, 1), 15);
        mapping.put(new GeneratorGridPosition(7, 1), 16);
        mapping.put(new GeneratorGridPosition(8, 1), 17);
        
        // Row 2 (center row)
        mapping.put(new GeneratorGridPosition(0, 2), 19);
        mapping.put(new GeneratorGridPosition(1, 2), 20);
        mapping.put(new GeneratorGridPosition(2, 2), 21);
        mapping.put(new GeneratorGridPosition(3, 2), 22);
        mapping.put(new GeneratorGridPosition(4, 2), 23);
        mapping.put(new GeneratorGridPosition(5, 2), 24);
        mapping.put(new GeneratorGridPosition(6, 2), 25);
        mapping.put(new GeneratorGridPosition(7, 2), 27);
        
        // Row 3
        mapping.put(new GeneratorGridPosition(0, 3), 28);
        mapping.put(new GeneratorGridPosition(1, 3), 29);
        mapping.put(new GeneratorGridPosition(2, 3), 30);
        mapping.put(new GeneratorGridPosition(3, 3), 31);
        mapping.put(new GeneratorGridPosition(4, 3), 32);
        mapping.put(new GeneratorGridPosition(5, 3), 33);
        mapping.put(new GeneratorGridPosition(6, 3), 34);
        mapping.put(new GeneratorGridPosition(7, 3), 35);
        mapping.put(new GeneratorGridPosition(8, 3), 36);
        
        // Row 4 (bottom content row)
        mapping.put(new GeneratorGridPosition(0, 4), 37);
        mapping.put(new GeneratorGridPosition(1, 4), 38);
        mapping.put(new GeneratorGridPosition(2, 4), 39);
        mapping.put(new GeneratorGridPosition(3, 4), 40);
        mapping.put(new GeneratorGridPosition(4, 4), 41);
        mapping.put(new GeneratorGridPosition(5, 4), 42);
        mapping.put(new GeneratorGridPosition(6, 4), 43);
        mapping.put(new GeneratorGridPosition(7, 4), 46);
        mapping.put(new GeneratorGridPosition(8, 4), 47);
        
        // Row 5 (bottom row)
        mapping.put(new GeneratorGridPosition(0, 5), 48);
        mapping.put(new GeneratorGridPosition(1, 5), 50);
        mapping.put(new GeneratorGridPosition(2, 5), 51);
        mapping.put(new GeneratorGridPosition(3, 5), 52);
        
        return mapping;
    }
    
    private static @NotNull List<Integer> createAllStructureSlotNumbers() {
        final List<Integer> slots = new ArrayList<>();
        
        // Row 0
        slots.addAll(List.of(1, 2, 3, 5, 6, 7));
        // Row 1
        slots.addAll(List.of(9, 10, 11, 12, 13, 14, 15, 16, 17));
        // Row 2
        slots.addAll(List.of(19, 20, 21, 22, 23, 24, 25, 27));
        // Row 3
        slots.addAll(List.of(28, 29, 30, 31, 32, 33, 34, 35, 36));
        // Row 4
        slots.addAll(List.of(37, 38, 39, 40, 41, 42, 43, 46, 47));
        // Row 5
        slots.addAll(List.of(48, 50, 51, 52));
        
        return slots;
    }
}