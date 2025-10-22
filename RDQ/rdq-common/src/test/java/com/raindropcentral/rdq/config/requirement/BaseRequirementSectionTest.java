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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseRequirementSectionTest {

    @Test
    void itAutoDetectsRequirementTypesAfterParsing() throws Exception {
        assertDetectedType("ITEM", section -> {
            final ItemRequirementSection item = new ItemRequirementSection(new EvaluationEnvironmentBuilder());
            final ItemStackSection stackSection = mock(ItemStackSection.class, Answers.RETURNS_DEEP_STUBS);
            when(stackSection.asItem().build()).thenReturn(new ItemStack(Material.DIAMOND));

            final Map<String, ItemStackSection> requiredItems = new HashMap<>();
            requiredItems.put("diamond", stackSection);
            setField(item, "requiredItems", requiredItems);
            setField(section, "itemRequirement", item);
        });

        assertDetectedType("CURRENCY", section -> {
            final CurrencyRequirementSection currency = new CurrencyRequirementSection(new EvaluationEnvironmentBuilder());
            final Map<String, Double> requiredCurrencies = new HashMap<>();
            requiredCurrencies.put("coins", 12.5D);
            setField(currency, "requiredCurrencies", requiredCurrencies);
            setField(section, "currencyRequirement", currency);
        });

        assertDetectedType("EXPERIENCE_LEVEL", section -> {
            final ExperienceLevelRequirementSection experience = new ExperienceLevelRequirementSection(new EvaluationEnvironmentBuilder());
            setField(experience, "requiredExperience", 5);
            setField(section, "experienceRequirement", experience);
        });

        assertDetectedType("PLAYTIME", section -> {
            final PlaytimeRequirementSection playtime = new PlaytimeRequirementSection(new EvaluationEnvironmentBuilder());
            setField(playtime, "requiredPlaytimeSeconds", 600L);
            setField(section, "playtimeRequirement", playtime);
        });

        assertDetectedType("PERMISSION", section -> {
            final PermissionRequirementSection permission = new PermissionRequirementSection(new EvaluationEnvironmentBuilder());
            setField(permission, "requiredPermissions", new ArrayList<>(List.of("rdq.permission.use")));
            setField(section, "permissionRequirement", permission);
        });

        assertDetectedType("LOCATION", section -> {
            final LocationRequirementSection location = new LocationRequirementSection(new EvaluationEnvironmentBuilder());
            setField(section, "locationRequirement", location);
        });

        assertDetectedType("COMPOSITE", section -> {
            final CompositeRequirementSection composite = new CompositeRequirementSection(new EvaluationEnvironmentBuilder());
            final List<BaseRequirementSection> requirements = new ArrayList<>();
            requirements.add(new BaseRequirementSection(new EvaluationEnvironmentBuilder()));
            setField(composite, "requirements", requirements);
            setField(section, "compositeRequirement", composite);
        });

        assertDetectedType("CHOICE", section -> {
            final ChoiceRequirementSection choice = new ChoiceRequirementSection(new EvaluationEnvironmentBuilder());
            final List<BaseRequirementSection> choices = new ArrayList<>();
            choices.add(new BaseRequirementSection(new EvaluationEnvironmentBuilder()));
            setField(choice, "choices", choices);
            setField(section, "choiceRequirement", choice);
        });

        assertDetectedType("ACHIEVEMENT", section -> {
            final AchievementRequirementSection achievement = new AchievementRequirementSection(new EvaluationEnvironmentBuilder());
            setField(achievement, "requiredAchievements", new ArrayList<>(List.of("rdq:achieve")));
            setField(section, "achievementRequirement", achievement);
        });

        assertDetectedType("SKILLS", section -> {
            final SkillRequirementSection skill = new SkillRequirementSection(new EvaluationEnvironmentBuilder());
            final Map<String, Integer> requiredSkills = new HashMap<>();
            requiredSkills.put("alchemy", 5);
            setField(skill, "requiredSkills", requiredSkills);
            setField(section, "skillRequirement", skill);
        });

        assertDetectedType("JOBS", section -> {
            final JobRequirementSection job = new JobRequirementSection(new EvaluationEnvironmentBuilder());
            final Map<String, Integer> requiredJobs = new HashMap<>();
            requiredJobs.put("miner", 3);
            setField(job, "requiredJobs", requiredJobs);
            setField(section, "jobRequirement", job);
        });

        assertDetectedType("TIME_BASED", section -> {
            final TimeBasedRequirementSection time = new TimeBasedRequirementSection(new EvaluationEnvironmentBuilder());
            setField(time, "timeConstraintSeconds", 120L);
            setField(section, "timeBasedRequirement", time);
        });

        assertDetectedType("UNKNOWN", section -> {
            // No nested requirement populated to force UNKNOWN fallback.
        });
    }

    @Test
    void itGeneratesIconKeysWhenContextIsProvided() throws Exception {
        final BaseRequirementSection section = new BaseRequirementSection(new EvaluationEnvironmentBuilder());
        final IconSection icon = new IconSection(new EvaluationEnvironmentBuilder());
        setField(section, "icon", icon);

        final ExperienceLevelRequirementSection experience = new ExperienceLevelRequirementSection(new EvaluationEnvironmentBuilder());
        setField(experience, "requiredExperience", 8);
        setField(section, "experienceRequirement", experience);

        section.setContext("warrior", "novice", "req-1");
        section.afterParsing(Collections.emptyList());

        assertEquals("EXPERIENCE_LEVEL", section.getType(), "afterParsing should detect experience type");

        final IconSection processedIcon = section.getIcon();
        assertEquals("requirement.warrior.experience_level.name", processedIcon.getDisplayNameKey(),
                "afterParsing should generate a localized display name key using the context");
        assertEquals("requirement.warrior.experience_level.lore", processedIcon.getDescriptionKey(),
                "afterParsing should generate a localized description key using the context");
        assertEquals("warrior", section.getRankTreeName());
        assertEquals("novice", section.getRankName());
        assertEquals("req-1", section.getRequirementKey());
    }

    @Test
    void itProvidesDefaultSubsectionsWhenUnset() {
        final BaseRequirementSection section = new BaseRequirementSection(new EvaluationEnvironmentBuilder());

        assertProvidesDefaultInstance(section::getIcon, IconSection.class, "getIcon should supply a default icon");
        assertProvidesDefaultInstance(section::getItemRequirement, ItemRequirementSection.class,
                "getItemRequirement should provide a default item section");
        assertProvidesDefaultInstance(section::getCurrencyRequirement, CurrencyRequirementSection.class,
                "getCurrencyRequirement should provide a default currency section");
        assertProvidesDefaultInstance(section::getExperienceRequirement, ExperienceLevelRequirementSection.class,
                "getExperienceRequirement should provide a default experience section");
        assertProvidesDefaultInstance(section::getPlaytimeRequirement, PlaytimeRequirementSection.class,
                "getPlaytimeRequirement should provide a default playtime section");
        assertProvidesDefaultInstance(section::getPermissionRequirement, PermissionRequirementSection.class,
                "getPermissionRequirement should provide a default permission section");
        assertProvidesDefaultInstance(section::getLocationRequirement, LocationRequirementSection.class,
                "getLocationRequirement should provide a default location section");
        assertProvidesDefaultInstance(section::getCompositeRequirement, CompositeRequirementSection.class,
                "getCompositeRequirement should provide a default composite section");
        assertProvidesDefaultInstance(section::getChoiceRequirement, ChoiceRequirementSection.class,
                "getChoiceRequirement should provide a default choice section");
        assertProvidesDefaultInstance(section::getAchievementRequirement, AchievementRequirementSection.class,
                "getAchievementRequirement should provide a default achievement section");
        assertProvidesDefaultInstance(section::getSkillRequirement, SkillRequirementSection.class,
                "getSkillRequirement should provide a default skill section");
        assertProvidesDefaultInstance(section::getJobRequirement, JobRequirementSection.class,
                "getJobRequirement should provide a default job section");
        assertProvidesDefaultInstance(section::getTimeBasedRequirement, TimeBasedRequirementSection.class,
                "getTimeBasedRequirement should provide a default time-based section");
    }

    private void assertDetectedType(final String expectedType, final SectionConfigurator configurator) throws Exception {
        final BaseRequirementSection section = new BaseRequirementSection(new EvaluationEnvironmentBuilder());
        configurator.configure(section);
        section.afterParsing(Collections.emptyList());
        assertEquals(expectedType, section.getType(),
                "afterParsing should detect the correct requirement type for " + expectedType);
    }

    private <T> void assertProvidesDefaultInstance(final Supplier<T> supplier, final Class<?> expectedType,
                                                    final String message) {
        final T first = supplier.get();
        final T second = supplier.get();

        assertNotNull(first, message);
        assertNotNull(second, message);
        assertEquals(expectedType, first.getClass(), message);
        assertEquals(expectedType, second.getClass(), message);
        assertNotSame(first, second, message + " and return new instances for successive calls");
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final Field field = locateField(target.getClass(), name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Field locateField(Class<?> type, final String name) throws NoSuchFieldException {
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

    @FunctionalInterface
    private interface SectionConfigurator {
        void configure(BaseRequirementSection section) throws Exception;
    }
}
