package com.raindropcentral.rplatform.utility.heads.view;

import com.raindropcentral.rplatform.utility.heads.EHeadFilter;
import com.raindropcentral.rplatform.utility.heads.RHead;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class ProceedTest {

    private static final String IDENTIFIER = "proceed";
    private static final UUID UUID_VALUE = UUID.fromString("afb405c1-16ea-4a23-883f-97867e7db3f9");
    private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTc5YTVjOTVlZTE3YWJmZWY0NWM4ZGMyMjQxODk5NjQ5NDRkNTYwZjE5YTQ0ZjE5ZjhhNDZhZWYzZmVlNDc1NiJ9fX0=";

    @Test
    void constructorAssignsCatalogConstants() {
        final RHead head = new Proceed();

        HeadViewAssertions.assertMetadata(
            head,
            IDENTIFIER,
            UUID_VALUE,
            TEXTURE,
            EHeadFilter.INVENTORY
        );
    }

    @Test
    void getHeadBuildsLocalizedProceedHead() {
        final RHead head = new Proceed();

        HeadViewAssertions.assertBuildsWithTranslationKey(
            head,
            UUID_VALUE,
            TEXTURE,
            "head." + IDENTIFIER
        );
    }
}
