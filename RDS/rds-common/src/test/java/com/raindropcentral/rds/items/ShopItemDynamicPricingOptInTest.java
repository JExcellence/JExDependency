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

package com.raindropcentral.rds.items;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests dynamic-pricing opt-in behavior on {@link ShopItem}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ShopItemDynamicPricingOptInTest {

    @Test
    void declaresDynamicPricingFieldAsJsonProperty() throws NoSuchFieldException {
        final Field field = ShopItem.class.getDeclaredField("dynamicPricingEnabled");
        final JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
        assertNotNull(jsonProperty);
        assertEquals("dynamicPricingEnabled", jsonProperty.value());
    }

    @Test
    void jsonCreatorConstructorSupportsDynamicPricingProperty() {
        final Constructor<?> jsonCreatorConstructor = Arrays.stream(ShopItem.class.getDeclaredConstructors())
                .filter(this::hasJsonCreatorAnnotation)
                .findFirst()
                .orElseThrow();

        final boolean hasDynamicPricingParameter = Arrays.stream(jsonCreatorConstructor.getParameters())
                .map(this::extractJsonPropertyName)
                .anyMatch("dynamicPricingEnabled"::equals);
        assertTrue(hasDynamicPricingParameter);
    }

    private boolean hasJsonCreatorAnnotation(final Constructor<?> constructor) {
        return Arrays.stream(constructor.getAnnotations())
                .map(Annotation::annotationType)
                .anyMatch(JsonCreator.class::equals);
    }

    private String extractJsonPropertyName(final Parameter parameter) {
        final JsonProperty jsonProperty = parameter.getAnnotation(JsonProperty.class);
        return jsonProperty == null ? "" : jsonProperty.value();
    }
}
