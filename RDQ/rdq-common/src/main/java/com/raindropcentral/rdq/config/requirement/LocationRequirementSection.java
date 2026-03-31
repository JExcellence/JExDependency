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

package com.raindropcentral.rdq.config.requirement;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration section for location-based requirements.
 *
 * <p>This section handles all configuration options specific to LocationRequirement,
 * including world, region, coordinate, and distance requirements.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class LocationRequirementSection extends AConfigSection {
	
	// ~~~ LOCATION-SPECIFIC PROPERTIES ~~~
	
	/**
	 * Required world name.
	 * YAML key: "requiredWorld"
	 */
	private String requiredWorld;
	
	/**
	 * Alternative world field name.
	 * YAML key: "world"
	 */
	private String world;
	
	/**
	 * Required region name (WorldGuard integration).
	 * YAML key: "requiredRegion"
	 */
	private String requiredRegion;
	
	/**
	 * Alternative region field name.
	 * YAML key: "region"
	 */
	private String region;
	
	/**
	 * Required coordinates map (x, y, z).
	 * YAML key: "requiredCoordinates"
	 */
	private Map<String, Double> requiredCoordinates;
	
	/**
	 * Alternative coordinates field name.
	 * YAML key: "coordinates"
	 */
	private Map<String, Double> coordinates;
	
	/**
	 * Required distance from coordinates.
	 * YAML key: "requiredDistance"
	 */
	private Double requiredDistance;
	
	/**
	 * Alternative distance field name.
	 * YAML key: "distance"
	 */
	private Double distance;
	
	/**
	 * X coordinate for simple coordinate specification.
	 * YAML key: "x"
	 */
	private Double x;
	
	/**
	 * Y coordinate for simple coordinate specification.
	 * YAML key: "y"
	 */
	private Double y;
	
	/**
	 * Z coordinate for simple coordinate specification.
	 * YAML key: "z"
	 */
	private Double z;
	
	/**
	 * Whether to check exact coordinates or allow distance tolerance.
	 * YAML key: "exactLocation"
	 */
	private Boolean exactLocation;
	
	/**
	 * Constructs a new LocationRequirementSection.
	 *
	 * @param evaluationEnvironmentBuilder the evaluation environment builder
	 */
	public LocationRequirementSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
		super(evaluationEnvironmentBuilder);
	}
	
	// ~~~ GETTERS ~~~
	
	/**
	 * Gets exactLocation.
	 */
	public Boolean getExactLocation() {
		return this.exactLocation != null ? this.exactLocation : false;
	}
	
	/**
	 * Gets the required world name, trying multiple field names.
	 *
	 * @return the required world name
	 */
	public String getRequiredWorld() {
		if (this.requiredWorld != null) {
			return this.requiredWorld;
		}
		if (this.world != null) {
			return this.world;
		}
		return "";
	}
	
	/**
	 * Gets the required region name, trying multiple field names.
	 *
	 * @return the required region name
	 */
	public String getRequiredRegion() {
		if (this.requiredRegion != null) {
			return this.requiredRegion;
		}
		if (this.region != null) {
			return this.region;
		}
		return "";
	}
	
	/**
	 * Gets the required coordinates, combining all coordinate sources.
	 *
	 * @return the map of required coordinates
	 */
	public Map<String, Double> getRequiredCoordinates() {
		Map<String, Double> coords = new HashMap<>();
		
		// Add coordinates from requiredCoordinates map
		if (this.requiredCoordinates != null) {
			coords.putAll(this.requiredCoordinates);
		}
		
		// Add coordinates from alternative coordinates map
		if (this.coordinates != null) {
			coords.putAll(this.coordinates);
		}
		
		// Add individual coordinate fields
		if (this.x != null) {
			coords.put("x", this.x);
		}
		if (this.y != null) {
			coords.put("y", this.y);
		}
		if (this.z != null) {
			coords.put("z", this.z);
		}
		
		return coords;
	}
	
	/**
	 * Gets the required distance, trying multiple field names.
	 *
	 * @return the required distance
	 */
	public Double getRequiredDistance() {
		if (this.requiredDistance != null) {
			return this.requiredDistance;
		}
		if (this.distance != null) {
			return this.distance;
		}
		return 0.0;
	}
	
	/**
	 * Gets x.
	 */
	public Double getX() {
		return this.x;
	}
	
	/**
	 * Gets y.
	 */
	public Double getY() {
		return this.y;
	}
	
	/**
	 * Gets z.
	 */
	public Double getZ() {
		return this.z;
	}
}
