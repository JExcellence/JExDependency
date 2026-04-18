package de.jexcellence.jexplatform.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA converter for item lore lists stored as semicolon-delimited
 * Base64-encoded strings.
 *
 * <p>Each line is UTF-8 encoded before Base64. {@code null} entries
 * are preserved as empty tokens to maintain lore ordering.
 *
 * @author JExcellence
 * @since 1.0.0
 */
@Converter(autoApply = true)
public class LoreConverter implements AttributeConverter<List<String>, String> {

    private static final String DELIM = ";";
    private static final Base64.Encoder B64_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder B64_DECODER = Base64.getDecoder();

    @Override
    public String convertToDatabaseColumn(@Nullable List<String> lore) {
        if (lore == null) {
            return null;
        }
        if (lore.isEmpty()) {
            return "";
        }
        return lore.stream()
                .map(line -> line == null
                        ? ""
                        : B64_ENCODER.encodeToString(line.getBytes(StandardCharsets.UTF_8)))
                .collect(Collectors.joining(DELIM));
    }

    @Override
    public List<String> convertToEntityAttribute(@Nullable String columnValue) {
        if (columnValue == null) {
            return null;
        }
        if (columnValue.isBlank()) {
            return new ArrayList<>();
        }

        var tokens = columnValue.split(DELIM, -1);
        var result = new ArrayList<String>(tokens.length);

        for (var token : tokens) {
            if (token == null || token.isEmpty()) {
                result.add("");
            } else {
                try {
                    result.add(new String(B64_DECODER.decode(token), StandardCharsets.UTF_8));
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException(
                            "Invalid Base64 lore token encountered.", ex);
                }
            }
        }

        return result;
    }
}
