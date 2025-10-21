package com.raindropcentral.rplatform.utility.heads;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EHeadFilterTest {

    private static final String SAMPLE_IDENTIFIER = "sample";
    private static final String SAMPLE_UUID = "00000000-0000-0000-0000-000000000000";
    private static final String SAMPLE_TEXTURE = "base64-texture";

    @Test
    void valuesAreStable() {
        assertArrayEquals(
            new EHeadFilter[]{
                EHeadFilter.DECORATION,
                EHeadFilter.INVENTORY,
                EHeadFilter.PLAYER
            },
            EHeadFilter.values(),
            "EHeadFilter constants changed unexpectedly"
        );
    }

    @Test
    void displayNamesAreStable() {
        List<String> expectedNames = Arrays.asList("DECORATION", "INVENTORY", "PLAYER");
        assertEquals(
            expectedNames,
            Arrays.stream(EHeadFilter.values()).map(Enum::name).toList(),
            "EHeadFilter display names changed unexpectedly"
        );
    }

    @Test
    void explicitFilterIsPreserved() {
        TestHead head = new TestHead(
            SAMPLE_IDENTIFIER,
            SAMPLE_UUID,
            SAMPLE_TEXTURE,
            EHeadFilter.DECORATION
        );

        assertEquals(EHeadFilter.DECORATION, head.getFilter());
    }

    @Test
    void defaultFilterFallsBackToInventory() {
        TestHead head = new TestHead(
            SAMPLE_IDENTIFIER,
            SAMPLE_UUID,
            SAMPLE_TEXTURE
        );

        assertEquals(EHeadFilter.INVENTORY, head.getFilter());
    }

    private static final class TestHead extends RHead {

        private TestHead(
            final String identifier,
            final String uuid,
            final String texture,
            final EHeadFilter filter
        ) {
            super(identifier, uuid, texture, filter);
        }

        private TestHead(
            final String identifier,
            final String uuid,
            final String texture
        ) {
            super(identifier, uuid, texture);
        }
    }
}
