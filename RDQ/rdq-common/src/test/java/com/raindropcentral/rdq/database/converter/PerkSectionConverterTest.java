package com.raindropcentral.rdq.database.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.config.perk.PerkSettingsSection;
import com.raindropcentral.rdq.config.perk.PluginCurrencySection;
import com.raindropcentral.rdq.config.requirement.RequirementSection;
import com.raindropcentral.rdq.config.reward.RewardSection;
import com.raindropcentral.rplatform.config.DurationSection;
import com.raindropcentral.rplatform.config.permission.PermissionAmplifierSection;
import com.raindropcentral.rplatform.config.permission.PermissionCooldownSection;
import com.raindropcentral.rplatform.config.permission.PermissionDurationSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class PerkSectionConverterTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PerkSectionConverter converter = new PerkSectionConverter();
    private final Logger                logger    = Logger.getLogger(PerkSectionConverter.class.getName());

    private TestLogHandler handler;

    @BeforeEach
    void setUp() {
        this.handler = new TestLogHandler();
        this.logger.addHandler(this.handler);
    }

    @AfterEach
    void tearDown() {
        this.logger.removeHandler(this.handler);
    }

    @Test
    void convertToDatabaseColumnSerializesSection() throws Exception {
        final PerkSection section = this.createPopulatedSection();

        final String json = this.converter.convertToDatabaseColumn(section);

        assertNotNull(json);

        final JsonNode root = OBJECT_MAPPER.readTree(json);

        assertEquals("perk.speed.name", root.path("perkSettings").path("displayNameKey").asText());
        assertEquals("perk.speed.lore", root.path("perkSettings").path("descriptionKey").asText());
        assertEquals(5, root.path("perkSettings").path("priority").asInt());
        assertEquals("SUGAR", root.path("perkSettings").path("icon").path("material").asText());

        assertEquals(5L, root.path("permissionCooldowns").path("defaultCooldownSeconds").asLong());
        assertEquals(
                3L,
                root.path("permissionCooldowns").path("permissionCooldowns").path("rdq.perk.speed.vip").asLong()
        );

        assertEquals(1, root.path("permissionAmplifiers").path("defaultAmplifier").asInt());
        assertEquals(
                2,
                root.path("permissionAmplifiers").path("permissionAmplifiers").path("rdq.perk.speed.vip").asInt()
        );
        assertEquals(3, root.path("permissionAmplifiers").path("maxAmplifier").asInt());
        assertEquals(0, root.path("permissionAmplifiers").path("minAmplifier").asInt());

        assertEquals(30L, root.path("permissionDurations").path("defaultDuration").path("seconds").asLong());
        assertEquals(
                120L,
                root.path("permissionDurations").path("permissionDurations").path("rdq.perk.speed.vip").path("minutes").asLong()
        );

        assertEquals(
                "Vault",
                root.path("costs").path("economy").path("targetPluginId").asText()
        );
        assertEquals(250.0, root.path("costs").path("economy").path("amount").asDouble());

        assertEquals("LEVEL", root.path("requirements").path("level").path("type").asText());
        assertEquals("COMMAND", root.path("rewards").path("command").path("type").asText());
    }

    @Test
    void convertToEntityAttributeReturnsEmptySectionWhenJsonNull() {
        final PerkSection section = this.converter.convertToEntityAttribute(null);

        assertNotNull(section);
        assertTrue(section.getCosts().isEmpty());
        assertTrue(section.getRequirements().isEmpty());
        assertTrue(section.getRewards().isEmpty());
    }

    @Test
    void convertToEntityAttributeReturnsEmptySectionWhenJsonBlank() {
        final PerkSection section = this.converter.convertToEntityAttribute("   ");

        assertNotNull(section);
        assertTrue(section.getCosts().isEmpty());
        assertTrue(section.getRequirements().isEmpty());
        assertTrue(section.getRewards().isEmpty());
    }

    @Test
    void convertToEntityAttributeRestoresFields() throws Exception {
        final PerkSection original = this.createPopulatedSection();
        final String json = this.converter.convertToDatabaseColumn(original);

        final PerkSection restored = this.converter.convertToEntityAttribute(json);

        final PerkSettingsSection restoredSettings = this.getField(restored, "perkSettings", PerkSettingsSection.class);
        assertNotNull(restoredSettings);
        assertEquals("perk.speed.name", restoredSettings.getDisplayNameKey());
        assertEquals("perk.speed.lore", restoredSettings.getDescriptionKey());
        assertEquals(5, restoredSettings.getPriority());

        final PermissionCooldownSection restoredCooldowns =
                this.getField(restored, "permissionCooldowns", PermissionCooldownSection.class);
        assertNotNull(restoredCooldowns);
        assertEquals(5L, restoredCooldowns.getDefaultCooldownSeconds());
        assertEquals(3L, restoredCooldowns.getPermissionCooldowns().get("rdq.perk.speed.vip"));

        final PermissionAmplifierSection restoredAmplifiers =
                this.getField(restored, "permissionAmplifiers", PermissionAmplifierSection.class);
        assertNotNull(restoredAmplifiers);
        assertEquals(1, restoredAmplifiers.getDefaultAmplifier());
        assertEquals(2, restoredAmplifiers.getPermissionAmplifiers().get("rdq.perk.speed.vip"));

        final PermissionDurationSection restoredDurations =
                this.getField(restored, "permissionDurations", PermissionDurationSection.class);
        assertNotNull(restoredDurations);
        assertEquals(30L, restoredDurations.getDefaultDuration().getSeconds());
        assertEquals(
                120L,
                restoredDurations.getPermissionDurations().get("rdq.perk.speed.vip").getMinutes()
        );

        final Map<String, PluginCurrencySection> restoredCosts = this.getField(restored, "costs", Map.class);
        assertEquals(1, restoredCosts.size());
        assertEquals("Vault", restoredCosts.get("economy").getTargetPluginId());

        final Map<String, RequirementSection> restoredRequirements = this.getField(restored, "requirements", Map.class);
        assertEquals(1, restoredRequirements.size());
        assertEquals("LEVEL", restoredRequirements.get("level").getType());

        final Map<String, RewardSection> restoredRewards = this.getField(restored, "rewards", Map.class);
        assertEquals(1, restoredRewards.size());
        assertEquals("COMMAND", restoredRewards.get("command").getType());
    }

    @Test
    void convertToEntityAttributeLogsAndThrowsOnMalformedJson() {
        final RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> this.converter.convertToEntityAttribute("{invalid")
        );

        assertEquals("Failed to deserialize PerkSection", exception.getMessage());
        final LogRecord record = this.handler.getLastRecord();
        assertNotNull(record);
        assertTrue(record.getMessage().contains("Failed to convert JSON to PerkSection"));
        assertEquals(Level.SEVERE, record.getLevel());
    }

    private PerkSection createPopulatedSection() throws Exception {
        final PerkSection section = new PerkSection(new EvaluationEnvironmentBuilder());

        final PerkSettingsSection settings = new PerkSettingsSection(new EvaluationEnvironmentBuilder());
        final IconSection icon = new IconSection(new EvaluationEnvironmentBuilder());
        icon.setMaterial("SUGAR");
        icon.setDisplayNameKey("perk.speed.name");
        icon.setDescriptionKey("perk.speed.lore");
        this.setField(settings, "icon", icon);
        this.setField(settings, "displayNameKey", "perk.speed.name");
        this.setField(settings, "descriptionKey", "perk.speed.lore");
        this.setField(settings, "priority", 5);

        final PermissionCooldownSection cooldowns = new PermissionCooldownSection(new EvaluationEnvironmentBuilder());
        this.setField(cooldowns, "defaultCooldownSeconds", 5L);
        final Map<String, Long> cooldownMap = new LinkedHashMap<>();
        cooldownMap.put("rdq.perk.speed.vip", 3L);
        this.setField(cooldowns, "permissionCooldowns", cooldownMap);

        final PermissionAmplifierSection amplifiers = new PermissionAmplifierSection(new EvaluationEnvironmentBuilder());
        this.setField(amplifiers, "defaultAmplifier", 1);
        final Map<String, Integer> amplifierMap = new LinkedHashMap<>();
        amplifierMap.put("rdq.perk.speed.vip", 2);
        this.setField(amplifiers, "permissionAmplifiers", amplifierMap);
        this.setField(amplifiers, "maxAmplifier", 3);
        this.setField(amplifiers, "minAmplifier", 0);

        final PermissionDurationSection durations = new PermissionDurationSection(new EvaluationEnvironmentBuilder());
        final DurationSection defaultDuration = new DurationSection(new EvaluationEnvironmentBuilder());
        this.setField(defaultDuration, "seconds", 30L);
        this.setField(durations, "defaultDuration", defaultDuration);
        final Map<String, DurationSection> durationMap = new LinkedHashMap<>();
        final DurationSection vipDuration = new DurationSection(new EvaluationEnvironmentBuilder());
        this.setField(vipDuration, "minutes", 120L);
        durationMap.put("rdq.perk.speed.vip", vipDuration);
        this.setField(durations, "permissionDurations", durationMap);

        final Map<String, PluginCurrencySection> costs = new LinkedHashMap<>();
        final PluginCurrencySection currency = new PluginCurrencySection(new EvaluationEnvironmentBuilder());
        this.setField(currency, "targetPluginId", "Vault");
        this.setField(currency, "currencyTypeId", "money");
        this.setField(currency, "amount", 250.0);
        costs.put("economy", currency);

        final Map<String, RequirementSection> requirements = new LinkedHashMap<>();
        final RequirementSection requirement = new RequirementSection(new EvaluationEnvironmentBuilder());
        this.setField(requirement, "type", "LEVEL");
        requirements.put("level", requirement);

        final Map<String, RewardSection> rewards = new LinkedHashMap<>();
        final RewardSection reward = new RewardSection(new EvaluationEnvironmentBuilder());
        this.setField(reward, "type", "COMMAND");
        rewards.put("command", reward);

        this.setField(section, "perkSettings", settings);
        this.setField(section, "permissionCooldowns", cooldowns);
        this.setField(section, "permissionAmplifiers", amplifiers);
        this.setField(section, "permissionDurations", durations);
        this.setField(section, "costs", costs);
        this.setField(section, "requirements", requirements);
        this.setField(section, "rewards", rewards);

        return section;
    }

    private void setField(final Object target, final String fieldName, final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(final Object target, final String fieldName, final Class<T> type) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private static final class TestLogHandler extends Handler {

        private LogRecord lastRecord;

        @Override
        public void publish(final LogRecord record) {
            this.lastRecord = record;
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public void close() {
            // no-op
        }

        LogRecord getLastRecord() {
            return this.lastRecord;
        }
    }
}
