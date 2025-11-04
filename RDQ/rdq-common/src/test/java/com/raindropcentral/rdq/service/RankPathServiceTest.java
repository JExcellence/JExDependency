package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRank;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankPath;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rdq.database.repository.RDQPlayerRepository;
import com.raindropcentral.rdq.database.repository.RPlayerRankPathRepository;
import com.raindropcentral.rdq.database.repository.RPlayerRankRepository;
import com.raindropcentral.rdq.service.rank.RankPathService;
import com.raindropcentral.rdq.view.rank.RankProgressionManager;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RankPathServiceTest {

    private RDQ rdq;
    private RPlayerRankPathRepository rankPathRepository;
    private RPlayerRankRepository playerRankRepository;
    private RDQPlayerRepository playerRepository;
    private RankProgressionManager progressionManager;

    @BeforeEach
    void setUp() {
        this.rdq = mock(RDQ.class);
        this.rankPathRepository = mock(RPlayerRankPathRepository.class);
        this.playerRankRepository = mock(RPlayerRankRepository.class);
        this.playerRepository = mock(RDQPlayerRepository.class);

        when(this.rdq.getPlayerRankPathRepository()).thenReturn(this.rankPathRepository);
        when(this.rdq.getPlayerRankRepository()).thenReturn(this.playerRankRepository);
        when(this.rdq.getPlayerRepository()).thenReturn(this.playerRepository);
    }

    @Test
    @DisplayName("selectRankPath returns false when the target rank tree is disabled")
    void selectRankPathRejectsDisabledTrees() {
        RankPathService service = createService();
        RDQPlayer player = samplePlayer("DisabledTree");
        RRankTree tree = sampleTree("tree.disabled");
        tree.setEnabled(false);
        RRank rank = sampleRank(tree, "rank.disabled");

        boolean result = service.selectRankPath(player, tree, rank);

        assertFalse(result, "Disabled rank trees should be rejected");
        verifyNoInteractions(this.rankPathRepository);
        verifyNoInteractions(this.progressionManager);
    }

    @Test
    @DisplayName("selectRankPath aborts when prerequisites fail")
    void selectRankPathStopsWhenPrerequisitesFail() {
        RankPathService service = createService(invocation -> {
            if ("checkRankTreePrerequisites".equals(invocation.getMethod().getName())) {
                return false;
            }
            return invocation.callRealMethod();
        });
        RDQPlayer player = samplePlayer("PrereqFailure");
        RRankTree tree = sampleTree("tree.prereq");
        RRank rank = sampleRank(tree, "rank.prereq");

        boolean result = service.selectRankPath(player, tree, rank);

        assertFalse(result, "selectRankPath should return false when prerequisites fail");
        verifyNoInteractions(this.rankPathRepository);
        verifyNoInteractions(this.playerRankRepository);
        verify(this.progressionManager, never()).assignInitialRankForPath(any(), any());
        verify(this.progressionManager, never()).processAutoCompletableRanks(any(), any());
    }

    @Test
    @DisplayName("selectRankPath reactivates existing paths and reuses stored ranks")
    void selectRankPathReactivatesExistingPath() {
        RankPathService service = createService();
        RDQPlayer player = samplePlayer("ExistingPath");
        RRankTree tree = sampleTree("tree.existing");
        RRank rank = sampleRank(tree, "rank.existing");

        RPlayerRankPath cachedPath = new RPlayerRankPath(player, tree, true);
        assignEntityId(cachedPath, 5L);
        RPlayerRankPath persistedPath = new RPlayerRankPath(player, tree, false);
        assignEntityId(persistedPath, 5L);

        when(this.rankPathRepository.findListByAttributes(argThat(matchesPlayerAttribute(player))))
                .thenReturn(Collections.emptyList());
        when(this.rankPathRepository.findByAttributes(argThat(matchesActivePathQuery(player, tree))))
                .thenReturn(cachedPath);
        when(this.rankPathRepository.findById(5L)).thenReturn(persistedPath);

        RPlayerRank storedRank = new RPlayerRank(player, rank, tree, false);
        assignEntityId(storedRank, 11L);
        when(this.playerRankRepository.findListByAttributes(argThat(matchesPlayerRankQuery(player))))
                .thenReturn(List.of(storedRank));
        when(this.playerRankRepository.findById(11L)).thenReturn(storedRank);

        boolean result = service.selectRankPath(player, tree, rank);

        assertTrue(result, "Existing path should be reactivated successfully");
        assertTrue(persistedPath.isActive(), "The persisted path should be toggled active");
        verify(this.rankPathRepository).update(persistedPath);
        verify(this.rankPathRepository, never()).create(any());
        verify(this.playerRankRepository).update(storedRank);
        verify(this.playerRankRepository, never()).create(any());
        verify(this.progressionManager).assignInitialRankForPath(player, tree);
        verify(this.progressionManager).processAutoCompletableRanks(player, tree);
    }

    @Test
    @DisplayName("selectRankPath creates new paths and player ranks when missing")
    void selectRankPathCreatesNewPath() {
        RankPathService service = createService();
        RDQPlayer player = samplePlayer("NewPath");
        RRankTree tree = sampleTree("tree.new");
        RRank rank = sampleRank(tree, "rank.new");

        when(this.rankPathRepository.findListByAttributes(argThat(matchesPlayerAttribute(player))))
                .thenReturn(Collections.emptyList());
        when(this.rankPathRepository.findByAttributes(anyMap())).thenReturn(null);
        when(this.playerRankRepository.findListByAttributes(argThat(matchesPlayerRankQuery(player))))
                .thenReturn(Collections.emptyList());
        when(this.playerRepository.findById(any())).thenReturn(null);

        boolean result = service.selectRankPath(player, tree, rank);

        assertTrue(result, "New paths should be created successfully");
        ArgumentCaptor<RPlayerRankPath> pathCaptor = ArgumentCaptor.forClass(RPlayerRankPath.class);
        verify(this.rankPathRepository).create(pathCaptor.capture());
        RPlayerRankPath createdPath = pathCaptor.getValue();
        assertSame(player, createdPath.getPlayer(), "Created path should target the player");
        assertSame(tree, createdPath.getRankTree(), "Created path should reference the tree");
        assertTrue(createdPath.isActive(), "New paths must be active");

        ArgumentCaptor<RPlayerRank> rankCaptor = ArgumentCaptor.forClass(RPlayerRank.class);
        verify(this.playerRankRepository).create(rankCaptor.capture());
        RPlayerRank createdRank = rankCaptor.getValue();
        assertSame(player, createdRank.getRdqPlayer(), "Created rank should belong to the player");
        assertSame(rank, createdRank.getCurrentRank(), "Created rank should use the starting rank");
        assertSame(tree, createdRank.getRankTree(), "Created rank should point at the selected tree");

        verify(this.progressionManager).assignInitialRankForPath(player, tree);
        verify(this.progressionManager).processAutoCompletableRanks(player, tree);
    }

    @Test
    @DisplayName("selectRankPath returns false when repository interactions fail")
    void selectRankPathHandlesRepositoryFailure() {
        RankPathService service = createService();
        RDQPlayer player = samplePlayer("RepoFailure");
        RRankTree tree = sampleTree("tree.failure");
        RRank rank = sampleRank(tree, "rank.failure");

        when(this.rankPathRepository.findListByAttributes(anyMap())).thenThrow(new RuntimeException("boom"));

        boolean result = service.selectRankPath(player, tree, rank);

        assertFalse(result, "Exceptions should cause selectRankPath to return false");
        verify(this.progressionManager, never()).assignInitialRankForPath(any(), any());
    }

    @Test
    @DisplayName("switchRankPath halts when prerequisites fail")
    void switchRankPathRespectsPrerequisites() {
        RankPathService service = createService(invocation -> {
            if ("checkRankTreePrerequisites".equals(invocation.getMethod().getName())) {
                return false;
            }
            return invocation.callRealMethod();
        });
        RDQPlayer player = samplePlayer("SwitchFail");
        RRankTree tree = sampleTree("tree.switch.fail");
        RRank rank = sampleRank(tree, "rank.switch.fail");

        boolean result = service.switchRankPath(player, tree, rank);

        assertFalse(result, "switchRankPath should return false when prerequisites fail");
        verify(service, never()).selectRankPath(any(), any(), any());
    }

    @Test
    @DisplayName("switchRankPath delegates to selectRankPath on success")
    void switchRankPathDelegates() {
        RankPathService service = createService(invocation -> {
            if ("checkRankTreePrerequisites".equals(invocation.getMethod().getName())) {
                return true;
            }
            return invocation.callRealMethod();
        });
        RDQPlayer player = samplePlayer("SwitchSuccess");
        RRankTree tree = sampleTree("tree.switch.success");
        RRank rank = sampleRank(tree, "rank.switch.success");

        doReturn(true).when(service).selectRankPath(player, tree, rank);

        boolean result = service.switchRankPath(player, tree, rank);

        assertTrue(result, "switchRankPath should forward to selectRankPath when prerequisites pass");
        verify(service).selectRankPath(player, tree, rank);
    }

    @Test
    @DisplayName("hasSelectedRankPath returns true for active selections and false otherwise")
    void hasSelectedRankPathResolvesActiveState() {
        RankPathService service = createService();
        RDQPlayer player = samplePlayer("ActiveCheck");
        RRankTree tree = sampleTree("tree.active");

        RPlayerRankPath activePath = new RPlayerRankPath(player, tree, true);
        when(this.rankPathRepository.findByAttributes(argThat(matchesActivePathQuery(player, tree))))
                .thenReturn(activePath)
                .thenThrow(new RuntimeException("boom"));

        assertTrue(service.hasSelectedRankPath(player, tree), "Active rank path should be detected");
        assertFalse(service.hasSelectedRankPath(player, tree), "Exceptions should be handled as false");
    }

    @Test
    @DisplayName("getPlayerRanksForTree filters by tree and handles failures")
    void getPlayerRanksForTreeFiltersAndHandlesErrors() {
        RankPathService service = createService();
        RDQPlayer player = samplePlayer("RankFilter");
        RRankTree matchTree = sampleTree("tree.match");
        RRankTree otherTree = sampleTree("tree.other");

        RRank matchRank = sampleRank(matchTree, "rank.match");
        RPlayerRank matching = new RPlayerRank(player, matchRank, matchTree, true);
        RRank otherRank = sampleRank(otherTree, "rank.other");
        RPlayerRank other = new RPlayerRank(player, otherRank, otherTree, true);

        when(this.playerRankRepository.findListByAttributes(argThat(matchesPlayerRankQuery(player))))
                .thenReturn(List.of(matching, other))
                .thenThrow(new RuntimeException("boom"));

        List<RPlayerRank> filtered = service.getPlayerRanksForTree(player, matchTree);
        assertEquals(List.of(matching), filtered, "Only ranks from the requested tree should be returned");

        List<RPlayerRank> all = service.getPlayerRanksForTree(player, null);
        assertEquals(List.of(matching, other), all, "Null tree should return all ranks");

        List<RPlayerRank> failure = service.getPlayerRanksForTree(player, matchTree);
        assertTrue(failure.isEmpty(), "Failures should return an empty list");
    }

    @Test
    @DisplayName("deactivateAllRankPaths toggles existing paths and suppresses failures")
    void deactivateAllRankPathsUpdatesState() throws Exception {
        RankPathService service = createService();
        RDQPlayer player = samplePlayer("DeactivatePaths");
        RRankTree tree = sampleTree("tree.path.one");
        RRankTree treeTwo = sampleTree("tree.path.two");

        RPlayerRankPath first = new RPlayerRankPath(player, tree, true);
        assignEntityId(first, 101L);
        RPlayerRankPath firstPersisted = new RPlayerRankPath(player, tree, true);
        assignEntityId(firstPersisted, 101L);
        RPlayerRankPath second = new RPlayerRankPath(player, treeTwo, true);
        assignEntityId(second, 202L);
        RPlayerRankPath secondPersisted = new RPlayerRankPath(player, treeTwo, true);
        assignEntityId(secondPersisted, 202L);

        when(this.rankPathRepository.findListByAttributes(argThat(matchesPlayerAttribute(player))))
                .thenReturn(List.of(first, second))
                .thenThrow(new RuntimeException("boom"));
        when(this.rankPathRepository.findById(101L)).thenReturn(firstPersisted);
        when(this.rankPathRepository.findById(202L)).thenReturn(secondPersisted);

        invokePrivate(service, "deactivateAllRankPaths", RDQPlayer.class, player);

        assertFalse(firstPersisted.isActive(), "First persisted path should be inactive");
        assertFalse(secondPersisted.isActive(), "Second persisted path should be inactive");
        verify(this.rankPathRepository).update(firstPersisted);
        verify(this.rankPathRepository).update(secondPersisted);

        assertDoesNotThrow(() -> invokePrivate(service, "deactivateAllRankPaths", RDQPlayer.class, player),
                "Exceptions should be suppressed inside deactivateAllRankPaths");
    }

    @Test
    @DisplayName("deactivateAllPlayerRanks toggles stored ranks and suppresses failures")
    void deactivateAllPlayerRanksUpdatesState() throws Exception {
        RankPathService service = createService();
        RDQPlayer player = samplePlayer("DeactivateRanks");
        RRankTree tree = sampleTree("tree.rank.one");
        RRankTree treeTwo = sampleTree("tree.rank.two");
        RRank rankOne = sampleRank(tree, "rank.one");
        RRank rankTwo = sampleRank(treeTwo, "rank.two");

        RPlayerRank first = new RPlayerRank(player, rankOne, tree, true);
        assignEntityId(first, 303L);
        RPlayerRank firstPersisted = new RPlayerRank(player, rankOne, tree, true);
        assignEntityId(firstPersisted, 303L);
        RPlayerRank second = new RPlayerRank(player, rankTwo, treeTwo, true);
        assignEntityId(second, 404L);
        RPlayerRank secondPersisted = new RPlayerRank(player, rankTwo, treeTwo, true);
        assignEntityId(secondPersisted, 404L);

        when(this.playerRankRepository.findListByAttributes(argThat(matchesPlayerRankQuery(player))))
                .thenReturn(List.of(first, second))
                .thenThrow(new RuntimeException("boom"));
        when(this.playerRankRepository.findById(303L)).thenReturn(firstPersisted);
        when(this.playerRankRepository.findById(404L)).thenReturn(secondPersisted);

        invokePrivate(service, "deactivateAllPlayerRanks", RDQPlayer.class, player);

        assertFalse(firstPersisted.isActive(), "First persisted rank should be inactive");
        assertFalse(secondPersisted.isActive(), "Second persisted rank should be inactive");
        verify(this.playerRankRepository).update(firstPersisted);
        verify(this.playerRankRepository).update(secondPersisted);

        assertDoesNotThrow(() -> invokePrivate(service, "deactivateAllPlayerRanks", RDQPlayer.class, player),
                "Exceptions should be suppressed inside deactivateAllPlayerRanks");
    }

    private RankPathService createService() {
        return createService(InvocationOnMock::callRealMethod);
    }

    private RankPathService createService(final Answer<Object> defaultAnswer) {
        AtomicReference<RankPathService> reference = new AtomicReference<>();
        try (MockedConstruction<RankProgressionManager> construction = Mockito.mockConstruction(
                RankProgressionManager.class,
                (mock, context) -> this.progressionManager = mock
        )) {
            RankPathService service = Mockito.mock(RankPathService.class, Mockito.withSettings()
                    .useConstructor(this.rdq)
                    .defaultAnswer(defaultAnswer));
            reference.set(service);
        }
        return reference.get();
    }

    private static RDQPlayer samplePlayer(final String name) {
        return new RDQPlayer(UUID.nameUUIDFromBytes(name.getBytes()), name);
    }

    private static RRankTree sampleTree(final String identifier) {
        IconSection icon = new IconSection(new EvaluationEnvironmentBuilder());
        icon.setMaterial("PAPER");
        return new RRankTree(identifier, identifier + ".name", identifier + ".desc", icon, 1, 0, true, false);
    }

    private static RRank sampleRank(final RRankTree tree, final String identifier) {
        IconSection icon = new IconSection(new EvaluationEnvironmentBuilder());
        icon.setMaterial("PAPER");
        return new RRank(identifier, identifier + ".name", identifier + ".desc", "group", "prefix", "suffix", icon, true, 1, 0, tree);
    }

    private static void assignEntityId(final Object entity, final long id) {
        try {
            Class<?> current = entity.getClass();
            Field idField = null;
            while (current != null && idField == null) {
                try {
                    idField = current.getDeclaredField("id");
                } catch (final NoSuchFieldException ignored) {
                    current = current.getSuperclass();
                }
            }
            if (idField == null) {
                throw new NoSuchFieldException("id");
            }
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (final ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to assign entity id", exception);
        }
    }

    private static ArgumentMatcher<Map<String, Object>> matchesPlayerAttribute(final RDQPlayer player) {
        return map -> map != null && player.equals(map.get("player"));
    }

    private static ArgumentMatcher<Map<String, Object>> matchesActivePathQuery(final RDQPlayer player, final RRankTree tree) {
        return map -> map != null
                && player.equals(map.get("player"))
                && tree.equals(map.get("rankTree"))
                && Boolean.TRUE.equals(map.get("isActive"));
    }

    private static ArgumentMatcher<Map<String, Object>> matchesPlayerRankQuery(final RDQPlayer player) {
        return map -> map != null
                && Objects.equals(map.get("player.uniqueId"), player.getUniqueId());
    }

    private static void invokePrivate(final RankPathService service, final String method,
                                      final Class<?> parameterType, final Object argument)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method reflected = RankPathService.class.getDeclaredMethod(method, parameterType);
        reflected.setAccessible(true);
        reflected.invoke(service, argument);
    }
}

