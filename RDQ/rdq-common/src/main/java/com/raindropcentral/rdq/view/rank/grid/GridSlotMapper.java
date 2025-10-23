package com.raindropcentral.rdq.view.rank.grid;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GridSlotMapper {

    private static final Map<GridPosition, Integer> SLOT_MAPPING = createSlotMapping();
    private static final List<Integer> VALID_SLOTS = createValidSlots();

    private GridSlotMapper() {
    }

    public static @Nullable Integer getSlotForPosition(final @NotNull GridPosition position) {
        return SLOT_MAPPING.get(position);
    }

    public static @Nullable GridPosition getPositionForSlot(final int slotNumber) {
        for (final Map.Entry<GridPosition, Integer> entry : SLOT_MAPPING.entrySet()) {
            if (entry.getValue() == slotNumber) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static @NotNull List<Integer> getValidSlots() {
        return Collections.unmodifiableList(VALID_SLOTS);
    }

    public static @NotNull Map<GridPosition, Integer> getSlotMapping() {
        return Collections.unmodifiableMap(SLOT_MAPPING);
    }

    public static boolean isValidSlot(final int slotNumber) {
        return VALID_SLOTS.contains(slotNumber);
    }

    private static @NotNull Map<GridPosition, Integer> createSlotMapping() {
        final Map<GridPosition, Integer> mapping = new HashMap<>();

        mapping.put(new GridPosition(0, 0), 1);
        mapping.put(new GridPosition(1, 0), 2);
        mapping.put(new GridPosition(2, 0), 3);
        mapping.put(new GridPosition(3, 0), 5);
        mapping.put(new GridPosition(4, 0), 6);
        mapping.put(new GridPosition(5, 0), 7);

        mapping.put(new GridPosition(0, 1), 10);
        mapping.put(new GridPosition(1, 1), 11);
        mapping.put(new GridPosition(2, 1), 12);
        mapping.put(new GridPosition(3, 1), 13);
        mapping.put(new GridPosition(4, 1), 14);
        mapping.put(new GridPosition(5, 1), 15);
        mapping.put(new GridPosition(6, 1), 16);

        mapping.put(new GridPosition(0, 2), 19);
        mapping.put(new GridPosition(1, 2), 20);
        mapping.put(new GridPosition(2, 2), 21);
        mapping.put(new GridPosition(3, 2), 22);
        mapping.put(new GridPosition(4, 2), 23);
        mapping.put(new GridPosition(5, 2), 24);
        mapping.put(new GridPosition(6, 2), 25);

        mapping.put(new GridPosition(0, 3), 28);
        mapping.put(new GridPosition(1, 3), 29);
        mapping.put(new GridPosition(2, 3), 30);
        mapping.put(new GridPosition(3, 3), 31);
        mapping.put(new GridPosition(4, 3), 32);
        mapping.put(new GridPosition(5, 3), 33);
        mapping.put(new GridPosition(6, 3), 34);

        mapping.put(new GridPosition(0, 4), 37);
        mapping.put(new GridPosition(1, 4), 38);
        mapping.put(new GridPosition(2, 4), 39);
        mapping.put(new GridPosition(3, 4), 40);
        mapping.put(new GridPosition(4, 4), 41);
        mapping.put(new GridPosition(5, 4), 42);
        mapping.put(new GridPosition(6, 4), 43);

        return mapping;
    }

    private static @NotNull List<Integer> createValidSlots() {
        final List<Integer> slots = new ArrayList<>();
        slots.addAll(List.of(1, 2, 3, 5, 6, 7));
        slots.addAll(List.of(10, 11, 12, 13, 14, 15, 16));
        slots.addAll(List.of(19, 20, 21, 22, 23, 24, 25));
        slots.addAll(List.of(28, 29, 30, 31, 32, 33, 34));
        slots.addAll(List.of(37, 38, 39, 40, 41, 42, 43));
        return slots;
    }
}