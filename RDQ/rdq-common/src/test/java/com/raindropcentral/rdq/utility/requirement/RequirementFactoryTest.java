package com.raindropcentral.rdq.utility.requirement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.config.requirement.*;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq.database.entity.rank.RRequirement;
import com.raindropcentral.rdq.database.json.requirement.RequirementParser;
import com.raindropcentral.rdq.database.repository.RRequirementRepository;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RequirementFactoryTest {

    private static IconSection createIcon(final String material) {
        IconSection icon = new IconSection(new EvaluationEnvironmentBuilder());
        icon.setMaterial(material);
        return icon;
    }

    @Test
    void parseRequirementsSkipsInvalidSectionsAndAppliesDisplayOrder() throws IOException {
        RDQ rdq = mock(RDQ.class);
        RequirementFactory factory = new RequirementFactory(rdq);
        RRequirementRepository repository = mock(RRequirementRepository.class);
        when(rdq.getRequirementRepository()).thenReturn(repository);

        BaseRequirementSection invalidBase = mock(BaseRequirementSection.class, RETURNS_DEEP_STUBS);
        when(invalidBase.getType()).thenReturn("ITEM");
        ItemRequirementSection invalidItem = mock(ItemRequirementSection.class);
        when(invalidItem.getRequiredItemsList()).thenReturn(List.of());
        when(invalidBase.getItemRequirement()).thenReturn(invalidItem);
        when(invalidBase.getIcon()).thenReturn(createIcon("PAPER"));

        BaseRequirementSection itemBase = mock(BaseRequirementSection.class, RETURNS_DEEP_STUBS);
        when(itemBase.getType()).thenReturn("ITEM");
        ItemRequirementSection itemSection = mock(ItemRequirementSection.class);
        when(itemSection.getRequiredItemsList()).thenReturn(List.of(new ItemStack(Material.DIAMOND, 3)));
        when(itemSection.getConsumeOnComplete()).thenReturn(true);
        when(itemBase.getItemRequirement()).thenReturn(itemSection);
        IconSection itemIcon = createIcon("EMERALD_BLOCK");
        when(itemBase.getIcon()).thenReturn(itemIcon);
        when(itemBase.getDisplayOrder()).thenReturn(3);

        BaseRequirementSection currencyBase = mock(BaseRequirementSection.class, RETURNS_DEEP_STUBS);
        when(currencyBase.getType()).thenReturn("CURRENCY");
        CurrencyRequirementSection currencySection = mock(CurrencyRequirementSection.class);
        when(currencySection.getRequiredCurrencies()).thenReturn(Map.of("gold", 250.0));
        when(currencySection.getConsumeOnComplete()).thenReturn(false);
        when(currencySection.getCurrencyPlugin()).thenReturn("vault");
        when(currencyBase.getCurrencyRequirement()).thenReturn(currencySection);
        IconSection currencyIcon = createIcon("GOLD_INGOT");
        when(currencyBase.getIcon()).thenReturn(currencyIcon);
        when(currencyBase.getDisplayOrder()).thenReturn(0);

        LinkedHashMap<String, BaseRequirementSection> requirements = new LinkedHashMap<>();
        requirements.put("invalid", invalidBase);
        requirements.put("item", itemBase);
        requirements.put("currency", currencyBase);

        AbstractRequirement itemRequirement = mock(AbstractRequirement.class);
        AbstractRequirement currencyRequirement = mock(AbstractRequirement.class);

        AtomicInteger parseCalls = new AtomicInteger();
        try (MockedStatic<RequirementParser> parserMock = Mockito.mockStatic(RequirementParser.class)) {
            parserMock.when(() -> RequirementParser.parse(anyString())).thenAnswer(invocation -> {
                String json = invocation.getArgument(0);
                parseCalls.incrementAndGet();
                if (json.contains("\"type\":\"CURRENCY\"")) {
                    return currencyRequirement;
                }
                return itemRequirement;
            });

            var rank = mock(com.raindropcentral.rdq.database.entity.rank.RRank.class);
            when(rank.getIdentifier()).thenReturn("rank-one");
            when(rank.addUpgradeRequirement(any())).thenReturn(true);

            List<RRankUpgradeRequirement> result = factory.parseRequirements(rank, requirements);

            assertEquals(2, result.size());
            RRankUpgradeRequirement first = result.getFirst();
            RRankUpgradeRequirement second = result.get(1);
            assertEquals(3, first.getDisplayOrder());
            assertEquals(0, second.getDisplayOrder());
            assertSame(itemRequirement, first.getRequirement().getRequirement());
            assertSame(currencyRequirement, second.getRequirement().getRequirement());
            assertEquals(itemIcon, first.getRequirement().getShowcase());
            assertEquals(currencyIcon, second.getRequirement().getShowcase());
        }
        assertEquals(2, parseCalls.get());
    }

    @Test
    void parseRequirementsAsyncUsesProvidedExecutor() {
        RDQ rdq = mock(RDQ.class);
        RequirementFactory factory = Mockito.spy(new RequirementFactory(rdq));
        var rank = mock(com.raindropcentral.rdq.database.entity.rank.RRank.class);
        Map<String, BaseRequirementSection> requirements = Map.of();
        List<RRankUpgradeRequirement> expected = List.of(mock(RRankUpgradeRequirement.class));
        doReturn(expected).when(factory).parseRequirements(rank, requirements);

        AtomicInteger executorInvocations = new AtomicInteger();
        Executor executor = command -> {
            executorInvocations.incrementAndGet();
            command.run();
        };

        List<RRankUpgradeRequirement> result = factory.parseRequirementsAsync(rank, requirements, executor).join();

        assertEquals(expected, result);
        assertEquals(1, executorInvocations.get());
        verify(factory).parseRequirements(rank, requirements);
    }

    @Test
    void persistRequirementEntitiesAsyncPersistsNewEntitiesUsingProvidedExecutor() {
        RDQ rdq = mock(RDQ.class);
        RRequirementRepository repository = mock(RRequirementRepository.class);
        when(rdq.getRequirementRepository()).thenReturn(repository);
        RequirementFactory factory = new RequirementFactory(rdq);

        RRequirement existingRequirement = mock(RRequirement.class);
        when(existingRequirement.getId()).thenReturn(5L);
        RRankUpgradeRequirement existingUpgrade = mock(RRankUpgradeRequirement.class);
        when(existingUpgrade.getRequirement()).thenReturn(existingRequirement);

        RRequirement newRequirement = mock(RRequirement.class);
        when(newRequirement.getId()).thenReturn(null);
        RRankUpgradeRequirement newUpgrade = mock(RRankUpgradeRequirement.class);
        when(newUpgrade.getRequirement()).thenReturn(newRequirement);

        RRequirement savedRequirement = mock(RRequirement.class);
        when(repository.createAsync(newRequirement)).thenReturn(CompletableFuture.completedFuture(savedRequirement));

        Executor executor = command -> command.run();

        List<RRankUpgradeRequirement> result = factory.persistRequirementEntitiesAsync(List.of(existingUpgrade, newUpgrade), executor).join();

        assertEquals(List.of(existingUpgrade, newUpgrade), result);
        verify(repository).createAsync(newRequirement);
        verify(newUpgrade).setRequirement(savedRequirement);
        verify(existingUpgrade, never()).setRequirement(any());
        verify(rdq, never()).getExecutor();
    }

    @Test
    void createRequirementFromSectionProducesExpectedJsonForAllTypes() throws Exception {
        RDQ rdq = mock(RDQ.class);
        RequirementFactory factory = new RequirementFactory(rdq);
        ObjectMapper objectMapper = new ObjectMapper();

        ItemRequirementSection itemSection = mock(ItemRequirementSection.class);
        when(itemSection.getRequiredItemsList()).thenReturn(List.of(new ItemStack(Material.DIAMOND, 2)));
        when(itemSection.getConsumeOnComplete()).thenReturn(true);

        CurrencyRequirementSection currencySection = mock(CurrencyRequirementSection.class);
        when(currencySection.getRequiredCurrencies()).thenReturn(Map.of("silver", 75.0));
        when(currencySection.getConsumeOnComplete()).thenReturn(false);
        when(currencySection.getCurrencyPlugin()).thenReturn("vault");

        ExperienceLevelRequirementSection experienceSection = mock(ExperienceLevelRequirementSection.class);
        when(experienceSection.getRequiredLevel()).thenReturn(5);
        when(experienceSection.getConsumeOnComplete()).thenReturn(false);
        when(experienceSection.getExperienceType()).thenReturn("LEVEL");

        PlaytimeRequirementSection playtimeSection = mock(PlaytimeRequirementSection.class);
        when(playtimeSection.getRequiredPlaytimeSeconds()).thenReturn(7200L);
        when(playtimeSection.getUseTotalPlaytime()).thenReturn(true);

        PermissionRequirementSection permissionSection = mock(PermissionRequirementSection.class);
        when(permissionSection.getRequiredPermissions()).thenReturn(List.of("rdq.test"));
        when(permissionSection.getRequireAll()).thenReturn(true);
        when(permissionSection.getCheckNegation()).thenReturn(false);

        LocationRequirementSection locationSection = mock(LocationRequirementSection.class);
        when(locationSection.getRequiredWorld()).thenReturn("world");
        when(locationSection.getExactLocation()).thenReturn(false);

        CompositeRequirementSection compositeSection = mock(CompositeRequirementSection.class);
        when(compositeSection.getOperator()).thenReturn("AND");
        when(compositeSection.getMinimumRequired()).thenReturn(1);
        when(compositeSection.getMaximumRequired()).thenReturn(2);
        when(compositeSection.getAllowPartialProgress()).thenReturn(true);
        when(compositeSection.getCompositeRequirements()).thenReturn(List.of());

        ChoiceRequirementSection choiceSection = mock(ChoiceRequirementSection.class);
        when(choiceSection.getMinimumRequired()).thenReturn(1);
        when(choiceSection.getMaximumRequired()).thenReturn(3);
        when(choiceSection.getAllowPartialProgress()).thenReturn(false);
        when(choiceSection.getChoices()).thenReturn(List.of());

        AchievementRequirementSection achievementSection = mock(AchievementRequirementSection.class);
        when(achievementSection.getRequiredAchievements()).thenReturn(List.of("first_blood"));
        when(achievementSection.getRequireAll()).thenReturn(true);
        when(achievementSection.getAchievementPlugin()).thenReturn("advancedachievements");

        SkillRequirementSection skillSection = mock(SkillRequirementSection.class);
        when(skillSection.getRequiredSkills()).thenReturn(Map.of("mining", 10));
        when(skillSection.getConsumeOnComplete()).thenReturn(true);
        when(skillSection.getSkillPlugin()).thenReturn("ecoskills");

        JobRequirementSection jobSection = mock(JobRequirementSection.class);
        when(jobSection.getRequiredJobs()).thenReturn(Map.of("miner", 12));
        when(jobSection.getConsumeOnComplete()).thenReturn(true);
        when(jobSection.getJobPlugin()).thenReturn("jobs_reborn");

        TimeBasedRequirementSection timeSection = mock(TimeBasedRequirementSection.class);
        when(timeSection.getTimeConstraintSeconds()).thenReturn(3600L);
        when(timeSection.getCooldownSeconds()).thenReturn(60L);
        when(timeSection.getTimeZone()).thenReturn("UTC");
        when(timeSection.getRecurring()).thenReturn(false);
        when(timeSection.getUseRealTime()).thenReturn(true);

        List<AConfigSection> sections = List.of(
                itemSection,
                currencySection,
                experienceSection,
                playtimeSection,
                permissionSection,
                locationSection,
                compositeSection,
                choiceSection,
                achievementSection,
                skillSection,
                jobSection,
                timeSection
        );

        List<Consumer<Map<String, Object>>> assertions = List.of(
                map -> {
                    assertEquals("ITEM", map.get("type"));
                    assertTrue(map.containsKey("requiredItems"));
                },
                map -> {
                    assertEquals("CURRENCY", map.get("type"));
                    assertEquals("vault", map.get("currencyPlugin"));
                },
                map -> {
                    assertEquals("EXPERIENCE_LEVEL", map.get("type"));
                    assertEquals(5, map.get("requiredLevel"));
                },
                map -> {
                    assertEquals("PLAYTIME", map.get("type"));
                    assertEquals(7200, map.get("requiredPlaytimeSeconds"));
                },
                map -> {
                    assertEquals("PERMISSION", map.get("type"));
                    assertEquals(List.of("rdq.test"), map.get("requiredPermissions"));
                },
                map -> {
                    assertEquals("LOCATION", map.get("type"));
                    assertEquals("world", map.get("requiredWorld"));
                },
                map -> {
                    assertEquals("COMPOSITE", map.get("type"));
                    assertTrue(map.containsKey("requirements"));
                },
                map -> {
                    assertEquals("CHOICE", map.get("type"));
                    assertTrue(map.containsKey("choices"));
                },
                map -> {
                    assertEquals("CUSTOM", map.get("type"));
                    assertTrue(map.containsKey("requiredAchievements"));
                },
                map -> {
                    assertEquals("SKILLS", map.get("type"));
                    assertTrue(map.containsKey("requiredSkills"));
                },
                map -> {
                    assertEquals("JOBS", map.get("type"));
                    assertTrue(map.containsKey("requiredJobs"));
                },
                map -> {
                    assertEquals("TIME_BASED", map.get("type"));
                    assertEquals(3600, map.get("timeConstraintSeconds"));
                }
        );

        List<AbstractRequirement> expectedRequirements = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            expectedRequirements.add(mock(AbstractRequirement.class));
        }

        AtomicInteger index = new AtomicInteger();
        try (MockedStatic<RequirementParser> parserMock = Mockito.mockStatic(RequirementParser.class)) {
            parserMock.when(() -> RequirementParser.parse(anyString())).thenAnswer(invocation -> {
                int current = index.getAndIncrement();
                String json = invocation.getArgument(0);
                Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<>() {});
                assertions.get(current).accept(parsed);
                return expectedRequirements.get(current);
            });

            for (int i = 0; i < sections.size(); i++) {
                AbstractRequirement result = factory.createRequirementFromSection(sections.get(i));
                assertSame(expectedRequirements.get(i), result);
            }
        }

        assertEquals(sections.size(), index.get());
    }

    @Test
    void determineShowcaseMaterialFallsBackWhenIconInvalid() {
        BaseRequirementSection valid = mock(BaseRequirementSection.class);
        IconSection validIcon = createIcon("DIAMOND_BLOCK");
        when(valid.getIcon()).thenReturn(validIcon);
        assertEquals(Material.DIAMOND_BLOCK, RequirementFactory.determineShowcaseMaterial(valid));

        BaseRequirementSection currency = mock(BaseRequirementSection.class);
        IconSection badIcon = createIcon("invalid_material");
        when(currency.getIcon()).thenReturn(badIcon);
        when(currency.getType()).thenReturn("CURRENCY");
        assertEquals(Material.GOLD_INGOT, RequirementFactory.determineShowcaseMaterial(currency));

        BaseRequirementSection timeBased = mock(BaseRequirementSection.class);
        IconSection missingIcon = createIcon(null);
        when(timeBased.getIcon()).thenReturn(missingIcon);
        when(timeBased.getType()).thenReturn("TIME_BASED");
        assertEquals(Material.CLOCK, RequirementFactory.determineShowcaseMaterial(timeBased));

        BaseRequirementSection unknown = mock(BaseRequirementSection.class);
        IconSection unknownIcon = createIcon(" ");
        when(unknown.getIcon()).thenReturn(unknownIcon);
        when(unknown.getType()).thenReturn(null);
        assertEquals(Material.PAPER, RequirementFactory.determineShowcaseMaterial(unknown));
    }
}
