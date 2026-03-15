package com.raindropcentral.rplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts item lore {@link List lists} to a semicolon-delimited Base64 string for persistence and rebuilds.
 * them during entity hydration.
 *
 * <p>Every line is encoded as UTF-8 bytes before applying Base64, preserving {@code null} entries as empty
 * tokens so lore order remains stable. {@code null} lists map to {@code null} columns, while blank column
 * values create empty lists. Any decoding failure raises an {@link IllegalArgumentException}.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Converter(autoApply = true)
public class LoreConverter implements AttributeConverter<List<String>, String> {

    /** Delimiter separating lore tokens within the column representation. */
    private static final String DELIM = ";";
    /** Encoder converting lore lines into Base64 text. */
    private static final Base64.Encoder B64_ENCODER = Base64.getEncoder();
    /** Decoder restoring lore lines from Base64 text. */
    private static final Base64.Decoder B64_DECODER = Base64.getDecoder();

    /**
     * Serialises the provided lore lines to the Base64 column payload.
     *
     * @param lore the lore lines targeted for persistence; may be {@code null}
     * @return {@code null} when {@code lore} is {@code null}, an empty string for empty lists, or the joined payload otherwise
     */
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

    /**
     * Rehydrates lore lines from the Base64 column payload.
     *
     * @param columnValue the column data; {@code null} returns {@code null} and blank values yield an empty list
     * @return the reconstructed lore lines, never {@code null} unless {@code columnValue} is {@code null}
     * @throws IllegalArgumentException when a token cannot be decoded from Base64
     */
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
