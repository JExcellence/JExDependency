/**
 * Model classes for the progression system.
 * <p>
 * This package contains immutable data models that represent the state
 * of progression nodes:
 * </p>
 * <ul>
 *     <li>{@link com.raindropcentral.rplatform.progression.model.ProgressionStatus} - Status enumeration</li>
 *     <li>{@link com.raindropcentral.rplatform.progression.model.ProgressionState} - State record</li>
 * </ul>
 *
 * <h2>Design Principles:</h2>
 * <ul>
 *     <li>Immutability - All models are immutable for thread safety</li>
 *     <li>Type Safety - Generic types ensure compile-time correctness</li>
 *     <li>Null Safety - All fields are non-null with validation</li>
 *     <li>Defensive Copying - Collections are copied to prevent modification</li>
 * </ul>
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
package com.raindropcentral.rplatform.progression.model;
