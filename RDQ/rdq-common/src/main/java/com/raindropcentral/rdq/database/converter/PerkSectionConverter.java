package com.raindropcentral.rdq.database.converter;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.config.perk.PerkSettingsSection;
import com.raindropcentral.rdq.config.perk.PluginCurrencySection;
import com.raindropcentral.rdq.config.requirement.BaseRequirementSection;
import com.raindropcentral.rdq.config.reward.RewardSection;
import com.raindropcentral.rplatform.config.permission.PermissionAmplifierSection;
import com.raindropcentral.rplatform.config.permission.PermissionCooldownSection;
import com.raindropcentral.rplatform.config.permission.PermissionDurationSection;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converter that serializes {@link PerkSection} instances into a JSON payload for persistence and
 * reconstructs them when loading entities from the database.
 *
 * <p>The converter relies on {@link ConverterTool} to access the non-public properties that back
 * the section implementations.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Converter(autoApply = true)
public class PerkSectionConverter implements AttributeConverter<PerkSection, String> {

    private static final Logger LOGGER = Logger.getLogger(PerkSectionConverter.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();
    private static final ConverterTool CONVERTER_TOOL = new ConverterTool();

    /**
     * Mixin to ignore AConfigSection fields during serialization/deserialization.
     */
    @JsonIgnoreProperties({"baseEnvironment", "builtBaseEnvironment", "fieldDefaultSuppliers"})
    private abstract static class AConfigSectionMixin {
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper()
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        
        // Add mixin to ignore AConfigSection internal fields
        mapper.addMixIn(AConfigSection.class, AConfigSectionMixin.class);
        
        // Add mixin to ignore @type for costs, requirements, and rewards - we know the concrete types
        mapper.addMixIn(PluginCurrencySection.class, IgnoreTypeInfoMixin.class);
        mapper.addMixIn(BaseRequirementSection.class, IgnoreTypeInfoMixin.class);
        mapper.addMixIn(RewardSection.class, IgnoreTypeInfoMixin.class);
        
        // Register custom deserializers for all AConfigSection subclasses
        com.fasterxml.jackson.databind.module.SimpleModule module = new com.fasterxml.jackson.databind.module.SimpleModule();
        module.addDeserializer(PermissionCooldownSection.class, new PermissionSectionDeserializer<>(PermissionCooldownSection.class));
        module.addDeserializer(PermissionAmplifierSection.class, new PermissionSectionDeserializer<>(PermissionAmplifierSection.class));
        module.addDeserializer(PermissionDurationSection.class, new PermissionSectionDeserializer<>(PermissionDurationSection.class));
        module.addDeserializer(PluginCurrencySection.class, new PermissionSectionDeserializer<>(PluginCurrencySection.class));
        module.addDeserializer(BaseRequirementSection.class, new PermissionSectionDeserializer<>(BaseRequirementSection.class));
        module.addDeserializer(RewardSection.class, new PermissionSectionDeserializer<>(RewardSection.class));
        module.addDeserializer(PerkSettingsSection.class, new PermissionSectionDeserializer<>(PerkSettingsSection.class));
        mapper.registerModule(module);
        
        return mapper;
    }

    /**
     * Mixin to ignore @type property during deserialization.
     */
    @JsonIgnoreProperties({"@type"})
    private abstract static class IgnoreTypeInfoMixin {
    }

    /**
     * Converts the provided {@link PerkSection} into a database friendly representation.
     *
     * @param perkSection the section being persisted
     * @return JSON content suitable for storage (currently unimplemented)
     */
    @Override
    public String convertToDatabaseColumn(final PerkSection perkSection) {
        try {
            if (perkSection == null) {
                return null;
            }

            final PerkSectionData data = new PerkSectionData(
                    perkSection.getPerkSettings(),
                    perkSection.getPermissionCooldowns(),
                    perkSection.getPermissionAmplifiers(),
                    perkSection.getPermissionDurations(),
                    perkSection.getCosts(),
                    perkSection.getRequirements(),
                    perkSection.getRewards()
            );

            return OBJECT_MAPPER.writeValueAsString(data);
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to serialize PerkSection to JSON", exception);
            throw new RuntimeException("Failed to serialize PerkSection", exception);
        }
    }

    /**
     * Restores a {@link PerkSection} from the JSON payload stored in the database.
     *
     * @param jsonString the JSON value retrieved from persistence, may be {@code null}
     * @return the reconstructed {@link PerkSection}
     */
    @Override
    public PerkSection convertToEntityAttribute(
            final @Nullable String jsonString
    ) {

        if (
                jsonString == null || jsonString.trim().isEmpty()
        )
            return new PerkSection(new EvaluationEnvironmentBuilder());

        try {
            final JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonString);
            final PerkSectionData data = OBJECT_MAPPER.treeToValue(
                    jsonNode,
                    PerkSectionData.class
            );

            final PerkSection perkSection = new PerkSection(new EvaluationEnvironmentBuilder());

            CONVERTER_TOOL.setPrivateField(
                    perkSection,
                    "perkSettings",
                    data.perkSettingsSection,
                    LOGGER
            );

            CONVERTER_TOOL.setPrivateField(
                    perkSection,
                    "permissionAmplifiers",
                    data.permissionAmplifiers,
                    LOGGER
            );

            CONVERTER_TOOL.setPrivateField(
                    perkSection,
                    "permissionDurations",
                    data.permissionDurations,
                    LOGGER
            );

            CONVERTER_TOOL.setPrivateField(
                    perkSection,
                    "permissionCooldowns",
                    data.permissionCooldowns,
                    LOGGER
            );

            CONVERTER_TOOL.setPrivateField(
                    perkSection,
                    "costs",
                    data.costs,
                    LOGGER
            );

            CONVERTER_TOOL.setPrivateField(
                    perkSection,
                    "requirements",
                    data.requirements,
                    LOGGER
            );

            CONVERTER_TOOL.setPrivateField(
                    perkSection,
                    "rewards",
                    data.rewards,
                    LOGGER
            );

            return perkSection;
        } catch (
                final Exception exception
        ) {
            LOGGER.log(
                    Level.SEVERE,
                    "Failed to convert JSON to PerkSection: " + jsonString,
                    exception
            );
            throw new RuntimeException(
                    "Failed to deserialize PerkSection",
                    exception
            );
        }
    }

