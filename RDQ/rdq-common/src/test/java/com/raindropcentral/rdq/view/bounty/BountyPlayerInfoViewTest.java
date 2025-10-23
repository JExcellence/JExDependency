package com.raindropcentral.rdq.view.bounty;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.service.BountyService;
import com.raindropcentral.rdq.service.BountyServiceProvider;
import de.jexcellence.jextranslate.api.TranslatedMessage;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.context.Context;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;

class BountyPlayerInfoViewTest {

    private ServerMock server;
    private PlayerMock player;
    private BountyService bountyService;
    private Context origin;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.player = this.server.addPlayer();
        this.bountyService = Mockito.mock(BountyService.class);
        BountyServiceProvider.setInstance(this.bountyService);

        this.origin = Mockito.mock(Context.class);
        Mockito.when(this.origin.getPlayer()).thenReturn(this.player);
    }

    @AfterEach
    void tearDown() {
        BountyServiceProvider.reset();
        MockBukkit.unmock();
    }

    @Test
    void onResumeDeletesConfirmedBountyAndReturnsToMainView() {
        final RBounty bounty = Mockito.mock(RBounty.class);
        final RDQPlayer targetPlayer = new RDQPlayer(UUID.randomUUID(), "TargetPlayer");
        Mockito.when(bounty.getId()).thenReturn(42L);
        Mockito.when(bounty.getPlayer()).thenReturn(targetPlayer);

        final Map<String, Object> initialData = new HashMap<>();
        initialData.put("confirmed", true);
        initialData.put("bounty", bounty);

        final Context target = Mockito.mock(Context.class);
        Mockito.when(target.getInitialData()).thenReturn(initialData);

        Mockito.when(this.bountyService.deleteBounty(42L)).thenReturn(CompletableFuture.completedFuture(true));

        final BountyPlayerInfoView view = new BountyPlayerInfoView();

        try (MockedStatic<TranslationService> translations = this.mockTranslations()) {
            view.onResume(this.origin, target);

            Mockito.verify(this.bountyService).deleteBounty(42L);
            assertEquals("bounty.player_info.deleted_successfully", this.player.nextMessage());
            Mockito.verify(target).openForPlayer(BountyMainView.class);
        }
    }

    @Test
    void onResumeNotifiesFailureWhenDeletionFails() {
        final RBounty bounty = Mockito.mock(RBounty.class);
        final RDQPlayer targetPlayer = new RDQPlayer(UUID.randomUUID(), "TargetPlayer");
        Mockito.when(bounty.getId()).thenReturn(99L);
        Mockito.when(bounty.getPlayer()).thenReturn(targetPlayer);

        final Map<String, Object> initialData = new HashMap<>();
        initialData.put("confirmed", true);
        initialData.put("bounty", bounty);

        final Context target = Mockito.mock(Context.class);
        Mockito.when(target.getInitialData()).thenReturn(initialData);

        Mockito.when(this.bountyService.deleteBounty(99L)).thenReturn(CompletableFuture.completedFuture(false));

        final BountyPlayerInfoView view = new BountyPlayerInfoView();

        try (MockedStatic<TranslationService> translations = this.mockTranslations()) {
            view.onResume(this.origin, target);

            Mockito.verify(this.bountyService).deleteBounty(99L);
            assertEquals("bounty.player_info.delete_failed", this.player.nextMessage());
            Mockito.verify(target, never()).openForPlayer(BountyMainView.class);
        }
    }

    @Test
    void onResumeSkipsDeletionWhenInitialDataInvalid() {
        final BountyPlayerInfoView view = new BountyPlayerInfoView();

        final Context nullTarget = Mockito.mock(Context.class);
        Mockito.when(nullTarget.getInitialData()).thenReturn(null);
        view.onResume(this.origin, nullTarget);

        final Context unconfirmedTarget = Mockito.mock(Context.class);
        Mockito.when(unconfirmedTarget.getInitialData()).thenReturn(Map.of("confirmed", false));
        view.onResume(this.origin, unconfirmedTarget);

        final Context missingBountyTarget = Mockito.mock(Context.class);
        Mockito.when(missingBountyTarget.getInitialData()).thenReturn(Map.of("confirmed", true));
        view.onResume(this.origin, missingBountyTarget);

        Mockito.verifyNoInteractions(this.bountyService);
    }

    private MockedStatic<TranslationService> mockTranslations() {
        final MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class);
        translations.when(() -> TranslationService.create(Mockito.any(TranslationKey.class), Mockito.any(Player.class)))
            .thenAnswer(invocation -> {
                final TranslationKey key = invocation.getArgument(0);
                final Player target = invocation.getArgument(1);
                return this.createTranslation(key, target);
            });
        return translations;
    }

    private TranslationService createTranslation(final TranslationKey key, final Player target) {
        final TranslationService translation = Mockito.mock(TranslationService.class);
        final TranslatedMessage message = new TranslatedMessage(Component.text(key.key()), key);

        Mockito.when(translation.with(Mockito.anyString(), Mockito.any())).thenReturn(translation);
        Mockito.when(translation.withAll(Mockito.anyMap())).thenReturn(translation);
        Mockito.when(translation.withPrefix()).thenReturn(translation);
        Mockito.when(translation.build()).thenReturn(message);
        Mockito.doAnswer(invocation -> {
            target.sendMessage(key.key());
            return null;
        }).when(translation).send();
        return translation;
    }
}

