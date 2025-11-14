package com.raindropcentral.rdq.database.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;

/**
 * Custom Jackson deserializer for configuration sections that require an
 * {@link EvaluationEnvironmentBuilder} constructor parameter.
 *
 * @param <T> the config section type
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.0
 */
public class PermissionSectionDeserializer<T extends AConfigSection> extends JsonDeserializer<T> {

    private final Class<T> targetClass;

    public PermissionSectionDeserializer(final Class<T> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public T deserialize(
            final JsonParser parser,
            final DeserializationContext context
    ) throws IOException {
        try {
            // Create instance using the EvaluationEnvironmentBuilder constructor
            final Constructor<T> constructor = targetClass.getDeclaredConstructor(EvaluationEnvironmentBuilder.class);
            constructor.setAccessible(true);
            final T instance = constructor.newInstance(new EvaluationEnvironmentBuilder());

            // Parse JSON and populate fields
            final JsonNode node = parser.getCodec().readTree(parser);
            populateFields(instance, node);

            return instance;
        } catch (final Exception e) {
            throw new IOException("Failed to deserialize " + targetClass.getSimpleName(), e);
        }
    }

    private Field findField(final Class<?> clazz, final String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (final NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private Object convertValue(final JsonNode node, final Field field) {
        if (node.isNull()) {
            return null;
        }

        final Class<?> targetType = field.getType();

        if (targetType == Long.class || targetType == long.class) {
            return node.asLong();
        } else if (targetType == Integer.class || targetType == int.class) {
            return node.asInt();
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return node.asBoolean();
        } else if (targetType == String.class) {
            return node.asText();
        } else if (targetType == Double.class || targetType == double.class) {
            return node.asDouble();
        } else if (AConfigSection.class.isAssignableFrom(targetType)) {
            // For nested AConfigSection objects, recursively deserialize
            try {
                @SuppressWarnings("unchecked")
                final Class<? extends AConfigSection> sectionClass = (Class<? extends AConfigSection>) targetType;
                final Constructor<? extends AConfigSection> constructor = sectionClass.getDeclaredConstructor(EvaluationEnvironmentBuilder.class);
                constructor.setAccessible(true);
                final AConfigSection instance = constructor.newInstance(new EvaluationEnvironmentBuilder());
                populateFields(instance, node);
                return instance;
            } catch (final Exception e) {
                return null;
            }
        } else if (Map.class.isAssignableFrom(targetType)) {
            // For maps, deserialize and convert values based on generic type
            try {
                final Map<String, Object> rawMap = new com.fasterxml.jackson.databind.ObjectMapper().treeToValue(node, Map.class);
                final Map<String, Object> convertedMap = new java.util.HashMap<>();
                
                // Determine the map value type from generics
                Class<?> mapValueType = Object.class;
                final Type genericType = field.getGenericType();
                if (genericType instanceof ParameterizedType) {
                    final ParameterizedType paramType = (ParameterizedType) genericType;
                    final Type[] typeArgs = paramType.getActualTypeArguments();
                    if (typeArgs.length == 2 && typeArgs[1] instanceof Class) {
                        mapValueType = (Class<?>) typeArgs[1];
                    }
                }
                
                // Convert values to the proper type
                for (final Map.Entry<String, Object> entry : rawMap.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof Number) {
                        if (mapValueType == Long.class || mapValueType == long.class) {
                            value = ((Number) value).longValue();
                        } else if (mapValueType == Integer.class || mapValueType == int.class) {
                            value = ((Number) value).intValue();
                        } else if (mapValueType == Double.class || mapValueType == double.class) {
                            value = ((Number) value).doubleValue();
                        }
                    }
                    convertedMap.put(entry.getKey(), value);
                }
                return convertedMap;
            } catch (final Exception e) {
                return null;
            }
        }

        return null;
    }
    
    @SuppressWarnings("unchecked")
    private void populateFields(final AConfigSection instance, final JsonNode node) throws Exception {
        final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            final Map.Entry<String, JsonNode> entry = fields.next();
            final String fieldName = entry.getKey();
            final JsonNode fieldValue = entry.getValue();

            try {
                final Field field = findField(instance.getClass(), fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    final Object value = convertValue(fieldValue, field);
                    field.set(instance, value);
                }
            } catch (final Exception e) {
                // Log but don't fail on individual field errors
            }
        }
    }
}
