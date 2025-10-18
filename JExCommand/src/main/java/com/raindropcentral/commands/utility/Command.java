package com.raindropcentral.commands.utility;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation used to flag command handler classes for discovery and automatic
 * registration within the command factory.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {}
