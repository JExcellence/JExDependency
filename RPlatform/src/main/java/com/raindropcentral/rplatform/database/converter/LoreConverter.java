package com.raindropcentral.rplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * JPA attribute converter for mapping List&lt;String&gt; lore to a semicolon-delimited Base64 string and back.
 *
 * Behavior:
 * - null list -> null column; empty list -> empty string
 * - null column -> null list; blank column -> empty list
 * - empty line stored as empty token to preserve position
 */
@Converter(autoApply = true)
public class LoreConverter implements AttributeConverter<List<String>, String> {

    private static final String DELIM = ";";
    private static final Base64.Encoder B64_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder B64_DECODER = Base64.getDecoder();

    @Override
    public String convertToDatabaseColumn(@Nullable final List<String> lore) {
        if (lore == null) {
            return null;
        }
        if (lore.isEmpty()) {
            return "";
        }
        return lore.stream()
                .map(line -> {
                    if (line == null) {
                        return ""; // preserve slot with empty token
                    }
                    final byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
                    return B64_ENCODER.encodeToString(bytes);
                })
                .collect(Collectors.joining(DELIM));
    }

    @Override
    public List<String> convertToEntityAttribute(@Nullable final String columnValue) {
        if (columnValue == null) {
            return null;
        }
        if (columnValue.isBlank()) {
            return new ArrayList<>();
        }

        final String[] tokens = columnValue.split(DELIM, -1);
        final List<String> result = new ArrayList<>(tokens.length);

        for (String token : tokens) {
            if (token == null || token.isEmpty()) {
                result.add("");
            } else {
                try {
                    final byte[] bytes = B64_DECODER.decode(token);
                    result.add(new String(bytes, StandardCharsets.UTF_8));
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Invalid Base64 lore token encountered.", ex);
                }
            }
        }

        return result;
    }
}