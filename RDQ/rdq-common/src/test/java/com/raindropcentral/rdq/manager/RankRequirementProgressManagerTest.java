package com.raindropcentral.rdq.manager;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankUpgradeProgress;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq.database.entity.rank.RRequirement;
import com.raindropcentral.rdq.database.repository.RPlayerRankUpgradeProgressRepository;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import com.raindropcentral.rdq.view.rank.RequirementCompletionResult;
import com.raindropcentral.rdq.view.rank.RequirementProgressData;
import com.raindropcentral.rdq.view.rank.RequirementStatus;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankRequirementProgressManagerTest {

    @Mock
    private RDQ plugin;
    @Mock
    private RPlayerRankUpgradeProgressRepository progressRepository;
    @Mock
    private Player player;

    private RankRequirementProgressManager manager;
    private RDQPlayer rdqPlayer;

    private Map<LookupKey, RPlayerRankUpgradeProgress> progressStore;
    private boolean failLookup;

    @BeforeEach
    void setUp() {
        manager = new RankRequirementProgressManager(plugin);
        rdqPlayer = new RDQPlayer(UUID.randomUUID(), "RDQPlayer");
        progressStore = new HashMap<>();
        failLookup = false;

        when(plugin.getPlayerRankUpgradeProgressRepository()).thenReturn(progressRepository);
        when(progressRepository.findListByAttributes(anyMap())).thenAnswer(invocation -> {
            if (failLookup) {
                throw new RuntimeException("lookup failure");
            }
            final Map<String, Object> attributes = invocation.getArgument(0);
            final UUID playerId = (UUID) attributes.get("player.uniqueId");
            final Long requirementId = (Long) attributes.get("upgradeRequirement.id");
            final RPlayerRankUpgradeProgress progress = progressStore.get(new LookupKey(playerId, requirementId));
            if (progress == null) {
                return List.of();
            }
            return List.of(progress);
        });
        when(progressRepository.create(any())).thenAnswer(invocation -> {
            final RPlayerRankUpgradeProgress progress = invocation.getArgument(0);
            final UUID createdPlayerId = progress.getPlayer().getUniqueId();
            final Long requirementId = (Long) progress.getUpgradeRequirement().getId();
            progressStore.put(new LookupKey(createdPlayerId, requirementId), progress);
            return null;
        });
        when(progressRepository.update(any())).thenReturn(null);

        final UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("PlayerOne");
    }

    @Test
    void getRequirementProgressReturnsCachedValueWhenValid() {
        final FakeRequirement fakeRequirement = new FakeRequirement("requirement.desc.cached");
        fakeRequirement.setMet(false);
        fakeRequirement.setProgress(0.4);
        final RRankUpgradeRequirement requirement = createRequirement(1L, fakeRequirement);
        storeProgress(rdqPlayer, requirement, 0.0);

        final RequirementProgressData first = manager.getRequirementProgress(player, rdqPlayer, requirement);
        final RequirementProgressData second = manager.getRequirementProgress(player, rdqPlayer, requirement);

        assertSame(first, second);
        verify(progressRepository).findListByAttributes(anyMap());
    }

    @Test
    void getRequirementProgressRecomputesWhenCacheExpired() throws Exception {
        final FakeRequirement fakeRequirement = new FakeRequirement("requirement.desc.expired");
        fakeRequirement.setMet(false);
        fakeRequirement.setProgress(0.2);
        final RRankUpgradeRequirement requirement = createRequirement(2L, fakeRequirement);
        storeProgress(rdqPlayer, requirement, 0.0);

        final RequirementProgressData first = manager.getRequirementProgress(player, rdqPlayer, requirement);

        final Map<String, Long> timestamps = cacheTimestamps();
        final String cacheKey = cacheKey(player, requirement);
        timestamps.put(cacheKey, System.currentTimeMillis() - 60_000L);

        final RequirementProgressData second = manager.getRequirementProgress(player, rdqPlayer, requirement);

        assertNotSame(first, second);
        verify(progressRepository, atLeast(2)).findListByAttributes(anyMap());
    }

    @Test
    void attemptRequirementCompletionCompletesRequirementAndInvalidatesCache() {
        final FakeRequirement fakeRequirement = new FakeRequirement("requirement.desc.complete");
        fakeRequirement.setMet(true);
        fakeRequirement.setProgress(1.0);
        final RRankUpgradeRequirement requirement = createRequirement(3L, fakeRequirement);
        final RPlayerRankUpgradeProgress progress = storeProgress(rdqPlayer, requirement, 0.0);

        final RequirementProgressData cached = manager.getRequirementProgress(player, rdqPlayer, requirement);
        clearInvocations(progressRepository);
        fakeRequirement.resetConsumption();

        final RequirementCompletionResult result = manager.attemptRequirementCompletion(player, rdqPlayer, requirement);

        assertTrue(result.isSuccess());
        assertEquals("requirement.completed_successfully", result.getMessageKey());
        assertTrue(result.getUpdatedProgress().isCompleted());
        assertNotSame(cached, result.getUpdatedProgress());
        assertTrue(fakeRequirement.wasConsumed());
        assertEquals(1.0, progress.getProgress());

        verify(progressRepository).update(progress);

        final RequirementProgressData refreshed = manager.getRequirementProgress(player, rdqPlayer, requirement);
        assertSame(result.getUpdatedProgress(), refreshed);
    }

    @Test
    void attemptRequirementCompletionFailsWhenConsumptionThrows() {
        final FakeRequirement fakeRequirement = new FakeRequirement("requirement.desc.consume");
        fakeRequirement.setMet(true);
        fakeRequirement.setProgress(1.0);
        fakeRequirement.setConsumeException(new RuntimeException("consume failed"));
        final RRankUpgradeRequirement requirement = createRequirement(4L, fakeRequirement);
        final RPlayerRankUpgradeProgress progress = storeProgress(rdqPlayer, requirement, 0.0);

        final RequirementCompletionResult result = manager.attemptRequirementCompletion(player, rdqPlayer, requirement);

        assertFalse(result.isSuccess());
        assertEquals("requirement.consumption_failed", result.getMessageKey());
        assertFalse(result.getUpdatedProgress().isCompleted());
        assertTrue(fakeRequirement.wasConsumed());
        assertEquals(0.0, progress.getProgress());

        verify(progressRepository, never()).update(any());
    }

    @Test
    void attemptRequirementCompletionReturnsAlreadyCompletedWhenProgressComplete() {
        final FakeRequirement fakeRequirement = new FakeRequirement("requirement.desc.already");
        fakeRequirement.setMet(true);
        fakeRequirement.setProgress(1.0);
        final RRankUpgradeRequirement requirement = createRequirement(5L, fakeRequirement);
        final RPlayerRankUpgradeProgress progress = storeProgress(rdqPlayer, requirement, 1.0);

        final RequirementCompletionResult result = manager.attemptRequirementCompletion(player, rdqPlayer, requirement);

        assertFalse(result.isSuccess());
        assertEquals("requirement.already_completed", result.getMessageKey());
        assertTrue(result.getUpdatedProgress().isCompleted());
        assertFalse(fakeRequirement.wasConsumed());
        assertEquals(1.0, progress.getProgress());

        verify(progressRepository, never()).update(any());
    }

    @Test
    void attemptRequirementCompletionReturnsNotMetWhenRequirementNotSatisfied() {
        final FakeRequirement fakeRequirement = new FakeRequirement("requirement.desc.notmet");
        fakeRequirement.setMet(false);
        fakeRequirement.setProgress(0.5);
        final RRankUpgradeRequirement requirement = createRequirement(6L, fakeRequirement);
        final RPlayerRankUpgradeProgress progress = storeProgress(rdqPlayer, requirement, 0.0);

        final RequirementCompletionResult result = manager.attemptRequirementCompletion(player, rdqPlayer, requirement);

        assertFalse(result.isSuccess());
        assertEquals("requirement.not_met", result.getMessageKey());
        assertFalse(result.getUpdatedProgress().isCompleted());
        assertFalse(fakeRequirement.wasConsumed());
        assertEquals(0.0, progress.getProgress());

        verify(progressRepository, never()).update(any());
    }

    @Test
    void attemptRequirementCompletionReturnsCompletionErrorOnRepositoryFailure() {
        failLookup = true;
        final FakeRequirement fakeRequirement = new FakeRequirement("requirement.desc.error");
        final RRankUpgradeRequirement requirement = createRequirement(7L, fakeRequirement);

        final RequirementCompletionResult result = manager.attemptRequirementCompletion(player, rdqPlayer, requirement);

        assertFalse(result.isSuccess());
        assertEquals("requirement.completion_error", result.getMessageKey());
        assertEquals(RequirementStatus.ERROR, result.getUpdatedProgress().getStatus());
    }

    @Test
    void areAllRequirementsCompletedReturnsTrueWhenAllCompleted() {
        final RRankUpgradeRequirement first = createRequirement(8L, new FakeRequirement("requirement.desc.first"));
        final RRankUpgradeRequirement second = createRequirement(9L, new FakeRequirement("requirement.desc.second"));
        storeProgress(rdqPlayer, first, 1.0);
        storeProgress(rdqPlayer, second, 1.0);

        final RRank rank = mock(RRank.class);
        when(rank.getUpgradeRequirements()).thenReturn(Set.of(first, second));

        assertTrue(manager.areAllRequirementsCompleted(player, rdqPlayer, rank));
    }

    @Test
    void areAllRequirementsCompletedReturnsFalseWhenAnyIncomplete() {
        final FakeRequirement completeRequirement = new FakeRequirement("requirement.desc.completeFlag");
        final FakeRequirement incompleteRequirement = new FakeRequirement("requirement.desc.incomplete");
        incompleteRequirement.setMet(false);
        incompleteRequirement.setProgress(0.25);

        final RRankUpgradeRequirement first = createRequirement(10L, completeRequirement);
        final RRankUpgradeRequirement second = createRequirement(11L, incompleteRequirement);
        storeProgress(rdqPlayer, first, 1.0);
        storeProgress(rdqPlayer, second, 0.0);

        final RRank rank = mock(RRank.class);
        when(rank.getUpgradeRequirements()).thenReturn(Set.of(first, second));

        assertFalse(manager.areAllRequirementsCompleted(player, rdqPlayer, rank));
    }

    @Test
    void getRankOverallProgressAveragesRequirementProgress() {
        final FakeRequirement firstRequirement = new FakeRequirement("requirement.desc.progress1");
        firstRequirement.setMet(false);
        firstRequirement.setProgress(0.25);
        final FakeRequirement secondRequirement = new FakeRequirement("requirement.desc.progress2");
        secondRequirement.setMet(true);
        secondRequirement.setProgress(0.75);

        final RRankUpgradeRequirement first = createRequirement(12L, firstRequirement);
        final RRankUpgradeRequirement second = createRequirement(13L, secondRequirement);
        storeProgress(rdqPlayer, first, 0.0);
        storeProgress(rdqPlayer, second, 0.0);

        final RRank rank = mock(RRank.class);
        when(rank.getUpgradeRequirements()).thenReturn(Set.of(first, second));

        final double progress = manager.getRankOverallProgress(player, rdqPlayer, rank);
        assertEquals(0.5, progress, 0.0001);
    }

    @Test
    void initializeRankProgressTrackingCreatesMissingEntries() {
        final RRankUpgradeRequirement existingRequirement = createRequirement(14L, new FakeRequirement("requirement.desc.existing"));
        final RRankUpgradeRequirement missingRequirement = createRequirement(15L, new FakeRequirement("requirement.desc.missing"));

        storeProgress(rdqPlayer, existingRequirement, 0.5);

        final RRank rank = mock(RRank.class);
        when(rank.getUpgradeRequirements()).thenReturn(Set.of(existingRequirement, missingRequirement));

        manager.initializeRankProgressTracking(rdqPlayer, rank);

        final ArgumentCaptor<RPlayerRankUpgradeProgress> createdCaptor = ArgumentCaptor.forClass(RPlayerRankUpgradeProgress.class);
        verify(progressRepository).create(createdCaptor.capture());

        final RPlayerRankUpgradeProgress created = createdCaptor.getValue();
        assertNotNull(created);
        assertSame(missingRequirement, created.getUpgradeRequirement());
        assertEquals(rdqPlayer.getUniqueId(), created.getPlayer().getUniqueId());
    }

    @Test
    void cleaRDQPlayerCacheRemovesOnlyTargetPlayerEntries() {
        final FakeRequirement fakeRequirement = new FakeRequirement("requirement.desc.clear");
        fakeRequirement.setMet(false);
        fakeRequirement.setProgress(0.3);
        final RRankUpgradeRequirement requirement = createRequirement(16L, fakeRequirement);
        storeProgress(rdqPlayer, requirement, 0.0);

        final RequirementProgressData initial = manager.getRequirementProgress(player, rdqPlayer, requirement);

        final Player otherPlayer = mock(Player.class);
        final UUID otherUuid = UUID.randomUUID();
        when(otherPlayer.getUniqueId()).thenReturn(otherUuid);
        when(otherPlayer.getName()).thenReturn("PlayerTwo");
        final RDQPlayer otherRdqPlayer = new RDQPlayer(otherUuid, "PlayerTwo");
        storeProgress(otherRdqPlayer, requirement, 0.0);
        final RequirementProgressData otherInitial = manager.getRequirementProgress(otherPlayer, otherRdqPlayer, requirement);

        manager.cleaRDQPlayerCache(player);

        final RequirementProgressData refreshed = manager.getRequirementProgress(player, rdqPlayer, requirement);
        final RequirementProgressData otherRefreshed = manager.getRequirementProgress(otherPlayer, otherRdqPlayer, requirement);

        assertNotSame(initial, refreshed);
        assertSame(otherInitial, otherRefreshed);
    }

    @Test
    void clearAllCacheRemovesEveryEntry() throws Exception {
        final FakeRequirement fakeRequirement = new FakeRequirement("requirement.desc.clearall");
        fakeRequirement.setMet(false);
        fakeRequirement.setProgress(0.6);
        final RRankUpgradeRequirement requirement = createRequirement(17L, fakeRequirement);
        storeProgress(rdqPlayer, requirement, 0.0);

        final RequirementProgressData initial = manager.getRequirementProgress(player, rdqPlayer, requirement);

        manager.clearAllCache();

        final Map<String, RequirementProgressData> cache = progressCache();
        assertTrue(cache.isEmpty());

        final RequirementProgressData refreshed = manager.getRequirementProgress(player, rdqPlayer, requirement);
        assertNotSame(initial, refreshed);
    }

    @Test
    void refreshRankProgressReloadsRequirements() {
        final FakeRequirement firstRequirement = new FakeRequirement("requirement.desc.refresh1");
        firstRequirement.setMet(false);
        firstRequirement.setProgress(0.2);
        final FakeRequirement secondRequirement = new FakeRequirement("requirement.desc.refresh2");
        secondRequirement.setMet(false);
        secondRequirement.setProgress(0.4);

        final RRankUpgradeRequirement first = createRequirement(18L, firstRequirement);
        final RRankUpgradeRequirement second = createRequirement(19L, secondRequirement);
        final RPlayerRankUpgradeProgress firstProgress = storeProgress(rdqPlayer, first, 0.0);
        final RPlayerRankUpgradeProgress secondProgress = storeProgress(rdqPlayer, second, 0.0);

        final RequirementProgressData firstInitial = manager.getRequirementProgress(player, rdqPlayer, first);
        final RequirementProgressData secondInitial = manager.getRequirementProgress(player, rdqPlayer, second);

        firstProgress.setProgress(1.0);
        secondRequirement.setMet(true);
        secondRequirement.setProgress(0.9);

        final RRank rank = mock(RRank.class);
        when(rank.getUpgradeRequirements()).thenReturn(Set.of(first, second));

        manager.refreshRankProgress(player, rdqPlayer, rank);

        final RequirementProgressData firstRefreshed = manager.getRequirementProgress(player, rdqPlayer, first);
        final RequirementProgressData secondRefreshed = manager.getRequirementProgress(player, rdqPlayer, second);

        assertNotSame(firstInitial, firstRefreshed);
        assertTrue(firstRefreshed.isCompleted());
        assertNotSame(secondInitial, secondRefreshed);
        assertEquals(0.9, secondRefreshed.getProgressPercentage(), 0.0001);
    }

    private RRankUpgradeRequirement createRequirement(final long id, final FakeRequirement requirementLogic) {
        final RRankUpgradeRequirement requirement = mock(RRankUpgradeRequirement.class);
        final RRequirement requirementEntity = mock(RRequirement.class);
        when(requirement.getId()).thenReturn(id);
        when(requirement.getDisplayOrder()).thenReturn(0);
        when(requirement.getRequirement()).thenReturn(requirementEntity);
        when(requirementEntity.getRequirement()).thenReturn(requirementLogic);
        return requirement;
    }

    private RPlayerRankUpgradeProgress storeProgress(final RDQPlayer playerEntity, final RRankUpgradeRequirement requirement, final double value) {
        final RPlayerRankUpgradeProgress progress = new RPlayerRankUpgradeProgress(playerEntity, requirement);
        progress.setProgress(value);
        final Long requirementId = (Long) requirement.getId();
        progressStore.put(new LookupKey(playerEntity.getUniqueId(), requirementId), progress);
        return progress;
    }

    private Map<String, Long> cacheTimestamps() throws Exception {
        final Field field = RankRequirementProgressManager.class.getDeclaredField("cacheTimestamps");
        field.setAccessible(true);
        @SuppressWarnings("unchecked") final Map<String, Long> timestamps = (Map<String, Long>) field.get(manager);
        return timestamps;
    }

    private Map<String, RequirementProgressData> progressCache() throws Exception {
        final Field field = RankRequirementProgressManager.class.getDeclaredField("progressCache");
        field.setAccessible(true);
        @SuppressWarnings("unchecked") final Map<String, RequirementProgressData> cache = (Map<String, RequirementProgressData>) field.get(manager);
        return cache;
    }

    private String cacheKey(final Player playerEntity, final RRankUpgradeRequirement requirement) {
        return playerEntity.getUniqueId().toString() + ":" + requirement.getId();
    }

    private record LookupKey(UUID playerId, Long requirementId) { }

    private static final class FakeRequirement extends AbstractRequirement {

        private boolean met;
        private double progress;
        private boolean consumed;
        private RuntimeException consumeException;
        private final String descriptionKey;

        private FakeRequirement(final String descriptionKey) {
            super(Type.ITEM);
            this.descriptionKey = descriptionKey;
            this.met = true;
            this.progress = 1.0;
        }

        @Override
        public boolean isMet(final Player player) {
            return met;
        }

        @Override
        public double calculateProgress(final Player player) {
            return progress;
        }

        @Override
        public void consume(final Player player) {
            consumed = true;
            if (consumeException != null) {
                throw consumeException;
            }
        }

        @Override
        public String getDescriptionKey() {
            return descriptionKey;
        }

        void setMet(final boolean met) {
            this.met = met;
        }

        void setProgress(final double progress) {
            this.progress = progress;
        }

        void setConsumeException(final RuntimeException consumeException) {
            this.consumeException = consumeException;
        }

        boolean wasConsumed() {
            return consumed;
        }

        void resetConsumption() {
            this.consumed = false;
            this.consumeException = null;
        }
    }
}
