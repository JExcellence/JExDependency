package com.raindropcentral.rplatform.utility.heads.view;

import com.raindropcentral.rplatform.utility.heads.EHeadFilter;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.raindropcentral.rplatform.utility.heads.view.HeadViewAssertions.assertBuildsWithTranslationKey;
import static com.raindropcentral.rplatform.utility.heads.view.HeadViewAssertions.assertMetadata;

class DownTest {

    private static final String IDENTIFIER = "pagination.down";
    private static final UUID UUID = UUID.fromString("7f39ccb5-a5cd-9bab-b130-97b4f8dbc9f1");
    private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2YzOWNjYjVhNWNkOWJhYmIxMzA5N2I0ZjhkYmM5ZjE0ZGMzOGFjMTcwMmEzMGQ5N2UyYWFiZjkwN2JmMTVjIn19fQ==";
    private static final String TRANSLATION_KEY = "head." + IDENTIFIER;

    @Test
    void constructorExposesConstantsViaGetters() {
        final Down head = new Down();

        assertMetadata(head, IDENTIFIER, UUID, TEXTURE, EHeadFilter.INVENTORY);
    }

    @Test
    void getHeadUsesPaginationTranslationKey() {
        final Down head = new Down();

        assertBuildsWithTranslationKey(head, UUID, TEXTURE, TRANSLATION_KEY);
    }
}
