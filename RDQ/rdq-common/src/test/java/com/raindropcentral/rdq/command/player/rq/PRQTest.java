package com.raindropcentral.rdq.command.player.rq;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.view.bounty.BountyMainView;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PRQTest {

    @Mock
    private EvaluationEnvironmentBuilder environmentBuilder;

    @Mock
    private RDQ rdq;

    @Mock
    private ViewFrame viewFrame;

    @Mock
    private Player player;

    private PRQ newCommand() {
        final PRQSection section = new PRQSection(this.environmentBuilder);
        return Mockito.spy(new PRQ(section, this.rdq));
    }

    @Test
    void onPlayerInvocationExitsEarlyWhenPermissionMissing() {
        final PRQ command = newCommand();
        doReturn(true).when(command).hasNoPermission(this.player, ERQPermission.COMMAND);

        command.onPlayerInvocation(this.player, "prq", new String[]{"bounty"});

        verify(command).hasNoPermission(this.player, ERQPermission.COMMAND);
        verifyNoInteractions(this.viewFrame);
        verify(this.rdq, Mockito.never()).getViewFrame();
    }

    @Test
    void onPlayerInvocationOpensBountyViewWhenActionSelected() {
        final PRQ command = newCommand();
        doReturn(false).when(command).hasNoPermission(this.player, ERQPermission.COMMAND);
        doReturn(false).when(command).hasNoPermission(this.player, ERQPermission.BOUNTY);
        when(this.rdq.getViewFrame()).thenReturn(this.viewFrame);

        command.onPlayerInvocation(this.player, "prq", new String[]{"bounty"});

        verify(this.rdq).getViewFrame();
        verify(this.viewFrame).open(BountyMainView.class, this.player);
    }

    @Test
    void onPlayerInvocationWithoutArgumentsTriggersHelp() {
        final PRQ command = newCommand();
        doReturn(false).when(command).hasNoPermission(this.player, ERQPermission.COMMAND);
        final TranslationKey helpKey = TranslationKey.of("rq.help");

        try (MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class)) {
            final TranslationService translation = mock(TranslationService.class);
            translations.when(() -> TranslationService.create(helpKey, this.player)).thenReturn(translation);
            when(translation.withPrefix()).thenReturn(translation);

            command.onPlayerInvocation(this.player, "prq", new String[0]);

            translations.verify(() -> TranslationService.create(helpKey, this.player));
            verify(translation).withPrefix();
            verify(translation).send();
        }
    }

    @Test
    void onPlayerTabCompletionReturnsEmptyWhenPermissionMissing() {
        final PRQ command = newCommand();
        doReturn(true).when(command).hasNoPermission(this.player, ERQPermission.COMMAND);

        final List<String> suggestions = command.onPlayerTabCompletion(this.player, "prq", new String[]{"b"});

        assertTrue(suggestions.isEmpty(), "No suggestions should be provided without permission.");
    }

    @Test
    void onPlayerTabCompletionReturnsFilteredSuggestionsWhenPermitted() {
        final PRQ command = newCommand();
        doReturn(false).when(command).hasNoPermission(this.player, ERQPermission.COMMAND);

        final List<String> suggestions = command.onPlayerTabCompletion(this.player, "prq", new String[]{"ma"});

        assertEquals(List.of("main"), suggestions, "Only matching suggestions should be returned.");
    }
}
