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
 * Configuration section for job-based requirements.
 *
 * <p>This section handles all configuration options specific to JobRequirement,
 * including required jobs, job levels, and job plugin integration.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@CSAlways
public class JobRequirementSection extends AConfigSection {
	
	// ~~~ JOB-SPECIFIC PROPERTIES ~~~
	
	/**
	 * Single required job name.
	 * YAML key: "requiredJob"
	 */
	private String requiredJob;
	
	/**
	 * Alternative job field name.
	 * YAML key: "job"
	 */
	private String job;
	
	/**
	 * Required job level for single job.
	 * YAML key: "requiredJobLevel"
	 */
	private Integer requiredJobLevel;
	
	/**
	 * Alternative job level field name.
	 * YAML key: "jobLevel"
	 */
	private Integer jobLevel;
	
	/**
	 * Map of required jobs with their levels.
	 * YAML key: "requiredJobs"
	 */
	private Map<String, Integer> requiredJobs;
	
	/**
	 * Alternative jobs map field name.
	 * YAML key: "jobs"
	 */
	private Map<String, Integer> jobs;
	
	/**
	 * Job plugin identifier (e.g., "jobs", "jobsreborn").
	 * YAML key: "jobPlugin"
	 */
	private String jobPlugin;
	
	/**
	 * Whether this requirement should consume job levels when completed.
	 * YAML key: "consumeOnComplete"
	 */
	private Boolean consumeOnComplete;
	
	/**
	 * Whether all jobs must be at required level (AND) or just one (OR).
	 * YAML key: "requireAll"
	 */
	private Boolean requireAll;
	
	/**
	 * Constructs a new JobRequirementSection.
	 *
	 * @param evaluationEnvironmentBuilder the evaluation environment builder
	 */
	public JobRequirementSection(EvaluationEnvironmentBuilder evaluationEnvironmentBuilder) {
		super(evaluationEnvironmentBuilder);
	}
	
	// ~~~ GETTERS ~~~
	
	/**
	 * Gets the single required job, trying multiple field names.
	 *
	 * @return the required job name
	 */
	public String getRequiredJob() {
		if (this.requiredJob != null) {
			return this.requiredJob;
		}
		if (this.job != null) {
			return this.job;
		}
		return "";
	}
	
	/**
	 * Gets the required job level, trying multiple field names.
	 *
	 * @return the required job level, defaulting to 1
	 */
	public Integer getRequiredJobLevel() {
		if (this.requiredJobLevel != null) {
			return this.requiredJobLevel;
		}
		if (this.jobLevel != null) {
			return this.jobLevel;
		}
		return 1;
	}
	
	/**
	 * Gets the complete map of required jobs from all sources.
	 *
	 * @return combined map of all required jobs
	 */
	public Map<String, Integer> getRequiredJobs() {
		Map<String, Integer> jobMap = new HashMap<>();
		
		// Add jobs from requiredJobs map
		if (this.requiredJobs != null) {
			jobMap.putAll(this.requiredJobs);
		}
		
		// Add jobs from alternative jobs map
		if (this.jobs != null) {
			jobMap.putAll(this.jobs);
		}
		
		// Add single job if specified
		String singleJob = getRequiredJob();
		if (!singleJob.isEmpty()) {
			jobMap.put(singleJob, getRequiredJobLevel());
		}
		
		return jobMap;
	}
	
	/**
	 * Gets the job plugin identifier.
	 *
	 * @return the job plugin identifier
	 */
	public String getJobPlugin() {
		return this.jobPlugin != null ? this.jobPlugin : "jobs";
	}
	
	/**
	 * Gets whether job levels should be consumed on completion.
	 *
	 * @return true if job levels should be consumed, false otherwise
	 */
	public Boolean getConsumeOnComplete() {
		return this.consumeOnComplete != null ? this.consumeOnComplete : false;
	}
	
	/**
	 * Gets whether all jobs must be at required level.
	 *
	 * @return true if all jobs are required, false if only one is needed
	 */
	public Boolean getRequireAll() {
		return this.requireAll != null ? this.requireAll : true;
	}
}
