package com.raindropcentral.rdq.view.rank.grid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class GridPositionTest {

    @Test
    void offsetProducesExpectedCoordinates() {
        final GridPosition origin = new GridPosition(2, 5);

        final GridPosition offset = origin.offset(-1, 3);

        assertEquals(2, origin.x(), "offset should not modify the original x coordinate");
        assertEquals(5, origin.y(), "offset should not modify the original y coordinate");
        assertEquals(1, offset.x(), "offset should adjust the x coordinate by deltaX");
        assertEquals(8, offset.y(), "offset should adjust the y coordinate by deltaY");
    }

    @Test
    void distanceToComputesEuclideanDistance() {
        final GridPosition origin = new GridPosition(0, 0);
        final GridPosition other = new GridPosition(3, 4);

        assertEquals(5.0, origin.distanceTo(other), 1.0e-9,
                "distanceTo should compute the Euclidean distance between positions");
        assertEquals(origin.distanceTo(other), other.distanceTo(origin), 1.0e-9,
                "distanceTo should be symmetric");
    }

    @Test
    void equalsAndHashCodeHonorCoordinatePairs() {
        final GridPosition first = new GridPosition(7, -2);
        final GridPosition same = new GridPosition(7, -2);
        final GridPosition differentX = new GridPosition(8, -2);
        final GridPosition differentY = new GridPosition(7, -1);

        assertEquals(first, same, "equals should match positions with identical coordinates");
        assertEquals(first.hashCode(), same.hashCode(),
                "hashCode should match for positions with identical coordinates");
        assertNotEquals(first, differentX, "equals should detect differing x coordinates");
        assertNotEquals(first, differentY, "equals should detect differing y coordinates");
        assertNotEquals(first.hashCode(), differentX.hashCode(),
                "hashCode should reflect differing x coordinates");
        assertNotEquals(first.hashCode(), differentY.hashCode(),
                "hashCode should reflect differing y coordinates");
    }
}
