package com.raindropcentral.rdq.database.json.requirement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import com.raindropcentral.rdq.requirement.ChoiceRequirement;
import com.raindropcentral.rdq.requirement.CustomRequirement;
import com.raindropcentral.rdq.requirement.PermissionRequirement;
import com.raindropcentral.rdq.requirement.PlaytimeRequirement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class RequirementMixinTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.objectMapper = new ObjectMapper()
                .addMixIn(AbstractRequirement.class, RequirementMixin.class);
    }

    static Stream<Arguments> requirementPayloads() {
        return Stream.of(
                Arguments.of(
                        """
                        {
                            "type": "PERMISSION",
                            "requiredPermissions": ["rdq.requirement.test"],
                            "permissionMode": "ALL",
                            "minimumRequired": 1,
                            "description": "Grant quest requirement",
                            "checkNegated": false
                        }
                        """,
                        PermissionRequirement.class,
                        AbstractRequirement.Type.PERMISSION
                ),
                Arguments.of(
                        """
                        {
                            "type": "CUSTOM",
                            "customType": "SCRIPT",
                            "customScript": "player.level > threshold",
                            "progressScript": "0.25",
                            "consumeScript": "void 0",
                            "customData": {"threshold": 5},
                            "description": "Scripted requirement",
                            "cacheScripts": true
                        }
                        """,
                        CustomRequirement.class,
                        AbstractRequirement.Type.CUSTOM
                ),
                Arguments.of(
                        """
                        {
                            "type": "PLAYTIME",
                            "requiredPlaytimeSeconds": 3600,
                            "worldPlaytimeRequirements": {"world": 600},
                            "useTotalPlaytime": true,
                            "description": "Play for an hour"
                        }
                        """,
                        PlaytimeRequirement.class,
                        AbstractRequirement.Type.PLAYTIME
                ),
                Arguments.of(
                        """
                        {
                            "type": "CHOICE",
                            "choices": [
                                {
                                    "type": "PERMISSION",
                                    "requiredPermissions": ["rdq.choice.alpha"],
                                    "permissionMode": "ALL",
                                    "minimumRequired": 1,
                                    "checkNegated": false
                                },
                                {
                                    "type": "CUSTOM",
                                    "customType": "SCRIPT",
                                    "customScript": "true",
                                    "customData": {}
                                }
                            ],
                            "minimumChoicesRequired": 1,
                            "description": "Select a path",
                            "allowPartialProgress": true
                        }
                        """,
                        ChoiceRequirement.class,
                        AbstractRequirement.Type.CHOICE
                )
        );
    }

    @ParameterizedTest
    @MethodSource("requirementPayloads")
    void itDeserializesKnownRequirementPayloads(
            final String json,
            final Class<? extends AbstractRequirement> expectedType,
            final AbstractRequirement.Type expectedDiscriminator
    ) throws Exception {
        final AbstractRequirement requirement = this.objectMapper.readValue(json, AbstractRequirement.class);

        assertInstanceOf(expectedType, requirement);
        assertEquals(expectedDiscriminator, requirement.getType());
    }

    @Test
    void itRetainsTypePropertyDuringSerialization() throws Exception {
        final PermissionRequirement requirement = new PermissionRequirement(List.of("rdq.requirement.type"));

        final String json = this.objectMapper.writeValueAsString(requirement);
        final JsonNode node = this.objectMapper.readTree(json);

        assertTrue(node.has("type"), "Serialized payload should include the type discriminator");
        assertEquals("PERMISSION", node.get("type").asText());
    }

    @Test
    void itFailsForUnknownTypesWithInformativeException() {
        final String json = """
                {
                    "type": "UNKNOWN",
                    "requiredPermissions": ["rdq.unknown"]
                }
                """;

        final InvalidTypeIdException exception = assertThrows(
                InvalidTypeIdException.class,
                () -> this.objectMapper.readValue(json, AbstractRequirement.class)
        );

        assertEquals("UNKNOWN", exception.getTypeId());
        assertTrue(exception.getMessage().contains("UNKNOWN"),
                "Exception message should mention the unknown type");
        assertTrue(exception.getMessage().contains(AbstractRequirement.class.getName()),
                "Exception message should mention the base requirement type");
    }
}
