package com.raindropcentral.rdq.utility.rank;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.config.ranks.rank.DefaultRankSection;
import com.raindropcentral.rdq.config.ranks.rank.RankSection;
import com.raindropcentral.rdq.config.ranks.ranktree.RankTreeSection;
import com.raindropcentral.rdq.config.ranks.system.RankSystemSection;
import com.raindropcentral.rdq.config.requirement.BaseRequirementSection;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankUpgradeProgress;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq.database.entity.rank.RRequirement;
import com.raindropcentral.rdq.database.repository.RPlayerRankUpgradeProgressRepository;
import com.raindropcentral.rdq.database.repository.RRankRepository;
import com.raindropcentral.rdq.database.repository.RRankTreeRepository;
import com.raindropcentral.rdq.database.repository.RRequirementRepository;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import com.raindropcentral.rdq.utility.requirement.RequirementFactory;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankEntityServiceTest {

    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    @Mock
    private RDQ rdq;
    @Mock
    private RRankRepository rankRepository;
    @Mock
    private RRankTreeRepository rankTreeRepository;
    @Mock
    private RRequirementRepository requirementRepository;
    @Mock
    private RPlayerRankUpgradeProgressRepository progressRepository;

    private RankEntityService service;
    private RequirementFactory requirementFactory;

    @BeforeEach
    void setUp() throws Exception {
        when(rdq.getRankRepository()).thenReturn(rankRepository);
        when(rdq.getRankTreeRepository()).thenReturn(rankTreeRepository);
        when(rdq.getRequirementRepository()).thenReturn(requirementRepository);
        when(rdq.getPlayerRankUpgradeProgressRepository()).thenReturn(progressRepository);

        service = new RankEntityService(rdq);
        requirementFactory = mock(RequirementFactory.class);
        final Field field = RankEntityService.class.getDeclaredField("requirementFactory");
        field.setAccessible(true);
        field.set(service, requirementFactory);
    }

    @Test
    void createDefaultRankAsyncCreatesMissingRank() {
        final RankSystemSection systemSection = mock(RankSystemSection.class);
        final DefaultRankSection defaultRank = mock(DefaultRankSection.class);
        when(systemSection.getDefaultRank()).thenReturn(defaultRank);
        when(defaultRank.getDefaultRankIdentifier()).thenReturn("starter");
        when(defaultRank.getDisplayNameKey()).thenReturn("rank.starter.name");
        when(defaultRank.getDescriptionKey()).thenReturn("rank.starter.desc");
        when(defaultRank.getLuckPermsGroup()).thenReturn("default");
        when(defaultRank.getPrefixKey()).thenReturn("<");
        when(defaultRank.getSuffixKey()).thenReturn(">");
        when(defaultRank.getIcon()).thenReturn(icon());
        when(defaultRank.getTier()).thenReturn(1);
        when(defaultRank.getWeight()).thenReturn(5);

        final RankSystemState state = RankSystemState.builder()
                .rankSystemSection(systemSection)
                .build();

        when(rankRepository.findByAttributes(argThat(map -> "starter".equals(map.get("identifier"))))).thenReturn(null);

        service.createDefaultRankAsync(state, DIRECT_EXECUTOR).join();

        final ArgumentCaptor<RRank> rankCaptor = ArgumentCaptor.forClass(RRank.class);
        verify(rankRepository).create(rankCaptor.capture());
        final RRank created = rankCaptor.getValue();

        assertEquals("starter", created.getIdentifier());
        assertSame(created, state.defaultRank());
    }

    @Test
    void createDefaultRankAsyncReusesExistingRank() {
        final RankSystemSection systemSection = mock(RankSystemSection.class);
        final DefaultRankSection defaultRank = mock(DefaultRankSection.class);
        when(systemSection.getDefaultRank()).thenReturn(defaultRank);
        when(defaultRank.getDefaultRankIdentifier()).thenReturn("starter");

        final RankSystemState state = RankSystemState.builder()
                .rankSystemSection(systemSection)
                .build();

        final RRank existing = new RRank("starter", "name", "desc", "group", "prefix", "suffix", icon(), true, 1, 1, null);
        when(rankRepository.findByAttributes(argThat(map -> "starter".equals(map.get("identifier"))))).thenReturn(existing);

        service.createDefaultRankAsync(state, DIRECT_EXECUTOR).join();

        verify(rankRepository, times(0)).create(any());
        assertSame(existing, state.defaultRank());
    }

    @Test
    void createRankTreesAsyncCreatesAndUpdatesEntities() {
        final RankTreeSection newTreeSection = mock(RankTreeSection.class);
        when(newTreeSection.getDisplayNameKey()).thenReturn("tree.new.name");
        when(newTreeSection.getDescriptionKey()).thenReturn("tree.new.desc");
        when(newTreeSection.getIcon()).thenReturn(icon());
        when(newTreeSection.getDisplayOrder()).thenReturn(7);
        when(newTreeSection.getMinimumRankTreesToBeDone()).thenReturn(2, 0);
        when(newTreeSection.getPrerequisiteRankTrees()).thenReturn(List.of());
        when(newTreeSection.getEnabled()).thenReturn(Boolean.TRUE);
        when(newTreeSection.getFinalRankTree()).thenReturn(Boolean.TRUE);

        final RankTreeSection existingTreeSection = mock(RankTreeSection.class);
        when(existingTreeSection.getDisplayOrder()).thenReturn(4);
        when(existingTreeSection.getMinimumRankTreesToBeDone()).thenReturn(3);
        when(existingTreeSection.getPrerequisiteRankTrees()).thenReturn(List.of("starter"));
        when(existingTreeSection.getFinalRankTree()).thenReturn(Boolean.FALSE);

        final Map<String, RankTreeSection> sections = new LinkedHashMap<>();
        sections.put("newTree", newTreeSection);
        sections.put("existingTree", existingTreeSection);

        final RankSystemState state = RankSystemState.builder()
                .rankTreeSections(sections)
                .rankTrees(new HashMap<>())
                .build();

        final RRankTree existingTree = new RRankTree("existingTree", "tree.existing.name", "tree.existing.desc", icon(), 1, 0, true, false);
        when(rankTreeRepository.findByAttributes(argThat(map -> "newTree".equals(map.get("identifier"))))).thenReturn(null);
        when(rankTreeRepository.findByAttributes(argThat(map -> "existingTree".equals(map.get("identifier"))))).thenReturn(existingTree);

        service.createRankTreesAsync(state, DIRECT_EXECUTOR).join();

        verify(newTreeSection).setMinimumRankTreesToBeDone(0);

        final ArgumentCaptor<RRankTree> treeCaptor = ArgumentCaptor.forClass(RRankTree.class);
        verify(rankTreeRepository).create(treeCaptor.capture());
        final RRankTree createdTree = treeCaptor.getValue();
        assertEquals("newTree", createdTree.getIdentifier());
        assertEquals(7, createdTree.getDisplayOrder());
        assertEquals(0, createdTree.getMinimumRankTreesToBeDone());
        assertSame(createdTree, state.rankTrees().get("newTree"));

        verify(rankTreeRepository).update(existingTree);
        assertEquals(4, existingTree.getDisplayOrder());
        assertEquals(3, existingTree.getMinimumRankTreesToBeDone());
        assertEquals(Boolean.FALSE, existingTree.isFinalRankTree());
        assertSame(existingTree, state.rankTrees().get("existingTree"));
    }

    @Test
    void createRanksAsyncSynchronizesEntitiesAndRequirements() {
        final RankSection existingRankSection = mock(RankSection.class);
        final RankSection newRankSection = mock(RankSection.class);

        final Map<String, BaseRequirementSection> existingRequirements = Map.of("legacy", mock(BaseRequirementSection.class));
        final Map<String, BaseRequirementSection> newRequirements = Map.of("fresh", mock(BaseRequirementSection.class));

        when(existingRankSection.getDisplayNameKey()).thenReturn("rank.existing.name");
        when(existingRankSection.getDescriptionKey()).thenReturn("rank.existing.desc");
        when(existingRankSection.getLuckPermsGroup()).thenReturn("group.existing");
        when(existingRankSection.getPrefixKey()).thenReturn("[");
        when(existingRankSection.getSuffixKey()).thenReturn("]");
        when(existingRankSection.getIcon()).thenReturn(icon());
        when(existingRankSection.getInitialRank()).thenReturn(Boolean.FALSE);
        when(existingRankSection.getTier()).thenReturn(2);
        when(existingRankSection.getWeight()).thenReturn(10);
        when(existingRankSection.getRequirements()).thenReturn(existingRequirements);
        when(existingRankSection.getPreviousRanks()).thenReturn(List.of("newRank"));
        when(existingRankSection.getNextRanks()).thenReturn(List.of("newRank"));

        when(newRankSection.getDisplayNameKey()).thenReturn("rank.new.name");
        when(newRankSection.getDescriptionKey()).thenReturn("rank.new.desc");
        when(newRankSection.getLuckPermsGroup()).thenReturn("group.new");
        when(newRankSection.getPrefixKey()).thenReturn("{");
        when(newRankSection.getSuffixKey()).thenReturn("}");
        when(newRankSection.getIcon()).thenReturn(icon());
        when(newRankSection.getInitialRank()).thenReturn(Boolean.TRUE);
        when(newRankSection.getTier()).thenReturn(1);
        when(newRankSection.getWeight()).thenReturn(5);
        when(newRankSection.getRequirements()).thenReturn(newRequirements);
        when(newRankSection.getPreviousRanks()).thenReturn(List.of("existingRank"));
        when(newRankSection.getNextRanks()).thenReturn(List.of());

        final Map<String, RankSection> treeRanks = new LinkedHashMap<>();
        treeRanks.put("existingRank", existingRankSection);
        treeRanks.put("newRank", newRankSection);

        final RankSystemState state = RankSystemState.builder().build();
        state.rankSections().put("treeA", treeRanks);

        final RRankTree tree = new RRankTree("treeA", "tree.name", "tree.desc", icon(), 1, 0, true, false);
        state.rankTrees().put("treeA", tree);

        final RRank existingRank = new RRank("existingRank", "rank.existing.name", "rank.existing.desc", "group.existing", "[", "]", icon(), false, 2, 10, null);
        final RRequirement legacyRequirement = new RRequirement(mock(AbstractRequirement.class), icon());
        final RRankUpgradeRequirement legacyUpgrade = new RRankUpgradeRequirement(existingRank, legacyRequirement, icon());
        existingRank.addUpgradeRequirement(legacyUpgrade);

        final RRank refreshedRank = new RRank("existingRank", "rank.existing.name", "rank.existing.desc", "group.existing", "[", "]", icon(), false, 2, 10, tree);
        final AtomicInteger lookupCounter = new AtomicInteger();
        when(rankRepository.findByAttributes(argThat(map -> "existingRank".equals(map.get("identifier"))))).thenAnswer(invocation -> {
            if (lookupCounter.getAndIncrement() == 0) {
                return existingRank;
            }
            return refreshedRank;
        });
        when(rankRepository.findByAttributes(argThat(map -> "newRank".equals(map.get("identifier"))))).thenReturn(null);

        final AtomicInteger updateCounter = new AtomicInteger();
        doAnswer(invocation -> {
            final RRank rank = invocation.getArgument(0);
            if ("existingRank".equals(rank.getIdentifier()) && updateCounter.getAndIncrement() == 1) {
                throw new RuntimeException("update-failure");
            }
            return null;
        }).when(rankRepository).update(any(RRank.class));

        when(requirementRepository.create(any(RRequirement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final IconSection requirementIcon = icon();
        final RRequirement newRequirement = new RRequirement(mock(AbstractRequirement.class), requirementIcon);
        final RRequirement updatedRequirement = new RRequirement(mock(AbstractRequirement.class), requirementIcon);

        final RRankUpgradeRequirement existingParsed = new RRankUpgradeRequirement(null, updatedRequirement, requirementIcon);
        existingParsed.setDisplayOrder(2);
        final RRankUpgradeRequirement newParsed = new RRankUpgradeRequirement(null, newRequirement, requirementIcon);
        newParsed.setDisplayOrder(1);

        when(requirementFactory.parseRequirements(any(RRank.class), anyMap())).thenAnswer(invocation -> {
            final RRank rank = invocation.getArgument(0);
            final Map<String, ?> map = invocation.getArgument(1);
            if ("existingRank".equals(rank.getIdentifier())) {
                assertSame(existingRequirements, map);
                return List.of(existingParsed);
            }
            if ("newRank".equals(rank.getIdentifier())) {
                assertSame(newRequirements, map);
                return List.of(newParsed);
            }
            return List.of();
        });

        when(progressRepository.findListByAttributes(anyMap())).thenAnswer(invocation -> {
            final Map<String, ?> query = invocation.getArgument(0);
            if (query.get("upgradeRequirement") == legacyUpgrade) {
                final RPlayerRankUpgradeProgress progress = mock(RPlayerRankUpgradeProgress.class);
                when(progress.getId()).thenReturn(99L);
                doThrow(new RuntimeException("delete-failure")).when(progressRepository).delete(99L);
                return List.of(progress);
            }
            return List.of();
        });

        service.createRanksAsync(state, DIRECT_EXECUTOR).join();

        verify(requirementFactory, times(2)).parseRequirements(any(RRank.class), anyMap());
        verify(requirementRepository, times(2)).create(any(RRequirement.class));
        verify(progressRepository).delete(99L);
        verify(rankRepository, atLeastOnce()).update(existingRank);
        verify(rankRepository).update(refreshedRank);
        verify(rankRepository).create(argThat(rank -> "newRank".equals(rank.getIdentifier())));

        final Map<String, RRank> createdRanks = state.ranks().get("treeA");
        assertNotNull(createdRanks);
        assertTrue(createdRanks.containsKey("newRank"));
        assertTrue(createdRanks.containsKey("existingRank"));
        assertEquals(tree, createdRanks.get("newRank").getRankTree());
        assertEquals(tree, existingRank.getRankTree());
        assertEquals(1, createdRanks.get("newRank").getUpgradeRequirements().size());
        assertEquals(1, existingRank.getUpgradeRequirements().size());
    }

    @Test
    void establishConnectionsAsyncUpdatesRankAndTreeRelationships() {
        final RankSection firstSection = mock(RankSection.class);
        final RankSection secondSection = mock(RankSection.class);
        when(firstSection.getPreviousRanks()).thenReturn(List.of("rankTwo"));
        when(firstSection.getNextRanks()).thenReturn(List.of("rankTwo", "missing"));
        when(secondSection.getPreviousRanks()).thenReturn(List.of("rankOne"));
        when(secondSection.getNextRanks()).thenReturn(List.of("rankOne"));

        final Map<String, RankSection> rankConfigs = new LinkedHashMap<>();
        rankConfigs.put("rankOne", firstSection);
        rankConfigs.put("rankTwo", secondSection);

        final RankTreeSection treeSection = mock(RankTreeSection.class);
        when(treeSection.getPrerequisiteRankTrees()).thenReturn(List.of("prereqTree", "unknown"));
        when(treeSection.getUnlockedRankTrees()).thenReturn(List.of("unlockTree"));
        when(treeSection.getConnectedRankTrees()).thenReturn(List.of("connectTree"));

        final RankSystemState state = RankSystemState.builder().build();
        state.rankSections().put("treeA", rankConfigs);
        state.rankTreeSections().put("treeA", treeSection);

        final RRankTree tree = new RRankTree("treeA", "tree.name", "tree.desc", icon(), 1, 0, true, false);
        state.rankTrees().put("treeA", tree);

        final RRank rankOne = new RRank("rankOne", "rank.one", "rank.one.desc", "group1", "<", ">", icon(), true, 1, 1, tree);
        final RRank rankTwo = new RRank("rankTwo", "rank.two", "rank.two.desc", "group2", "[", "]", icon(), false, 2, 2, tree);

        when(rankRepository.findByAttributes(argThat(map -> "rankOne".equals(map.get("identifier"))))).thenReturn(rankOne);
        when(rankRepository.findByAttributes(argThat(map -> "rankTwo".equals(map.get("identifier"))))).thenReturn(rankTwo);

        final RRankTree prereqTree = new RRankTree("prereqTree", "tree.pre.name", "tree.pre.desc", icon(), 2, 0, true, false);
        final RRankTree unlockTree = new RRankTree("unlockTree", "tree.unlock.name", "tree.unlock.desc", icon(), 3, 0, true, false);
        final RRankTree connectTree = new RRankTree("connectTree", "tree.connect.name", "tree.connect.desc", icon(), 4, 0, true, false);

        when(rankTreeRepository.findByAttributes(argThat(map -> "treeA".equals(map.get("identifier"))))).thenReturn(tree);
        when(rankTreeRepository.findByAttributes(argThat(map -> "prereqTree".equals(map.get("identifier"))))).thenReturn(prereqTree);
        when(rankTreeRepository.findByAttributes(argThat(map -> "unlockTree".equals(map.get("identifier"))))).thenReturn(unlockTree);
        when(rankTreeRepository.findByAttributes(argThat(map -> "connectTree".equals(map.get("identifier"))))).thenReturn(connectTree);
        when(rankTreeRepository.findByAttributes(argThat(map -> "unknown".equals(map.get("identifier"))))).thenReturn(null);

        service.establishConnectionsAsync(state, DIRECT_EXECUTOR).join();

        verify(rankRepository, times(2)).update(any(RRank.class));
        assertEquals(List.of("rankTwo"), rankOne.getPreviousRanks());
        assertEquals(List.of("rankTwo"), rankOne.getNextRanks());
        assertEquals(List.of("rankOne"), rankTwo.getPreviousRanks());
        assertEquals(List.of("rankOne"), rankTwo.getNextRanks());

        verify(rankTreeRepository).update(tree);
        assertEquals(List.of(prereqTree), tree.getPrerequisiteRankTrees());
        assertEquals(List.of(unlockTree), tree.getUnlockedRankTrees());
        assertEquals(List.of(connectTree), tree.getConnectedRankTrees());
    }

    private static IconSection icon() {
        return new IconSection(new EvaluationEnvironmentBuilder());
    }
}
