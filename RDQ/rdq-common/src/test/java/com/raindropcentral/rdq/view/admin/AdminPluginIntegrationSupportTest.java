package com.raindropcentral.rdq.view.admin;

import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests plugin detection and alias normalization for RDQ admin integration views.
 *
 * @author ItsRainingHP
 * @since 6.0.0
 * @version 6.0.0
 */
class AdminPluginIntegrationSupportTest {

    @Test
    void normalizePluginKeyCanonicalizesSkillAliases() {
        assertEquals("auraskills", AdminPluginIntegrationSupport.normalizePluginKey("Aura"));
        assertEquals("auraskills", AdminPluginIntegrationSupport.normalizePluginKey("AuraSkills"));
        assertEquals("auraskills", AdminPluginIntegrationSupport.normalizePluginKey("AureliumSkills"));
    }

    @Test
    void normalizePluginKeyCanonicalizesJobAliases() {
        assertEquals("jobsreborn", AdminPluginIntegrationSupport.normalizePluginKey("Jobs"));
        assertEquals("jobsreborn", AdminPluginIntegrationSupport.normalizePluginKey("JobsReborn"));
        assertEquals("ecojobs", AdminPluginIntegrationSupport.normalizePluginKey("EcoJobsPlugin"));
    }

    @Test
    void detectsEnabledPluginsByAlias() {
        final Plugin[] installed = new Plugin[]{
            this.plugin("Jobs", true),
            this.plugin("EcoSkills", true)
        };

        assertTrue(
            AdminPluginIntegrationSupport.isPluginDetected(List.of("JobsReborn"), installed)
        );
        assertTrue(
            AdminPluginIntegrationSupport.isPluginDetected(List.of("EcoSkills"), installed)
        );
    }

    @Test
    void ignoresDisabledPluginsDuringDetection() {
        final Plugin[] installed = new Plugin[]{
            this.plugin("EcoJobs", false),
            this.plugin("AuraSkills", false)
        };

        assertFalse(
            AdminPluginIntegrationSupport.isPluginDetected(List.of("EcoJobs"), installed)
        );
        assertFalse(
            AdminPluginIntegrationSupport.isPluginDetected(List.of("Aura"), installed)
        );
    }

    @Test
    void skillDetectionsReflectInstalledPluginSet() {
        final Plugin[] installed = new Plugin[]{
            this.plugin("mcMMO", true),
            this.plugin("AuraSkills", true),
            this.plugin("EcoJobs", true)
        };

        final Map<String, Boolean> detections = AdminPluginIntegrationSupport.detectSkillPlugins(installed)
            .stream()
            .collect(Collectors.toMap(
                AdminPluginIntegrationSupport.PluginDetectionEntry::integrationId,
                AdminPluginIntegrationSupport.PluginDetectionEntry::detected
            ));

        assertEquals(3, detections.size());
        assertTrue(detections.get("mcmmo"));
        assertTrue(detections.get("auraskills"));
        assertFalse(detections.get("ecoskills"));
    }

    @Test
    void jobDetectionsReflectInstalledPluginSet() {
        final Plugin[] installed = new Plugin[]{
            this.plugin("Jobs", true),
            this.plugin("EcoSkills", true)
        };

        final List<AdminPluginIntegrationSupport.PluginDetectionEntry> detections =
            AdminPluginIntegrationSupport.detectJobPlugins(installed);
        final Map<String, Boolean> detectionMap = detections.stream().collect(Collectors.toMap(
            AdminPluginIntegrationSupport.PluginDetectionEntry::integrationId,
            AdminPluginIntegrationSupport.PluginDetectionEntry::detected
        ));

        assertEquals(2, detectionMap.size());
        assertFalse(detectionMap.get("ecojobs"));
        assertTrue(detectionMap.get("jobsreborn"));
        assertEquals(
            1,
            AdminPluginIntegrationSupport.countDetectedEntries(detections)
        );
    }

    @Test
    void detectionEntriesRetainConfiguredIcons() {
        final Map<String, Material> iconById = AdminPluginIntegrationSupport.detectJobPlugins(new Plugin[0])
            .stream()
            .collect(Collectors.toMap(
                AdminPluginIntegrationSupport.PluginDetectionEntry::integrationId,
                AdminPluginIntegrationSupport.PluginDetectionEntry::iconType
            ));

        assertEquals(Material.DIAMOND_PICKAXE, iconById.get("ecojobs"));
        assertEquals(Material.GOLDEN_PICKAXE, iconById.get("jobsreborn"));
    }

    private @NotNull Plugin plugin(
        final @NotNull String name,
        final boolean enabled
    ) {
        return (Plugin) Proxy.newProxyInstance(
            Plugin.class.getClassLoader(),
            new Class<?>[]{Plugin.class},
            (proxy, method, args) -> {
                if ("getName".equals(method.getName())) {
                    return name;
                }
                if ("isEnabled".equals(method.getName())) {
                    return enabled;
                }
                return this.defaultValue(method.getReturnType());
            }
        );
    }

    private Object defaultValue(
        final @NotNull Class<?> returnType
    ) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0.0F;
        }
        if (returnType == double.class) {
            return 0.0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }
}
