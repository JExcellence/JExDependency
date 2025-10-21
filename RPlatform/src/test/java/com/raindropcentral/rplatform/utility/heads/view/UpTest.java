package com.raindropcentral.rplatform.utility.heads.view;

import com.raindropcentral.rplatform.utility.heads.EHeadFilter;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.raindropcentral.rplatform.utility.heads.view.HeadViewAssertions.assertBuildsWithTranslationKey;
import static com.raindropcentral.rplatform.utility.heads.view.HeadViewAssertions.assertMetadata;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UpTest {

    private static final String IDENTIFIER = "up";
    private static final UUID UUID = UUID.fromString("45c0cd71-7ca9-2288-4965-2a8ccdf5281c");
    private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDVjMGNkNzE3Y2E5MjI4ODQ5NjUyYThjY2RmNTI4MWE4NjBjYjJlNzk2NDZiNTZhMjJiYzU5MTEwZjM2MWRhIn19fQ==";
    private static final String TRANSLATION_KEY = "head." + IDENTIFIER;

    @Test
    void constructorExposesConstantsViaGetters() {
        final Up head = new Up();

        assertMetadata(head, IDENTIFIER, UUID, TEXTURE, EHeadFilter.INVENTORY);
    }

    @Test
    void translationKeyPrefixedWithHeadNamespace() {
        final Up head = new Up();

        assertEquals(TRANSLATION_KEY, head.getTranslationKey());
    }

    @Test
    void getHeadUsesUpTranslationKey() {
        final Up head = new Up();

        assertBuildsWithTranslationKey(head, UUID, TEXTURE, TRANSLATION_KEY);
    }
}
