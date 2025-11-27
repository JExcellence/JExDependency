package com.raindropcentral.rdq.database.converter;


import com.raindropcentral.rdq.database.json.requirement.RequirementParser;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Converter(autoApply = true)
public class RequirementConverter implements AttributeConverter<AbstractRequirement, String> {

    private static final Logger logger = LoggerFactory.getLogger(RequirementConverter.class);

    @Override
    public String convertToDatabaseColumn(AbstractRequirement attribute) {
        if (attribute == null) return null;
        
        try {
            return RequirementParser.serialize(attribute);
        } catch (IOException e) {
            logger.error("Failed to serialize requirement: {}", attribute, e);
            throw new RuntimeException("Failed to serialize requirement", e);
        }
    }

    @Override
    public AbstractRequirement convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        
        try {
            return RequirementParser.parse(dbData);
        } catch (IOException e) {
            logger.error("Failed to deserialize requirement from database string: {}", dbData, e);
            throw new RuntimeException("Failed to deserialize requirement", e);
        }
    }
}
