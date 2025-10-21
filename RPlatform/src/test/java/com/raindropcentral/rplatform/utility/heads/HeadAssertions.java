package com.raindropcentral.rplatform.utility.heads;

import com.raindropcentral.rplatform.utility.itembuilder.skull.IHeadBuilder;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

/**
 * Shared assertion helpers for validating {@link RHead} implementations in unit tests.
 */
public final class HeadAssertions {

    private HeadAssertions() {
    }

    public static void assertHeadDefinition(
            final RHead head,
            final String identifier,
            final UUID uuid,
            final String texture,
            final EHeadFilter filter
    ) {
        Assertions.assertEquals(identifier, head.getIdentifier(), "Head identifier mismatch");
        Assertions.assertEquals(uuid, head.getUuid(), "Head UUID mismatch");
        Assertions.assertEquals(texture, head.getTexture(), "Head texture mismatch");
        Assertions.assertEquals(filter, head.getFilter(), "Head filter mismatch");
        Assertions.assertEquals("head." + identifier, head.getTranslationKey(), "Head translation key mismatch");
    }

    public static void assertHeadBuilderInteractions(
            final IHeadBuilder<?> builder,
            final UUID expectedUuid,
            final String expectedTexture,
            final Component expectedName,
            final List<Component> expectedLore
    ) {
        Mockito.verify(builder).setCustomTexture(expectedUuid, expectedTexture);
        final ArgumentCaptor<Component> nameCaptor = ArgumentCaptor.forClass(Component.class);
        Mockito.verify(builder).setName(nameCaptor.capture());
        Assertions.assertEquals(expectedName, nameCaptor.getValue(), "Head display name mismatch");
        @SuppressWarnings("unchecked")
        final ArgumentCaptor<List<Component>> loreCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(builder).setLore(loreCaptor.capture());
        Assertions.assertEquals(expectedLore, loreCaptor.getValue(), "Head lore mismatch");
        Mockito.verify(builder).build();
    }
}
