/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

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
