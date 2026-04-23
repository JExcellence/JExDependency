package de.jexcellence.core.api.requirement;

import de.jexcellence.core.api.requirement.Requirement.Comparator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequirementComparatorTest {

    @Test
    void numericComparisons() {
        assertTrue(Comparator.LT.compare(1.0, 2.0));
        assertFalse(Comparator.LT.compare(2.0, 1.0));
        assertTrue(Comparator.LE.compare(2.0, 2.0));
        assertTrue(Comparator.EQ.compare(3.0, 3.0));
        assertFalse(Comparator.EQ.compare(3.0, 3.1));
        assertTrue(Comparator.NE.compare(3.0, 3.1));
        assertTrue(Comparator.GE.compare(5.0, 5.0));
        assertTrue(Comparator.GT.compare(5.0, 4.9));
    }

    @Test
    void stringComparisons() {
        assertTrue(Comparator.EQ.compare("alpha", "alpha"));
        assertFalse(Comparator.EQ.compare("alpha", "Alpha"));
        assertTrue(Comparator.LT.compare("alpha", "beta"));
        assertTrue(Comparator.GT.compare("beta", "alpha"));
        assertTrue(Comparator.NE.compare("alpha", "beta"));
    }
}
