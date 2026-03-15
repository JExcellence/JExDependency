package com.raindropcentral.rplatform.requirement.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Jackson mixin for AbstractRequirement to handle polymorphic deserialization.
 *
 * <p>Types are registered dynamically through RequirementRegistry.configureObjectMapper()
 * instead of being hardcoded here.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    visible = true
)
/**
 * Represents the RequirementMixin API type.
 */
public abstract class RequirementMixin {
    // Mixin class for Jackson annotations - no implementation needed
    // Types are registered dynamically via RequirementRegistry
}
