package com.raindropcentral.rplatform.head;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HeadCategoryTest {

    @Test
    void valuesExposeDocumentedCategories() {
        HeadCategory[] expectedOrder = {
            HeadCategory.DECORATION,
            HeadCategory.INVENTORY,
            HeadCategory.PLAYER
        };

        assertArrayEquals(expectedOrder, HeadCategory.values(),
            "The enumeration order documents the public categories surfaced in configuration and UI documentation.");

        assertEquals(3, HeadCategory.values().length, "Adding a new category requires updating any UI documentation and translations.");
    }

    @Test
    void valueOfResolvesAllCategories() {
        assertSame(HeadCategory.DECORATION, HeadCategory.valueOf("DECORATION"));
        assertSame(HeadCategory.INVENTORY, HeadCategory.valueOf("INVENTORY"));
        assertSame(HeadCategory.PLAYER, HeadCategory.valueOf("PLAYER"));
    }

    @Test
    void valueOfRejectsUnknownCategories() {
        assertThrows(IllegalArgumentException.class, () -> HeadCategory.valueOf("UNKNOWN"));
    }

    @Test
    void categoriesAreEnumeratedInLookupOrder() {
        Map<String, HeadCategory> documentedMappings = Map.of(
            "decoration", HeadCategory.DECORATION,
            "inventory", HeadCategory.INVENTORY,
            "player", HeadCategory.PLAYER
        );

        documentedMappings.forEach((key, category) -> assertSame(category, HeadCategory.valueOf(key.toUpperCase()),
            () -> "Documentation key '" + key + "' should map to " + category + "."));

        assertEquals(EnumSet.allOf(HeadCategory.class), EnumSet.copyOf(documentedMappings.values()),
            "All documented keys must cover the complete category set.");
    }
}