    /**
     * Jackson mapped representation of the {@link PerkSection} internals.
     */
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PerkSectionData {
        private PerkSettingsSection perkSettingsSection;
        private PermissionCooldownSection permissionCooldowns;
        private PermissionAmplifierSection permissionAmplifiers;
        private PermissionDurationSection permissionDurations;
        private Map<String, PluginCurrencySection> costs;
        private Map<String, BaseRequirementSection> requirements;
        private Map<String, RewardSection> rewards;
        //public Boolean requiresOwnedArea;

        /**
         * Creates an empty data container for Jackson.
         */
        public PerkSectionData() {}

        /**
         * Creates a populated data container for JSON mapping.
         *
         */
        public PerkSectionData(
                final @Nullable PerkSettingsSection perkSettingsSection,
                final @Nullable PermissionCooldownSection permissionCooldownSection,
                final @Nullable PermissionAmplifierSection permissionAmplifierSection,
                final @Nullable PermissionDurationSection permissionDurationSection,
                final @Nullable Map<String, PluginCurrencySection> costs,
                final @Nullable Map<String, BaseRequirementSection> requirements,
                final @Nullable Map<String, RewardSection> rewards
        ) {
            this.perkSettingsSection = perkSettingsSection;
            this.permissionCooldowns = permissionCooldownSection;
            this.permissionAmplifiers = permissionAmplifierSection;
            this.permissionDurations = permissionDurationSection;
            this.costs = costs;
            this.requirements = requirements;
            this.rewards = rewards;
        }

    }
}