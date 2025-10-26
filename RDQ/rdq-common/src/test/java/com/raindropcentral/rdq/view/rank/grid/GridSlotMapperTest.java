package com.raindropcentral.rdq.view.rank.grid;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridSlotMapperTest {

    @Test
    void getSlotForPositionAndGetPositionForSlotAreInverses() {
        final Map<GridPosition, Integer> slotMapping = GridSlotMapper.getSlotMapping();

        for (final Map.Entry<GridPosition, Integer> entry : slotMapping.entrySet()) {
            final GridPosition position = entry.getKey();
            final Integer slot = entry.getValue();

            assertEquals(slot, GridSlotMapper.getSlotForPosition(position),
                    "Slot lookup should return the configured slot number");
            assertEquals(position, GridSlotMapper.getPositionForSlot(slot),
                    "Position lookup should return the original grid position");
        }
    }

    @Test
    void getValidSlotsAndMappingAreImmutableViews() {
        final List<Integer> validSlots = GridSlotMapper.getValidSlots();
        assertThrows(UnsupportedOperationException.class, () -> validSlots.add(99),
                "Valid slot list should be immutable");

        final Map<GridPosition, Integer> slotMapping = GridSlotMapper.getSlotMapping();
        assertThrows(UnsupportedOperationException.class,
                () -> slotMapping.put(new GridPosition(9, 9), 99),
                "Slot mapping should be immutable");
    }

    @Test
    void isValidSlotIdentifiesValidAndInvalidSlots() {
        final List<Integer> validSlots = GridSlotMapper.getValidSlots();

        for (final Integer slot : validSlots) {
            assertTrue(GridSlotMapper.isValidSlot(slot), "Expected slot to be flagged as valid");
        }

        for (final int invalidSlot : List.of(0, 4, 8, 17, 26, 35, 44, 50)) {
            assertFalse(GridSlotMapper.isValidSlot(invalidSlot),
                    "Unexpected slot flagged as valid: " + invalidSlot);
        }
    }
}
