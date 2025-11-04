package com.raindropcentral.rdq.view.rank;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRank;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankUpgradeProgress;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq.database.entity.rank.RRequirement;
import com.raindropcentral.rdq.database.repository.RDQPlayerRepository;
import com.raindropcentral.rdq.database.repository.RPlayerRankRepository;
import com.raindropcentral.rdq.database.repository.RPlayerRankUpgradeProgressRepository;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import com.raindropcentral.rdq.service.rank.RankUpgradeProgressService;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.context.SlotClickContext;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankProgressionManagerTest {

    private RDQ rdq;
    private RPlayerRankRepository playerRankRepository;
    private RDQPlayerRepository playerRepository;
    private RPlayerRankUpgradeProgressRepository progressRepository;

    @BeforeEach
    void setUp() {
        this.rdq = Mockito.mock(RDQ.class);
        this.playerRankRepository = Mockito.mock(RPlayerRankRepository.class);
        this.playerRepository = Mockito.mock(RDQPlayerRepository.class);
        this.progressRepository = Mockito.mock(RPlayerRankUpgradeProgressRepository.class);

        Mockito.when(this.rdq.getPlayerRankRepository()).thenReturn(this.playerRankRepository);
        Mockito.when(this.rdq.getPlayerRepository()).thenReturn(this.playerRepository);
        Mockito.when(this.rdq.getPlayerRankUpgradeProgressRepository()).thenReturn(this.progressRepository);
        Mockito.when(this.rdq.getLuckPermsService()).thenReturn(null);

        Mockito.when(this.playerRankRepository.findListByAttributes(Mockito.anyMap()))
            .thenAnswer(invocation -> new ArrayList<RPlayerRank>());
        Mockito.when(this.playerRepository.findByAttributes(Mockito.anyMap())).thenReturn(null);
        Mockito.when(this.progressRepository.findListByAttributes(Mockito.anyMap()))
            .thenReturn(Collections.emptyList());
    }

    @Test
    void assignInitialRankForPathCreatesPlayerRankAndPersists() {
        final RDQPlayer rdqPlayer = new RDQPlayer(UUID.randomUUID(), "Alice");
        final RRankTree rankTree = Mockito.mock(RRankTree.class);
        final RRank initialRank = Mockito.mock(RRank.class);

        Mockito.when(rankTree.getIdentifier()).thenReturn("starter-tree");
        Mockito.when(rankTree.getRanks()).thenReturn(List.of(initialRank));

        Mockito.when(initialRank.isInitialRank()).thenReturn(true);
        Mockito.when(initialRank.getIdentifier()).thenReturn("starter");
        Mockito.when(initialRank.getRankTree()).thenReturn(rankTree);
        Mockito.when(initialRank.getUpgradeRequirements()).thenReturn(Collections.emptySet());
        Mockito.when(initialRank.getAssignedLuckPermsGroup()).thenReturn("");

        Mockito.when(this.playerRepository.findById(Mockito.any())).thenReturn(null);

        final List<String> messages = new ArrayList<>();

        try (MockedStatic<TranslationService> translations = this.mockTranslations(messages);
             MockedConstruction<RankUpgradeProgressService> services = Mockito.mockConstruction(RankUpgradeProgressService.class)) {

            final RankProgressionManager manager = new RankProgressionManager(this.rdq);
            manager.assignInitialRankForPath(rdqPlayer, rankTree);

            final ArgumentCaptor<RPlayerRank> rankCaptor = ArgumentCaptor.forClass(RPlayerRank.class);
            Mockito.verify(this.playerRankRepository).create(rankCaptor.capture());

            final RPlayerRank createdRank = rankCaptor.getValue();
            assertSame(initialRank, createdRank.getCurrentRank());
            assertSame(rankTree, createdRank.getRankTree());
            assertSame(rdqPlayer, createdRank.getRdqPlayer());
            assertEquals(1, rdqPlayer.getPlayerRanks().size());
            assertTrue(messages.isEmpty(), "No player messaging is expected for initial assignments");
        }
    }

    @Test
    void startRankProgressionWithRequirementsCreatesProgressEntriesAndSendsStartMessage() {
        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.getName()).thenReturn("Alice");

        final SlotClickContext context = Mockito.mock(SlotClickContext.class);
        Mockito.when(context.getPlayer()).thenReturn(player);

        final RDQPlayer rdqPlayer = new RDQPlayer(UUID.randomUUID(), "Alice");

        final RRankTree rankTree = Mockito.mock(RRankTree.class);
        Mockito.when(rankTree.getIdentifier()).thenReturn("progress-tree");

        final RRank rank = Mockito.mock(RRank.class);
        Mockito.when(rank.getIdentifier()).thenReturn("progress-rank");
        Mockito.when(rank.getRankTree()).thenReturn(rankTree);
        Mockito.when(rank.getAssignedLuckPermsGroup()).thenReturn("");

        final RRankUpgradeRequirement firstRequirement = Mockito.mock(RRankUpgradeRequirement.class);
        final RRankUpgradeRequirement secondRequirement = Mockito.mock(RRankUpgradeRequirement.class);
        Mockito.when(firstRequirement.getId()).thenReturn(1L);
        Mockito.when(secondRequirement.getId()).thenReturn(2L);

        final Set<RRankUpgradeRequirement> requirements = new HashSet<>();
        requirements.add(firstRequirement);
        requirements.add(secondRequirement);

        Mockito.when(rank.getUpgradeRequirements()).thenReturn(requirements);

        final RankNode rankNode = new RankNode(rank);
        final List<String> messages = new ArrayList<>();

        try (MockedStatic<TranslationService> translations = this.mockTranslations(messages);
             MockedConstruction<RankUpgradeProgressService> services = Mockito.mockConstruction(RankUpgradeProgressService.class)) {

            final RankProgressionManager manager = new RankProgressionManager(this.rdq);
            manager.startRankProgression(context, rankNode, rdqPlayer);

            Mockito.verify(this.progressRepository, Mockito.times(requirements.size()))
                .create(Mockito.argThat(progress -> requirements.contains(progress.getUpgradeRequirement())));

            Mockito.verify(this.playerRankRepository, Mockito.never()).create(Mockito.any());
            assertTrue(messages.contains("rank_progression.started"));
        }
    }

    @Test
    void startRankProgressionWithoutRequirementsAssignsRankAndSendsMessages() {
        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.getName()).thenReturn("Bob");

        final SlotClickContext context = Mockito.mock(SlotClickContext.class);
        Mockito.when(context.getPlayer()).thenReturn(player);

        final RDQPlayer rdqPlayer = new RDQPlayer(UUID.randomUUID(), "Bob");

        final RRankTree rankTree = Mockito.mock(RRankTree.class);
        Mockito.when(rankTree.getIdentifier()).thenReturn("no-req-tree");

        final RRank rank = Mockito.mock(RRank.class);
        Mockito.when(rank.getIdentifier()).thenReturn("no-req-rank");
        Mockito.when(rank.getRankTree()).thenReturn(rankTree);
        Mockito.when(rank.getUpgradeRequirements()).thenReturn(Collections.emptySet());
        Mockito.when(rank.getAssignedLuckPermsGroup()).thenReturn("");

        final RankNode rankNode = new RankNode(rank);

        final List<String> messages = new ArrayList<>();

        try (MockedStatic<TranslationService> translations = this.mockTranslations(messages);
             MockedConstruction<RankUpgradeProgressService> services = Mockito.mockConstruction(RankUpgradeProgressService.class)) {

            final RankProgressionManager manager = new RankProgressionManager(this.rdq);
            manager.startRankProgression(context, rankNode, rdqPlayer);

            final ArgumentCaptor<RPlayerRank> rankCaptor = ArgumentCaptor.forClass(RPlayerRank.class);
            Mockito.verify(this.playerRankRepository).create(rankCaptor.capture());
            assertSame(rank, rankCaptor.getValue().getCurrentRank());

            assertTrue(messages.contains("rank_progression.no_requirements"));
            assertTrue(messages.contains("rank_progression.redeemed_successfully"));
        }
    }

    @Test
    void startRankProgressionDoesNotDuplicateExistingProgress() {
        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.getName()).thenReturn("Chris");

        final SlotClickContext context = Mockito.mock(SlotClickContext.class);
        Mockito.when(context.getPlayer()).thenReturn(player);

        final RDQPlayer rdqPlayer = new RDQPlayer(UUID.randomUUID(), "Chris");

        final RRankTree rankTree = Mockito.mock(RRankTree.class);
        Mockito.when(rankTree.getIdentifier()).thenReturn("existing-progress-tree");

        final RRank rank = Mockito.mock(RRank.class);
        Mockito.when(rank.getIdentifier()).thenReturn("existing-progress-rank");
        Mockito.when(rank.getRankTree()).thenReturn(rankTree);
        Mockito.when(rank.getAssignedLuckPermsGroup()).thenReturn("");

        final RRankUpgradeRequirement requirement = Mockito.mock(RRankUpgradeRequirement.class);
        Mockito.when(requirement.getId()).thenReturn(9L);

        final Set<RRankUpgradeRequirement> requirements = new HashSet<>();
        requirements.add(requirement);
        Mockito.when(rank.getUpgradeRequirements()).thenReturn(requirements);

        Mockito.when(this.progressRepository.findListByAttributes(Mockito.anyMap()))
            .thenReturn(List.of(Mockito.mock(RPlayerRankUpgradeProgress.class)));

        final RankNode rankNode = new RankNode(rank);
        final List<String> messages = new ArrayList<>();

        try (MockedStatic<TranslationService> translations = this.mockTranslations(messages);
             MockedConstruction<RankUpgradeProgressService> services = Mockito.mockConstruction(RankUpgradeProgressService.class)) {

            final RankProgressionManager manager = new RankProgressionManager(this.rdq);
            manager.startRankProgression(context, rankNode, rdqPlayer);

            Mockito.verify(this.progressRepository, Mockito.never()).create(Mockito.any());
            assertTrue(messages.contains("rank_progression.started"));
        }
    }

    @Test
    void attemptRankRedemptionAssignsRankWhenRequirementsCompleted() {
        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.getName()).thenReturn("Dana");

        final SlotClickContext context = Mockito.mock(SlotClickContext.class);
        Mockito.when(context.getPlayer()).thenReturn(player);

        final RDQPlayer rdqPlayer = new RDQPlayer(UUID.randomUUID(), "Dana");
        Mockito.when(this.playerRepository.findByAttributes(Mockito.anyMap())).thenReturn(rdqPlayer);

        final RRankTree rankTree = Mockito.mock(RRankTree.class);
        Mockito.when(rankTree.getIdentifier()).thenReturn("completed-tree");

        final RRank rank = Mockito.mock(RRank.class);
        Mockito.when(rank.getIdentifier()).thenReturn("completed-rank");
        Mockito.when(rank.getRankTree()).thenReturn(rankTree);
        Mockito.when(rank.getUpgradeRequirements()).thenReturn(Collections.emptySet());
        Mockito.when(rank.getAssignedLuckPermsGroup()).thenReturn("");

        final RankNode rankNode = new RankNode(rank);
        final List<String> messages = new ArrayList<>();

        try (MockedStatic<TranslationService> translations = this.mockTranslations(messages);
             MockedConstruction<RankUpgradeProgressService> services = Mockito.mockConstruction(RankUpgradeProgressService.class,
                 (mock, contextConstruction) -> Mockito.when(mock.hasCompletedAllUpgradeRequirements(rdqPlayer, rank)).thenReturn(true))) {

            final RankProgressionManager manager = new RankProgressionManager(this.rdq);
            manager.attemptRankRedemption(context, rankNode);

            Mockito.verify(this.playerRankRepository).create(Mockito.any());
            assertTrue(messages.contains("rank_progression.redeemed_successfully"));
        }
    }

    @Test
    void attemptRankRedemptionShowsIncompleteRequirementDetails() {
        final Player player = Mockito.mock(Player.class);
        Mockito.when(player.getName()).thenReturn("Evan");

        final SlotClickContext context = Mockito.mock(SlotClickContext.class);
        Mockito.when(context.getPlayer()).thenReturn(player);

        final RDQPlayer rdqPlayer = new RDQPlayer(UUID.randomUUID(), "Evan");
        Mockito.when(this.playerRepository.findByAttributes(Mockito.anyMap())).thenReturn(rdqPlayer);

        final RRank rank = Mockito.mock(RRank.class);
        Mockito.when(rank.getIdentifier()).thenReturn("incomplete-rank");

        final RRankUpgradeRequirement requirement = Mockito.mock(RRankUpgradeRequirement.class);
        final RRequirement rRequirement = Mockito.mock(RRequirement.class);
        final AbstractRequirement abstractRequirement = Mockito.mock(AbstractRequirement.class);

        Mockito.when(requirement.getRequirement()).thenReturn(rRequirement);
        Mockito.when(rRequirement.getRequirement()).thenReturn(abstractRequirement);
        Mockito.when(abstractRequirement.getType()).thenReturn(AbstractRequirement.Type.ITEM);
        Mockito.when(requirement.getId()).thenReturn(12L);

        final Set<RRankUpgradeRequirement> requirements = Set.of(requirement);
        Mockito.when(rank.getUpgradeRequirements()).thenReturn(requirements);

        final RPlayerRankUpgradeProgress progress = new RPlayerRankUpgradeProgress(rdqPlayer, requirement);
        progress.setProgress(0.3);

        final List<String> messages = new ArrayList<>();

        try (MockedStatic<TranslationService> translations = this.mockTranslations(messages);
             MockedConstruction<RankUpgradeProgressService> services = Mockito.mockConstruction(RankUpgradeProgressService.class,
                 (mock, contextConstruction) -> {
                     Mockito.when(mock.hasCompletedAllUpgradeRequirements(rdqPlayer, rank)).thenReturn(false);
                     Mockito.when(mock.getOverallCompletionPercentage(rdqPlayer, rank)).thenReturn(0.45);
                     Mockito.when(mock.hasCompletedUpgradeRequirement(rdqPlayer, requirement)).thenReturn(false);
                     Mockito.when(mock.getProgressForRequirement(rdqPlayer, requirement)).thenReturn(progress);
                 })) {

            final RankProgressionManager manager = new RankProgressionManager(this.rdq);
            manager.attemptRankRedemption(context, new RankNode(rank));

            Mockito.verify(this.playerRankRepository, Mockito.never()).create(Mockito.any());
            assertTrue(messages.contains("rank_progression.requirements_incomplete"));
            assertTrue(messages.contains("rank_progression.requirement_incomplete"));
        }
    }

    @Test
    void processAutoCompletableRanksAssignsRequirementlessRanks() {
        final RDQPlayer rdqPlayer = new RDQPlayer(UUID.randomUUID(), "Finn");
        final RRankTree rankTree = Mockito.mock(RRankTree.class);
        Mockito.when(rankTree.getIdentifier()).thenReturn("auto-tree");

        final RRank rank = Mockito.mock(RRank.class);
        Mockito.when(rank.getIdentifier()).thenReturn("auto-rank");
        Mockito.when(rank.getRankTree()).thenReturn(rankTree);
        Mockito.when(rank.getUpgradeRequirements()).thenReturn(Collections.emptySet());
        Mockito.when(rank.getAssignedLuckPermsGroup()).thenReturn("");

        final List<String> messages = new ArrayList<>();
        final List<RRank> available = List.of(rank);

        final AtomicBoolean rankAssigned = new AtomicBoolean(false);
        Mockito.when(this.playerRankRepository.findListByAttributes(Mockito.anyMap())).thenAnswer(invocation -> {
            if (rankAssigned.get()) {
                final RPlayerRank existing = new RPlayerRank(rdqPlayer, rank, rankTree, true);
                return List.of(existing);
            }
            return new ArrayList<>();
        });
        Mockito.doAnswer(invocation -> {
            rankAssigned.set(true);
            return null;
        }).when(this.playerRankRepository).create(Mockito.any());

        try (MockedStatic<TranslationService> translations = this.mockTranslations(messages);
             MockedStatic<List> listStatic = Mockito.mockStatic(List.class, invocation -> invocation.callRealMethod());
             MockedConstruction<RankUpgradeProgressService> services = Mockito.mockConstruction(RankUpgradeProgressService.class)) {

            listStatic.when(() -> List.of()).thenReturn(available);

            final RankProgressionManager manager = new RankProgressionManager(this.rdq);
            manager.processAutoCompletableRanks(rdqPlayer, rankTree);

            Mockito.verify(this.playerRankRepository, Mockito.atLeastOnce()).create(Mockito.any());
            assertTrue(messages.isEmpty(), "Automatic completions should not send player messages");
        }
    }

    private MockedStatic<TranslationService> mockTranslations(final List<String> sentMessages) {
        final MockedStatic<TranslationService> translations = Mockito.mockStatic(TranslationService.class);
        translations.when(() -> TranslationService.create(Mockito.any(TranslationKey.class), Mockito.any(Player.class)))
            .thenAnswer(invocation -> {
                final TranslationKey key = invocation.getArgument(0);
                final TranslationService translation = Mockito.mock(TranslationService.class);

                Mockito.when(translation.with(Mockito.anyString(), Mockito.any())).thenReturn(translation);
                Mockito.when(translation.withAll(Mockito.anyMap())).thenReturn(translation);
                Mockito.when(translation.withPrefix()).thenReturn(translation);
                Mockito.when(translation.build()).thenReturn(null);

                Mockito.doAnswer(sendInvocation -> {
                    sentMessages.add(key.key());
                    return null;
                }).when(translation).send();

                return translation;
            });
        return translations;
    }
}
