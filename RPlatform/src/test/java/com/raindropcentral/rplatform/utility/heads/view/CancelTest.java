package com.raindropcentral.rplatform.utility.heads.view;

import com.raindropcentral.rplatform.utility.heads.EHeadFilter;
import com.raindropcentral.rplatform.utility.heads.HeadTestFixtures;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.raindropcentral.rplatform.utility.heads.HeadAssertions.assertHeadDefinition;

class CancelTest {

    private static final String IDENTIFIER = "cancel";
    private static final UUID EXPECTED_UUID = UUID.fromString("2066a7f4-f2a3-43b3-8425-dfeeb939d334");
    private static final String TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjc1NDgzNjJhMjRjMGZhODQ1M2U0ZDkzZTY4YzU5NjlkZGJkZTU3YmY2NjY2YzAzMTljMWVkMWU4NGQ4OTA2NSJ9fX0=";

    @Test
    void cancelHeadExposesExpectedMetadata() {
        final Cancel head = new Cancel();

        assertHeadDefinition(head, IDENTIFIER, EXPECTED_UUID, TEXTURE, EHeadFilter.INVENTORY);
    }

    @Test
    void getHeadUsesSharedFixtureToApplyMetadata() {
        final Cancel head = new Cancel();

        try (HeadTestFixtures.HeadFixture fixture = HeadTestFixtures.create(IDENTIFIER, EXPECTED_UUID.toString(), TEXTURE)) {
            final ItemStack result = fixture.invokeGetHead(head);

            fixture.verifyBuilderInteractions(result);
        }
    }
}
