package com.raindropcentral.rdq.database.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raindropcentral.rdq.config.perk.PerkSection;
import com.raindropcentral.rdq.config.perk.PerkSettingsSection;
import com.raindropcentral.rdq.config.perk.PluginCurrencySection;
import com.raindropcentral.rdq.config.requirement.RequirementSection;
import com.raindropcentral.rdq.config.reward.RewardSection;
import com.raindropcentral.rplatform.config.permission.PermissionAmplifierSection;
import com.raindropcentral.rplatform.config.permission.PermissionCooldownSection;
import com.raindropcentral.rplatform.config.permission.PermissionDurationSection;
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
 * @version 1.0.2
 */
@Converter(autoApply = true)
public class PerkSectionConverter implements AttributeConverter<PerkSection, String> {

        private static final Logger        LOGGER         = Logger.getLogger(PerkSectionConverter.class.getName());
	private static final ObjectMapper  OBJECT_MAPPER  = new ObjectMapper();
	private static final ConverterTool CONVERTER_TOOL = new ConverterTool();
	
        /**
         * Converts the provided {@link PerkSection} into a database friendly representation.
         *
         * @param perkSection the section being persisted
         * @return JSON content suitable for storage (currently unimplemented)
         */
        @Override
        public String convertToDatabaseColumn(final PerkSection perkSection) {

                if (perkSection == null)
                        return null;

                try {
                        final PerkSectionData data = new PerkSectionData(
                                CONVERTER_TOOL.getPrivateField(perkSection, "perkSettings", LOGGER),
                                CONVERTER_TOOL.getPrivateField(perkSection, "permissionCooldowns", LOGGER),
                                CONVERTER_TOOL.getPrivateField(perkSection, "permissionAmplifiers", LOGGER),
                                CONVERTER_TOOL.getPrivateField(perkSection, "permissionDurations", LOGGER),
                                CONVERTER_TOOL.getPrivateField(perkSection, "costs", LOGGER),
                                CONVERTER_TOOL.getPrivateField(perkSection, "requirements", LOGGER),
                                CONVERTER_TOOL.getPrivateField(perkSection, "rewards", LOGGER)
                        );

                        return OBJECT_MAPPER.writeValueAsString(data);
                } catch (final Exception exception) {
                        LOGGER.log(
                                Level.SEVERE,
                                "Failed to convert PerkSection to JSON",
                                exception
                        );
                        throw new RuntimeException(
                                "Failed to serialize PerkSection",
                                exception
                        );
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
                        final PerkSectionData data = OBJECT_MAPPER.readValue(
                                jsonString,
                                PerkSectionData.class
                        );

                        final PerkSection perkSection = new PerkSection(new EvaluationEnvironmentBuilder());

                        CONVERTER_TOOL.setPrivateField(
                                perkSection,
                                "perkSettings",
                                data.perkSettings,
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
        private static class PerkSectionData {

                public PerkSettingsSection perkSettings;
                public PermissionCooldownSection permissionCooldowns;
                public PermissionAmplifierSection permissionAmplifiers;
                public PermissionDurationSection permissionDurations;
                public Map<String, PluginCurrencySection> costs;
                public Map<String, RequirementSection>    requirements;
                public Map<String, RewardSection>         rewards;

                /**
                 * Creates an empty data container for Jackson.
                 */
                public PerkSectionData() {}

                /**
                 * Creates a populated data container for JSON mapping.
                 *
                 * @param perkSettings settings applied to the perk
                 * @param permissionCooldowns cooldown configuration for permissions
                 * @param permissionAmplifiers amplifier configuration for permissions
                 * @param permissionDurations duration configuration for permissions
                 * @param costs currency costs keyed by edition
                 * @param requirements requirement configuration keyed by identifier
                 * @param rewards reward configuration keyed by identifier
                 */
                public PerkSectionData(
                        final @Nullable PerkSettingsSection perkSettings,
                        final @Nullable PermissionCooldownSection permissionCooldowns,
                        final @Nullable PermissionAmplifierSection permissionAmplifiers,
                        final @Nullable PermissionDurationSection permissionDurations,
                        final @Nullable Map<String, PluginCurrencySection> costs,
                        final @Nullable Map<String, RequirementSection> requirements,
                        final @Nullable Map<String, RewardSection> rewards
                ) {

                        this.perkSettings = perkSettings;
                        this.permissionCooldowns = permissionCooldowns;
                        this.permissionAmplifiers = permissionAmplifiers;
                        this.permissionDurations = permissionDurations;
                        this.costs = costs;
                        this.requirements = requirements;
                        this.rewards = rewards;
                }

        }

}