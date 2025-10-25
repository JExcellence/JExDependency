package com.raindropcentral.rdq.view.rank;

import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

class RequirementCompletionResultTest {

    @Test
    void itExposesConstructorState() {
        final RequirementProgressData progressData = new RequirementProgressData(
                "req-123",
                "collect",
                "messages.requirement.collect",
                true,
                0.75,
                RequirementStatus.COMPLETED,
                "messages.requirement.collect.completed",
                5
        );

        final RequirementCompletionResult result = new RequirementCompletionResult(
                true,
                "messages.requirement.success",
                progressData
        );

        assertTrue(result.isSuccess(), "Constructor should retain success flag");
        assertEquals("messages.requirement.success", result.getMessageKey(),
                "Constructor should retain the message key");
        assertSame(progressData, result.getUpdatedProgress(),
                "Constructor should retain the provided progress data");
    }

    @Test
    void sendMessageRequiresPlayerAndUsesTranslationService() {
        final RequirementProgressData progressData = new RequirementProgressData(
                "req-456",
                "travel",
                "messages.requirement.travel",
                false,
                0.25,
                RequirementStatus.IN_PROGRESS,
                "messages.requirement.travel.in-progress",
                3
        );
        final RequirementCompletionResult result = new RequirementCompletionResult(
                false,
                "messages.requirement.travel.notify",
                progressData
        );

        assertThrows(NullPointerException.class, () -> result.sendMessage(null),
                "Null players should be rejected");

        final Player player = mock(Player.class);
        final TranslationService service = mock(TranslationService.class);
        when(service.withPrefix()).thenReturn(service);

        final String messageKey = "messages.requirement.travel.notify";

        try (MockedStatic<TranslationService> translationService = mockStatic(TranslationService.class)) {
            translationService.when(() -> TranslationService.create(
                    argThat((TranslationKey key) -> messageKey.equals(key.key())),
                    eq(player)
            )).thenReturn(service);

            result.sendMessage(player);

            translationService.verify(() -> TranslationService.create(
                    argThat((TranslationKey key) -> messageKey.equals(key.key())),
                    eq(player)
            ));
            verify(service).withPrefix();
            verify(service).send();
        }
    }
}
