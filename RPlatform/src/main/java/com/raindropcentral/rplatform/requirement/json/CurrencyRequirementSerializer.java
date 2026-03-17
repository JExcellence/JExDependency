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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.raindropcentral.rplatform.requirement.impl.CurrencyRequirement;

import java.io.IOException;

/**
 * Custom Jackson serializer for CurrencyRequirement.
 * This prevents Jackson from introspecting methods that might reference
 * optional dependencies (JExEconomy classes) that may not be available at runtime.
 */
public class CurrencyRequirementSerializer extends StdSerializer<CurrencyRequirement> {

    /**
     * Executes CurrencyRequirementSerializer.
     */
    public CurrencyRequirementSerializer() {
        super(CurrencyRequirement.class);
    }

    /**
     * Executes serialize.
     */
    @Override
    public void serialize(CurrencyRequirement requirement, JsonGenerator gen, SerializerProvider provider) 
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField("type", requirement.getTypeId());
        gen.writeStringField("currency", requirement.getCurrencyId());
        gen.writeNumberField("amount", requirement.getAmount());
        gen.writeBooleanField("consumable", requirement.isConsumable());
        gen.writeEndObject();
    }

    /**
     * Executes serializeWithType.
     */
    @Override
    public void serializeWithType(CurrencyRequirement requirement, JsonGenerator gen, 
                                   SerializerProvider provider, TypeSerializer typeSer) throws IOException {
        // For polymorphic serialization, just use the regular serialize method
        // The type information is already included in the "type" field
        serialize(requirement, gen, provider);
    }
}
