package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankUpgradeProgress;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq.database.repository.RPlayerRankUpgradeProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankUpgradeProgressServiceTest {

    private static final UUID PLAYER_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Mock
    private RDQ rdq;
    @Mock
    private RPlayerRankUpgradeProgressRepository progressRepository;

    private RankUpgradeProgressService service;
    private RDQPlayer player;

    @BeforeEach
    void setUp() {
        when(rdq.getPlayerRankUpgradeProgressRepository()).thenReturn(progressRepository);
        service = new RankUpgradeProgressService(rdq);
        player = new RDQPlayer(PLAYER_ID, "TestPlayer");
    }

    @Test
    void initializeProgressForRankCreatesMissingEntriesAndSkipsDuplicates() {
        final RRankUpgradeRequirement missingRequirement = requirementWithId(1L);
        final RRankUpgradeRequirement existingRequirement = requirementWithId(2L);
        final RRank targetRank = mock(RRank.class);
        when(targetRank.getIdentifier()).thenReturn("target");
        when(targetRank.getUpgradeRequirements()).thenReturn(Set.of(missingRequirement, existingRequirement));

        final RPlayerRankUpgradeProgress existingProgress = new RPlayerRankUpgradeProgress(player, existingRequirement);

        when(progressRepository.findListByAttributes(argThat(attributesForRequirement(player, missingRequirement))))
                .thenReturn(List.of());
        when(progressRepository.findListByAttributes(argThat(attributesForRequirement(player, existingRequirement))))
                .thenReturn(List.of(existingProgress));

        service.initializeProgressForRank(player, targetRank);

        final ArgumentCaptor<RPlayerRankUpgradeProgress> createdCaptor = ArgumentCaptor.forClass(RPlayerRankUpgradeProgress.class);
        verify(progressRepository, times(1)).create(createdCaptor.capture());
        verify(progressRepository, times(2)).findListByAttributes(anyMap());

        final RPlayerRankUpgradeProgress createdProgress = createdCaptor.getValue();
        assertSame(player, createdProgress.getPlayer());
        assertSame(missingRequirement, createdProgress.getUpgradeRequirement());
    }

    @Test
    void hasCompletedUpgradeRequirementReflectsRepositoryState() {
        final RRankUpgradeRequirement requirement = requirementWithId(3L);
        final RPlayerRankUpgradeProgress progress = new RPlayerRankUpgradeProgress(player, requirement);
        progress.setProgress(1.0);

        when(progressRepository.findListByAttributes(argThat(attributesForRequirement(player, requirement))))
                .thenReturn(List.of(progress));

        assertTrue(service.hasCompletedUpgradeRequirement(player, requirement));

        when(progressRepository.findListByAttributes(argThat(attributesForRequirement(player, requirement))))
                .thenReturn(List.of());

        assertFalse(service.hasCompletedUpgradeRequirement(player, requirement));
    }

    @Test
    void hasCompletedAllUpgradeRequirementsRequiresEveryRequirement() {
        final RRankUpgradeRequirement firstRequirement = requirementWithId(4L);
        final RRankUpgradeRequirement secondRequirement = requirementWithId(5L);
        final RRank targetRank = mock(RRank.class);
        when(targetRank.getUpgradeRequirements()).thenReturn(Set.of(firstRequirement, secondRequirement));

        final RPlayerRankUpgradeProgress firstProgress = new RPlayerRankUpgradeProgress(player, firstRequirement);
        firstProgress.setProgress(1.0);
        final RPlayerRankUpgradeProgress secondProgress = new RPlayerRankUpgradeProgress(player, secondRequirement);
        secondProgress.setProgress(0.5);

        when(progressRepository.findListByAttributes(argThat(attributesForRequirement(player, firstRequirement))))
                .thenReturn(List.of(firstProgress));
        when(progressRepository.findListByAttributes(argThat(attributesForRequirement(player, secondRequirement))))
                .thenReturn(List.of(secondProgress));

        assertFalse(service.hasCompletedAllUpgradeRequirements(player, targetRank));

        secondProgress.setProgress(1.0);
        when(progressRepository.findListByAttributes(argThat(attributesForRequirement(player, secondRequirement))))
                .thenReturn(List.of(secondProgress));

        assertTrue(service.hasCompletedAllUpgradeRequirements(player, targetRank));
    }

    @Test
    void updateProgressPersistsNewValue() {
        final RRankUpgradeRequirement requirement = requirementWithId(6L);
        final RPlayerRankUpgradeProgress progress = new RPlayerRankUpgradeProgress(player, requirement);
        progress.setProgress(0.25);

        when(progressRepository.findListByAttributes(argThat(attributesForRequirement(player, requirement))))
                .thenReturn(List.of(progress));

        service.updateProgress(player, requirement, 0.75);

        assertEquals(0.75, progress.getProgress());
        verify(progressRepository).update(progress);
    }

    @Test
    void incrementProgressPersistsIncrementedValue() {
        final RRankUpgradeRequirement requirement = requirementWithId(7L);
        final RPlayerRankUpgradeProgress progress = new RPlayerRankUpgradeProgress(player, requirement);
        progress.setProgress(0.4);

        when(progressRepository.findListByAttributes(argThat(attributesForRequirement(player, requirement))))
                .thenReturn(List.of(progress));

        service.incrementProgress(player, requirement, 0.35);

        assertEquals(0.75, progress.getProgress());
        verify(progressRepository).update(progress);
    }

    @Test
    void clearProgressForRankDeletesProgressEntries() {
        final RRankUpgradeRequirement firstRequirement = requirementWithId(8L);
        final RRankUpgradeRequirement secondRequirement = requirementWithId(9L);
        final RRankUpgradeRequirement otherRequirement = requirementWithId(10L);
        final RRank targetRank = mock(RRank.class);
        when(targetRank.getUpgradeRequirements()).thenReturn(Set.of(firstRequirement, secondRequirement));

        final RPlayerRankUpgradeProgress firstProgress = mock(RPlayerRankUpgradeProgress.class);
        when(firstProgress.getUpgradeRequirement()).thenReturn(firstRequirement);
        when(firstProgress.getId()).thenReturn(101L);
        final RPlayerRankUpgradeProgress secondProgress = mock(RPlayerRankUpgradeProgress.class);
        when(secondProgress.getUpgradeRequirement()).thenReturn(secondRequirement);
        when(secondProgress.getId()).thenReturn(102L);
        final RPlayerRankUpgradeProgress unrelatedProgress = mock(RPlayerRankUpgradeProgress.class);
        when(unrelatedProgress.getUpgradeRequirement()).thenReturn(otherRequirement);
        when(unrelatedProgress.getId()).thenReturn(103L);

        when(progressRepository.findListByAttributes(argThat(attributesForPlayer(player))))
                .thenReturn(List.of(firstProgress, secondProgress, unrelatedProgress));

        service.clearProgressForRank(player, targetRank);

        verify(progressRepository).delete(101L);
        verify(progressRepository).delete(102L);
        verify(progressRepository, never()).delete(103L);
    }

    @Test
    void resetProgressForRequirementResetsAndUpdates() {
        final RRankUpgradeRequirement requirement = requirementWithId(11L);
        final RPlayerRankUpgradeProgress progress = new RPlayerRankUpgradeProgress(player, requirement);
        progress.setProgress(0.9);

        when(progressRepository.findListByAttributes(argThat(attributesForRequirement(player, requirement))))
                .thenReturn(List.of(progress));

        service.resetProgressForRequirement(player, requirement);

        assertEquals(0.0, progress.getProgress());
        verify(progressRepository).update(progress);
    }

    @Test
    void getOverallCompletionPercentageAveragesKnownProgress() {
        final RRankUpgradeRequirement firstRequirement = requirementWithId(12L);
        final RRankUpgradeRequirement secondRequirement = requirementWithId(13L);
        final RRankUpgradeRequirement thirdRequirement = requirementWithId(14L);
        final RRank targetRank = mock(RRank.class);
        when(targetRank.getUpgradeRequirements()).thenReturn(Set.of(firstRequirement, secondRequirement, thirdRequirement));

        final RPlayerRankUpgradeProgress firstProgress = new RPlayerRankUpgradeProgress(player, firstRequirement);
        firstProgress.setProgress(0.5);
        final RPlayerRankUpgradeProgress secondProgress = new RPlayerRankUpgradeProgress(player, secondRequirement);
        secondProgress.setProgress(1.0);

        when(progressRepository.findListByAttributes(argThat(attributesForRequirement(player, firstRequirement))))
                .thenReturn(List.of(firstProgress));
        when(progressRepository.findListByAttributes(argThat(attributesForRequirement(player, secondRequirement))))
                .thenReturn(List.of(secondProgress));
        when(progressRepository.findListByAttributes(argThat(attributesForRequirement(player, thirdRequirement))))
                .thenReturn(List.of());

        final double percentage = service.getOverallCompletionPercentage(player, targetRank);

        assertEquals((0.5 + 1.0) / 3.0, percentage, 1.0e-6);
    }

    private static RRankUpgradeRequirement requirementWithId(final long id) {
        final RRankUpgradeRequirement requirement = mock(RRankUpgradeRequirement.class);
        when(requirement.getId()).thenReturn(id);
        return requirement;
    }

    private ArgumentMatcher<Map<String, Object>> attributesForRequirement(final RDQPlayer player,
                                                                           final RRankUpgradeRequirement requirement) {
        return attributes -> player.getUniqueId().equals(attributes.get("player.uniqueId"))
                && requirement.getId().equals(attributes.get("upgradeRequirement.id"));
    }

    private ArgumentMatcher<Map<String, Object>> attributesForPlayer(final RDQPlayer player) {
        return attributes -> attributes.size() == 1
                && player.getUniqueId().equals(attributes.get("player.uniqueId"));
    }
}
