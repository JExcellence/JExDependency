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

    public CurrencyRequirementSerializer() {
        super(CurrencyRequirement.class);
    }

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

    @Override
    public void serializeWithType(CurrencyRequirement requirement, JsonGenerator gen, 
                                   SerializerProvider provider, TypeSerializer typeSer) throws IOException {
        // For polymorphic serialization, just use the regular serialize method
        // The type information is already included in the "type" field
        serialize(requirement, gen, provider);
    }
}
