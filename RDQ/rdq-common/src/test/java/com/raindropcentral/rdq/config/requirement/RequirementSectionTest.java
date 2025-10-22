package com.raindropcentral.rdq.config.requirement;

import com.raindropcentral.rdq.config.item.IconSection;
import de.jexcellence.evaluable.section.ItemStackSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequirementSectionTest {

    @Test
    void itProvidesDefaultsForTypeAndIconWhenUnset() {
        final RequirementSection section = new RequirementSection(new EvaluationEnvironmentBuilder());

        assertEquals("ITEM", section.getType(), "getType should default to ITEM when unset");

        final IconSection firstIcon = section.getIcon();
        final IconSection secondIcon = section.getIcon();
        assertNotNull(firstIcon, "getIcon should never return null when unset");
        assertNotSame(firstIcon, secondIcon, "getIcon should return new instances for unset values");
    }

    @Test
    void itAggregatesItemCurrencyAndAchievementSources() throws Exception {
        final RequirementSection section = new RequirementSection(new EvaluationEnvironmentBuilder());

        final ItemStack mappedStack = new ItemStack(Material.DIAMOND);
        final ItemStackSection mappedSection = mock(ItemStackSection.class, Answers.RETURNS_DEEP_STUBS);
        when(mappedSection.asItem().build()).thenReturn(mappedStack);

        final ItemStack directStack = new ItemStack(Material.EMERALD, 2);

        final ItemStack singleStack = new ItemStack(Material.GOLD_INGOT);
        final ItemStackSection singleSection = mock(ItemStackSection.class, Answers.RETURNS_DEEP_STUBS);
        when(singleSection.asItem().build()).thenReturn(singleStack);

        final Map<String, ItemStackSection> requiredItems = new HashMap<>();
        requiredItems.put("mapped", mappedSection);

        final List<ItemStack> items = new ArrayList<>();
        items.add(directStack);

        setField(section, "requiredItems", requiredItems);
        setField(section, "items", items);
        setField(section, "requiredItem", singleSection);
        setField(section, "requiredAmount", 3);

        final Map<String, Double> requiredCurrencies = new HashMap<>();
        requiredCurrencies.put("coins", 12.5D);
        setField(section, "requiredCurrencies", requiredCurrencies);
        setField(section, "currency", "tokens");
        setField(section, "amount", 4.5D);

        final List<String> requiredAchievements = new ArrayList<>();
        requiredAchievements.add("achievement:required");
        final List<String> achievements = new ArrayList<>();
        achievements.add("achievement:legacy");
        setField(section, "requiredAchievements", requiredAchievements);
        setField(section, "achievements", achievements);
        setField(section, "achievement", "achievement:single");

        final List<ItemStack> aggregatedItems = section.getRequiredItemsList();
        assertEquals(3, aggregatedItems.size(), "getRequiredItemsList should combine mapped, direct, and single items");
        assertTrue(aggregatedItems.contains(mappedStack), "getRequiredItemsList should include mapped items");
        assertTrue(aggregatedItems.contains(directStack), "getRequiredItemsList should include direct items");
        assertTrue(aggregatedItems.contains(singleStack), "getRequiredItemsList should include single items");
        assertEquals(3, singleStack.getAmount(), "getRequiredItemsList should apply requiredAmount to single items");

        final Map<String, Double> currencies = section.getRequiredCurrencies();
        assertEquals(2, currencies.size(), "getRequiredCurrencies should combine map and scalar entries");
        assertEquals(12.5D, currencies.get("coins"));
        assertEquals(4.5D, currencies.get("tokens"));

        final List<String> combinedAchievements = section.getRequiredAchievements();
        final List<String> expectedAchievements = List.of(
                "achievement:required",
                "achievement:legacy",
                "achievement:single"
        );
        assertEquals(expectedAchievements, combinedAchievements, "getRequiredAchievements should merge list and scalar values without duplicates");
    }

    @Test
    void itMergesLegacyFieldsForRanksSkillsJobsAndOperators() throws Exception {
        final RequirementSection section = new RequirementSection(new EvaluationEnvironmentBuilder());

        setField(section, "requiredPreviousRanks", new ArrayList<>(List.of("rank-one")));
        setField(section, "requiredPreviousRank", null);
        setField(section, "previousRank", "rank-legacy");
        setField(section, "requiredPreviousRankTree", null);
        setField(section, "previousRankTree", "tree-legacy");

        final Map<String, Integer> requiredSkills = new HashMap<>();
        requiredSkills.put("alchemy", 5);
        final Map<String, Integer> legacySkills = new HashMap<>();
        legacySkills.put("foraging", 3);
        setField(section, "requiredSkills", requiredSkills);
        setField(section, "skills", legacySkills);
        setField(section, "requiredSkill", null);
        setField(section, "skill", "farming");
        setField(section, "requiredSkillLevel", null);
        setField(section, "skillLevel", 7);

        final Map<String, Integer> requiredJobs = new HashMap<>();
        requiredJobs.put("miner", 4);
        final Map<String, Integer> legacyJobs = new HashMap<>();
        legacyJobs.put("hunter", 2);
        setField(section, "requiredJobs", requiredJobs);
        setField(section, "jobs", legacyJobs);
        setField(section, "requiredJob", null);
        setField(section, "job", "builder");
        setField(section, "requiredJobLevel", null);
        setField(section, "jobLevel", 6);

        setField(section, "operator", null);
        setField(section, "compositeOperator", "XOR");

        final List<String> previousRanks = section.getRequiredPreviousRanks();
        assertEquals(List.of("rank-one", "rank-legacy"), previousRanks, "getRequiredPreviousRanks should merge list values with legacy single value");
        assertEquals("tree-legacy", section.getRequiredPreviousRankTree(), "getRequiredPreviousRankTree should fall back to previousRankTree when primary field unset");

        final Map<String, Integer> combinedSkills = section.getRequiredSkills();
        final Map<String, Integer> expectedSkills = new HashMap<>();
        expectedSkills.put("alchemy", 5);
        expectedSkills.put("foraging", 3);
        expectedSkills.put("farming", 7);
        assertEquals(expectedSkills, combinedSkills, "getRequiredSkills should merge maps and single skill fallback");

        final Map<String, Integer> combinedJobs = section.getRequiredJobs();
        final Map<String, Integer> expectedJobs = new HashMap<>();
        expectedJobs.put("miner", 4);
        expectedJobs.put("hunter", 2);
        expectedJobs.put("builder", 6);
        assertEquals(expectedJobs, combinedJobs, "getRequiredJobs should merge maps and single job fallback");

        assertEquals("XOR", section.getOperator(), "getOperator should use compositeOperator when operator field unset");

        setField(section, "compositeOperator", null);
        assertEquals("AND", section.getOperator(), "getOperator should default to AND when no operator fields set");
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = locateField(target.getClass(), name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Field locateField(final Class<?> type, final String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (final NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
