package com.raindropcentral.rdq.database.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raindropcentral.rdq.config.item.IconSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IconSectionConverterTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final IconSectionConverter converter = new IconSectionConverter();

    @Test
    void convertToDatabaseColumnSerializesSection() throws Exception {
        final IconSection section = new IconSection(new EvaluationEnvironmentBuilder());
        section.setMaterial("DIAMOND");
        section.setDisplayNameKey("icon.display");
        section.setDescriptionKey("icon.description");

        final String json = this.converter.convertToDatabaseColumn(section);

        assertNotNull(json);

        final JsonNode root = OBJECT_MAPPER.readTree(json);
        assertEquals("DIAMOND", root.path("material").asText());
        assertEquals("icon.display", root.path("displayNameKey").asText());
        assertEquals("icon.description", root.path("descriptionKey").asText());
    }

    @Test
    void convertToDatabaseColumnReturnsNullForNullSection() {
        assertNull(this.converter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttributeReturnsDefaultSectionWhenJsonNull() {
        final IconSection section = this.converter.convertToEntityAttribute(null);

        assertNotNull(section);
        assertEquals("PAPER", section.getMaterial());
        assertEquals("not_defined", section.getDisplayNameKey());
        assertEquals("not_defined", section.getDescriptionKey());
    }

    @Test
    void convertToEntityAttributeRestoresValidMaterial() {
        final IconSection original = new IconSection(new EvaluationEnvironmentBuilder());
        original.setMaterial("STONE");
        original.setDisplayNameKey("icon.stone.name");
        original.setDescriptionKey("icon.stone.desc");

        final String json = this.converter.convertToDatabaseColumn(original);
        final IconSection restored = this.converter.convertToEntityAttribute(json);

        assertEquals("STONE", restored.getMaterial());
        assertEquals("icon.stone.name", restored.getDisplayNameKey());
        assertEquals("icon.stone.desc", restored.getDescriptionKey());
    }

    @Test
    void convertToEntityAttributeFallsBackToBarrierOnInvalidMaterial() {
        final IconSection original = new IconSection(new EvaluationEnvironmentBuilder());
        original.setMaterial("NOT_A_MATERIAL");
        original.setDisplayNameKey("icon.invalid.name");
        original.setDescriptionKey("icon.invalid.desc");

        final String json = this.converter.convertToDatabaseColumn(original);
        final IconSection restored = this.converter.convertToEntityAttribute(json);

        assertEquals("BARRIER", restored.getMaterial());
        assertEquals("icon.invalid.name", restored.getDisplayNameKey());
        assertEquals("icon.invalid.desc", restored.getDescriptionKey());
    }

    @Test
    void convertToEntityAttributeThrowsRuntimeExceptionOnMalformedJson() {
        final RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> this.converter.convertToEntityAttribute("{invalid")
        );

        assertEquals("Failed to deserialize IconSection", exception.getMessage());
    }
}
